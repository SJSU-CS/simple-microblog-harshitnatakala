package edu.sjsu.cmpe272.simpleblog.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import picocli.CommandLine.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Command(name = "microblog-cli", mixinStandardHelpOptions = true, version = "1.0",
        description = "Command-line tool for managing your MicroBlog account.",
        subcommands = {MicroblogCLI.PostMessageCommand.class, MicroblogCLI.ListMessagesCommand.class, MicroblogCLI.CreateUserCommand.class})
public class MicroblogCLI {

    private static final String CONFIG_FILE_PATH = "mb.ini";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Command(name = "create", description = "Creates a unique id and stores it in the mb.ini.")
    static class CreateUserCommand implements Runnable {
        @Parameters(index = "0") String username;
        @Override
        public void run() {
            try {
                File configFile = new File(CONFIG_FILE_PATH);
                if (configFile.exists()) {
                    System.out.print(CONFIG_FILE_PATH + " This User already exists. Overwrite? (y/n): ");
                    Scanner scanner = new Scanner(System.in);
                    if (!scanner.nextLine().equalsIgnoreCase("y")) {
                        System.out.println("The process was interrupted by the user.");
                        return;
                    }
                }

                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                KeyPair pair = keyGen.generateKeyPair();
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CONFIG_FILE_PATH))) {
                    oos.writeObject(username);
                    oos.writeObject(pair.getPublic());
                    oos.writeObject(pair.getPrivate());
                }

                System.out.printf("Your user ID has been generated and stored in %s.%n", CONFIG_FILE_PATH);
                System.out.println("{ \"message\": \"welcome\" }");
            } catch (Exception e) {
                System.err.printf("Error generating ID: %s%n", e.getMessage());
            }
        }
    }

    @Command(name = "post", description = "Posts a new message.")
    static class PostMessageCommand implements Runnable {
        @Parameters(index = "0", description = "The message.")
        private String messageText;

        @Option(names = {"-f", "--files"}, description = "Optional file")
        private File attachmentFile;

        private final RestTemplate restClient = new RestTemplate();

        @Override
        public void run() {
            try {
                Map<String, Object> userData = loadUserData(CONFIG_FILE_PATH);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> payload = constructPayload(userData);
                String jsonToSign = OBJECT_MAPPER.writeValueAsString(payload);

                String signature = signPayload(jsonToSign, (PrivateKey) userData.get("privateKey"));
                payload.put("signature", signature);

                String jsonPayload = OBJECT_MAPPER.writeValueAsString(payload);

                HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);

                ResponseEntity<String> response = restClient.postForEntity("http://localhost:8080/messages/create", requestEntity, String.class);
                System.out.println("Server response: " + response.getBody());
            } catch (Exception e) {
                System.err.println("An error occurred: " + e.getMessage());
                System.out.println(e);
            }
        }

        private String signPayload(String payload, PrivateKey privateKey) throws Exception {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        }

        private Map<String, Object> loadUserData(String configFilePath) throws IOException, ClassNotFoundException {
            try (FileInputStream fis = new FileInputStream(configFilePath);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                String username = (String) ois.readObject();
                PublicKey publicKey = (PublicKey) ois.readObject();
                PrivateKey privateKey = (PrivateKey) ois.readObject();
                return Map.of("username", username, "publicKey", publicKey, "privateKey", privateKey);
            }
        }

        private Map<String, Object> constructPayload(Map<String, Object> userData) throws GeneralSecurityException, IOException {
            String username = (String) userData.get("username");

            String utcDate = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("date", utcDate);
            payload.put("author", username);
            payload.put("message", messageText);
            addAttachmentToPayload(payload);
            return payload;
        }

        private void addAttachmentToPayload(Map<String, Object> payload) throws IOException {
            if (attachmentFile != null && attachmentFile.isFile()) {
                byte[] fileContent = Files.readAllBytes(attachmentFile.toPath());
                payload.put("attachment", Base64.getEncoder().encodeToString(fileContent));
            }
        }
    }

    @Command(name = "list", description = "Lists messages from the MicroBlog.")
    public static class ListMessagesCommand implements Runnable {

        @Option(names = {"--starting", "-s"}, description = "ID to start listing from.")
        private Integer startingId = -1;

        @Option(names = {"--count", "-c"}, description = "Number of messages to retrieve.", defaultValue = "10")
        private int count;

        @Option(names = {"--save-attachment", "-sa"}, description = "Save attachments if present.")
        private boolean saveAttachment;

        private final RestTemplate restClient = new RestTemplate();

        @Override
        public void run() {
            int messagesFetched = 0;
            int messagesToFetch = count;
            while (messagesFetched < count) {
                String url = String.format("http://localhost:8080/messages/list?limit=%d&next=%s",
                        Math.min(messagesToFetch, 20), startingId);
                try {
                    ResponseEntity<String> response = restClient.getForEntity(url, String.class);
                    if (response.getBody() != null) {
                        List<Map<String, Object>> messages = OBJECT_MAPPER.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
                        if (messages.isEmpty()) {
                            break;
                        }
                        for (Map<String, Object> message : messages) {
                            try {
                                displayMessage(message);
                                if (saveAttachment && message.get("attachment") != null && !((String) message.get("attachment")).isEmpty()) {
                                    saveAttachment((String) message.get("attachment"), (Integer) message.get("message-id"));
                                }

                            } catch (Exception e) {
                                System.out.println(e);
                                System.err.printf("Exception while verifying signature for message-id: %s. Error: %s%n", message.get("message-id"), e.getMessage());
                            }
                            messagesFetched++;
                            startingId = (Integer) message.get("message-id") - 1;
                        }

                        messagesToFetch -= messages.size();
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    break;
                }
            }
        }

        private void saveAttachment(String encodedAttachment, int messageId) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(encodedAttachment);
                Path path = Files.createTempFile("message-" + messageId, ".out");
                Files.write(path, decodedBytes);
                System.out.printf("Attachment for message-id %d saved to: %s%n", messageId, path);
            } catch (IOException e) {
                System.err.printf("Failed to save attachment for message-id %d: %s%n", messageId, e.getMessage());
            }
        }

        private void displayMessage(Map<String, Object> message) {
            String formattedMessage = "%s: %s %s says \"%s\"%s"
                    .formatted(message.get("message-id"),
                            message.get("date"),
                            message.get("author"),
                            message.get("message"),
                            message.containsKey("attachment") && message.get("attachment") != null && !((String) message.get("attachment")).isEmpty() ? " 📎" : "");
            System.out.println(formattedMessage);
        }

        private boolean verifyMessageSignature(Map<String, Object> message) {
            try {
                PublicKey publicKey = loadPublicKey();
                if (publicKey == null) {
                    System.err.println("Public key not found.");
                    return false;
                }
                Map<String, Object> verificationMap = message.entrySet().stream()
                        .filter(e -> !e.getKey().equals("message-id") && !e.getKey().equals("signature"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                String signatureData = (String) message.get("signature");
                if (signatureData == null) {
                    System.err.println("Signature not found in the data");
                    return false;
                }

                String serializedData = constructSigningData(verificationMap);

                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initVerify(publicKey);
                signature.update(serializedData.getBytes(StandardCharsets.UTF_8));

                return signature.verify(Base64.getDecoder().decode(signatureData));
            } catch (Exception e) {
                System.err.printf("Verification failed: %s%n", e.getMessage());
                return false;
            }
        }

        private String constructSigningData(Map<String, Object> message) {
            try {
                return OBJECT_MAPPER.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                System.err.println("Failed to serialize signed data: " + e.getMessage());
                return null;
            }
        }

        private PublicKey loadPublicKey() {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                ois.readObject();
                return (PublicKey) ois.readObject();
            } catch (Exception e) {
                System.err.printf("Failed to load public key: %s%n", e.getMessage());
                return null;
            }
        }
    }
}
package edu.sjsu.cmpe272.simpleblog.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.course.simplemicroblogcli.GenerateKeyPair.encodePublicKeyToPEM;

@Slf4j
@Component
@Command(name = "cli", mixinStandardHelpOptions = true, subcommands = {CommandLineInterface.PostMessage.class, CommandLineInterface.CreateUser.class, CommandLineInterface.ListMessages.class})
public class MicroblogCli {
    public static final String BASE_URL = "http://localhost:8080/";

    @Component
    @Command(
            name = "list",
            mixinStandardHelpOptions = true,
            exitCodeOnExecutionException = 34
    )
    public static class ListMessages implements Callable<Integer> {
        @Option(
                names = "--starting-id",
                description = "Starting message ID (default: 0)",
                defaultValue = "0"
        )
        private Integer startingId;

        @Option(
                names = "--limit",
                description = "Maximum number of messages to retrieve (default: 10)",
                defaultValue = "10"
        )
        private Integer limit;

        @Option(
                names = "--save-attachment",
                description = "Whether to save attachments (default: false)",
                defaultValue = "false"
        )
        private boolean saveAttachment;

        @Override
        public Integer call() throws Exception {
            log.info("Retrieving messages with starting ID: {} and limit: {}", startingId, limit);

            ListMessage listMessage = new ListMessage(limit, 1);
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(listMessage);
            System.out.println(json);

            ApiService apiService = new ApiService();
            String response = apiService.sendRawJsonPostRequest(BASE_URL + "messages/list", json);
            System.out.println(response);
            return 0; // Use 0 for successful execution
        }
    }

    @Component
    @Command(name = "create-user", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 34)
    public static class CreateUser implements Callable<Integer> {

        @Option(
                names = "--user-id",
                required = true,
                description = "User ID"
        )
        private String userId;

        @Override
        public Integer call() throws Exception {
            log.info("Creating user with ID: {}", userId);

            Map<String, String> keys = GenerateKeyPair.generateRSAKeyPair();
            if (keys.containsKey("privateKey")) {
                SaveToINI.saveToINIFile(userId, keys.get("privateKey"));
            }
            String publicKeyPem = encodePublicKeyToPEM(keys.get("publicKey"));

            Api.User user = new Api.User(userId, publicKeyPem);
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(user);

            ApiService apiService = new ApiService();
            String response = apiService.sendRawJsonPostRequest(BASE_URL + "user/create", json);
            System.out.println(response);
            return 0;
        }
    }

    @Component
    @Command(name = "post", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 34)
    public static class PostMessage implements Callable<Integer> {

        @Option(
                names = "--message",
                required = true,
                description = "Message content"
        )
        private String message;

        @Option(
                names = "--file-to-attach",
                description = "Path to file to attach"
        )
        private String fileToAttach;

        @Override
        public Integer call() throws Exception {
            log.info("Posting message: {}", message);

            LocalDateTime dateTime = LocalDateTime.now();
            String author = retriveUserId();
        }
    }
}

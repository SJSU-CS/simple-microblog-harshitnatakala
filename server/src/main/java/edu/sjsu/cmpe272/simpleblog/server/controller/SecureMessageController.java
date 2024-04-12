package edu.sjsu.cmpe272.simpleblog.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;

/**
 * This class defines a REST controller for handling message operations,
 * including adding and retrieving messages.
 */
@RestController
@RequestMapping("/messages")
public class MessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageRepository messageRepository;

    /**
     * Handles POST requests to add a new message. It expects a Message object
     * in the request body containing message details. The message signature is
     * verified using the public key of the author retrieved from the user service.
     *
     * @param message The Message object containing message details and signature.
     * @return A ResponseEntity object with a message ID on successful message
     *         creation, or an error message if signature verification fails.
     * @throws Exception If an exception occurs during signature verification.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addMessage(@RequestBody Message message) throws Exception {
        LOGGER.info("Received request to add message from user: {}", message.getAuthor());

        PublicKey publicKey = UserService.getPublicKeyFromString(UserService.userPublicKeys.get(message.getAuthor()));

        if (publicKey != null && VerifySignature.verifySignature(message, publicKey)) {
            LOGGER.info("Signature verified successfully");
            Message savedMessage = messageRepository.save(message);
            return ResponseEntity.ok(Map.of("message-id", savedMessage.getMessageId()));
        } else {
            LOGGER.warn("Signature verification failed for message from user: {}", message.getAuthor());
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to add message: Invalid signature"));
        }
    }

    /**
     * Handles POST requests to retrieve messages. It allows filtering by limit,
     * starting ID, and next/previous order.
     *
     * @param params A Map containing optional parameters for filtering messages.
     * @return A ResponseEntity object containing a list of messages or an error
     *         message if the limit parameter is invalid.
     */
    @PostMapping("/list")
    public ResponseEntity<?> retrieveMessages(@RequestBody Map<String, Object> params) {
        Integer limit = (Integer) params.getOrDefault("limit", 10);
        Integer next = (Integer) params.getOrDefault("next", -1);
        Integer startingId = (Integer) params.getOrDefault("starting_id", 0);

        if (limit > 20) {
            return ResponseEntity.badRequest().body("Error: Limit cannot be greater than 20");
        }

        List<Message> messages;
        if (next == -1) {
            messages = messageRepository.findAllByOrderByMessageIdAsc();
        } else {
            messages = messageRepository.findAllByMessageIdGreaterThanOrderByMessageIdAsc(startingId);
        }

        if (messages.size() > limit) {
            messages = messages.subList(0, limit);
        }

        return ResponseEntity.ok(messages);
    }
}

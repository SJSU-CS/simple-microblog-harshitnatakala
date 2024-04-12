package edu.sjsu.cmpe272.simpleblog.server.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class defines a REST controller for handling user-related operations,
 * including creating users, listing user public keys, and retrieving a specific
 * user's public key.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * Handles POST requests to create a new user. It expects a User object
     * in the request body containing the username and public key. The public key
     * is sanitized by removing whitespaces and header/footer lines before being
     * passed to the user service.
     *
     * @param params The User object containing username and public key.
     * @return A ResponseEntity object with a success message on successful
     *         user creation.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody User params) {
        LOGGER.info("Received create user request for username: {}", params.getUser());

        String sanitizedPublicKey = sanitizePublicKey(params.getPublicKey());
        userService.createUser(params.getUser(), sanitizedPublicKey);

        return ResponseEntity.ok(Map.of("message", "welcome"));
    }

    /**
     * Handles POST requests to list all usernames associated with public keys.
     * It retrieves the user public keys from the user service and returns a list
     * containing just the usernames.
     *
     * @return A ResponseEntity object containing a list of usernames.
     */
    @PostMapping("/list")
    public ResponseEntity<?> listUsers() {
        LOGGER.info("Received request to list user keys");

        Map<String, String> userPublicKeys = userService.getUserPublicKeys();
        List<String> usernames = new ArrayList<>(userPublicKeys.keySet());

        return ResponseEntity.ok(usernames);
    }

    /**
     * Handles GET requests to retrieve the public key of a specific user
     * identified by username. It retrieves the public key from the user service
     * and returns it in the response body if found.
     *
     * @param username The username of the user whose public key is requested.
     * @return A ResponseEntity object containing the public key (if found) or
     *         an error message (if not found).
     */
    @GetMapping("/{username}/public-key")
    public ResponseEntity<?> getPublicKey(@PathVariable String username) {
        LOGGER.info("Received request for public key of username: {}", username);

        String publicKey = userService.getPublicKeyByUsername(username);
        if (publicKey == null) {
            LOGGER.warn("Public key not found for username: {}", username);
            return ResponseEntity.badRequest().body(Map.of("error", "Public key not found"));
        }
        return ResponseEntity.ok(Map.of("public-key", publicKey));
    }

    /**
     * Sanitizes the provided public key string by removing whitespaces and
     * header/footer lines (assuming they are not part of the actual key).
     *
     * @param publicKey The public key string to be sanitized.
     * @return The sanitized public key string.
     */
    private String sanitizePublicKey(String publicKey) {
        return publicKey.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}

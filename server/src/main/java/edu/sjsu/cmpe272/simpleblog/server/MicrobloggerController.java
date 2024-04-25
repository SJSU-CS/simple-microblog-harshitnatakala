package edu.sjsu.cmpe272.simpleblog.server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/user")
public class MicrobloggerController {
    @Autowired
    private MicrobloggerService microbloggerService;

    @GetMapping("/{username}/public-key")
    public ResponseEntity<?> getPublicKey(@PathVariable String username) {
        String publicKey = microbloggerService.getPublicKeyByUsername(username);
        if (publicKey == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "signature didn't match"));
        }
        return ResponseEntity.ok(Map.of("public-key", publicKey));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody Microblogger params) {
        String publicKey = params.getPublicKey().replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    microbloggerService.createUser(params.getUser(),publicKey);
        return ResponseEntity.ok(Map.of("message", "welcome"));
    }

    @PostMapping("/list")
    public ResponseEntity<?> listMessages() {
        Map<String, String> users = MicrobloggerService.microbloggerPublicKeys;
        List<String> keyList = new ArrayList<>(users.keySet());
        return ResponseEntity.ok(keyList);
    }

}

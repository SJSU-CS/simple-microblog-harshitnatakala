package edu.sjsu.cmpe272.simpleblog.server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MicroblogController {

    @Autowired
    private MicroblogDataRepository microblogDataRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createMessage(@RequestBody MicroblogPost message) throws Exception {
    PublicKey publicKey = MicrobloggerService.getPublicKeyFromString(MicrobloggerService.microbloggerPublicKeys.get(message.getAuthor()));

        if(publicKey!=null && VerifySignature.AuthenticateSignature(message,publicKey) ){
            System.out.println("verify signature "+VerifySignature.AuthenticateSignature(message,publicKey));
            MicroblogPost savedMessage = microblogDataRepository.save(message);
            return ResponseEntity.ok(Map.of("message-id", savedMessage.getMessageId()));
        }
        else{

            return ResponseEntity.ok(Map.of("error", "failed to create message"));
        }

    }

    @PostMapping("/list")
    public ResponseEntity<?> listMessages(@RequestBody Map<String, Object> params) {
        Integer limit = (Integer) params.getOrDefault("limit", 10);
        Integer next = (Integer) params.getOrDefault("next",-1);
        Integer starting_id = (Integer) params.getOrDefault("starting_id",0);
        if (limit > 20) {
            return ResponseEntity.badRequest().body("Error: Limit value out of range 20");
        }
        List<MicroblogPost> messages= microblogDataRepository.findAllByOrderByMessageIdAsc();
        if(next==-1){
            messages = microblogDataRepository.findAllByOrderByMessageIdDesc();
        }

        if (messages.size() > limit) {
            messages = messages.subList(starting_id, limit);
        }
        return ResponseEntity.ok(messages);
    }
}

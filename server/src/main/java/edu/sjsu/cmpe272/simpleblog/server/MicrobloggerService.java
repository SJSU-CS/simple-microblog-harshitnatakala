package edu.sjsu.cmpe272.simpleblog.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class MicrobloggerService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    public static Map<String, String> microbloggerPublicKeys = new HashMap<>();

    public void createUser(String username, String publicKey) {
        microbloggerPublicKeys.put(username.toLowerCase(), publicKey);
    }

    public String getPublicKey(String username) {
        return microbloggerPublicKeys.get(username.toLowerCase());
    }



    public static PrivateKey getPrivateKeyFromString(String key) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(key.getBytes());
        PKCS8EncodedKeySpec PKCS8privateKey = new PKCS8EncodedKeySpec(byteKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(PKCS8privateKey);
    }

    public String getPublicKeyByUsername(String username) {
        if(microbloggerPublicKeys.containsKey(username)){
            return microbloggerPublicKeys.get(username);
        }
        return null;
    }

    public static PublicKey getPublicKeyFromString(String key) throws Exception {
        try{
            byte[] byteKey = Base64.getDecoder().decode(key.getBytes());
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return kf.generatePublic(X509publicKey);
        }
        catch (Exception e){
            return null;
        }
    }
}
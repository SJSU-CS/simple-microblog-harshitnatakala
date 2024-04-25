package edu.sjsu.cmpe272.simpleblog.server;

import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class VerifySignature {
    public static boolean AuthenticateSignature(MicroblogPost message, PublicKey publicKey) throws Exception {
        String messageSignature = message.getSignature();
        byte[] decoded = Base64.getDecoder().decode(messageSignature);
        String data =message.getDate() + message.getAuthor() + message.getMessage() + message.getAttachment();
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(data.getBytes());
        return sig.verify(decoded);
    }
}

package edu.sjsu.cmpe272.simpleblog.server;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


@Entity
public class MicroblogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = true)
    private String attachment;

    @Column(nullable = false)
    private String date;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false,length = 2048)

    private String signature;

    public MicroblogPost() {
    }

    public MicroblogPost(String date, String author, String message, String attachment, String signature) {
        this.date = date;
        this.author = author;
        this.message = message;
        this.attachment = attachment;
        this.signature = signature;
    }

    // Getters and setters



    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }


    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

package com.mohit.camverz;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private boolean seen;
    private List<String> deletedFor;
    private String replyToId;
    private String replyToText;
    private String replyToSenderName;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
        this.deletedFor = new ArrayList<>();
    }

    public Message(String messageId, String senderId, String receiverId, String message, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = false;
        this.deletedFor = new ArrayList<>();
    }

    public Message(String messageId, String senderId, String receiverId, String message, long timestamp,
                   String replyToId, String replyToText, String replyToSenderName) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = false;
        this.deletedFor = new ArrayList<>();
        this.replyToId = replyToId;
        this.replyToText = replyToText;
        this.replyToSenderName = replyToSenderName;
    }

    public String getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }

    public String getReplyToText() {
        return replyToText;
    }

    public void setReplyToText(String replyToText) {
        this.replyToText = replyToText;
    }

    public String getReplyToSenderName() {
        return replyToSenderName;
    }

    public void setReplyToSenderName(String replyToSenderName) {
        this.replyToSenderName = replyToSenderName;
    }

    public boolean hasReply() {
        return replyToText != null && !replyToText.trim().isEmpty();
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public List<String> getDeletedFor() {
        return deletedFor;
    }

    public void setDeletedFor(List<String> deletedFor) {
        this.deletedFor = deletedFor;
    }
}

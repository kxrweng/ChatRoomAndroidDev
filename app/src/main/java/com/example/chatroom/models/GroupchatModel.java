package com.example.chatroom.models;

import java.util.Date;

public class GroupchatModel {

    String message;
    String username;
    String messageID;
    Date timestamp;

    public GroupchatModel(){}
    public GroupchatModel(String message, String username, String messageID, Date timestamp) {
        this.message = message;
        this.username = username;
        this.messageID = messageID;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}

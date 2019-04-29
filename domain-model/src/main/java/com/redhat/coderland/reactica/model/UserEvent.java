package com.redhat.coderland.reactica.model;

public class UserEvent {
    public String action;
    public User user;

    public UserEvent(String action, User user) {
        this.action = action;
        this.user = user;
    }

    public UserEvent() {
    }
}

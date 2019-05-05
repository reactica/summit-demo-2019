package com.redhat.coderland.reactica.model;

public class UserInLineEvent extends Event {

    private static final EventType eventType = EventType.USER_IN_LINE;
    private User user;

    public UserInLineEvent() {
    }

    public UserInLineEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public String getQueueName() {
        return Event.USER_IN_LINE;
    }
}

package com.redhat.coderland.reactica.model;

public class UserOnRideEvent extends Event {

    private static final EventType eventType = EventType.USER_ON_RIDE;
    private User user;


    public UserOnRideEvent() {
    }

    public UserOnRideEvent(User user) {
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
        return Event.USER_ON_RIDE;
    }
}

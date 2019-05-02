package com.redhat.coderland.reactica.model;

public class UserLeavingRideEvent extends Event {

    private static final EventType eventType = EventType.USER_LEAVING_RIDE;
    private User user;


    public UserLeavingRideEvent() {
    }

    public UserLeavingRideEvent(User user) {
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
        return Event.USER_LEAVING;
    }
}

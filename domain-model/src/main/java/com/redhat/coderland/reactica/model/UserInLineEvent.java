package com.redhat.coderland.reactica.model;

public class UserInLineEvent extends Event {

    private static final EventType eventType = EventType.USER_IN_LINE;
    private User user;
    private Ride ride;


    public UserInLineEvent() {
    }

    public UserInLineEvent(User user, Ride ride) {
        this.user = user;
        this.ride = ride;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
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

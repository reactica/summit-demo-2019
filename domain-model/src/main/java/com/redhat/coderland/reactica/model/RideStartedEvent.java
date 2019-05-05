package com.redhat.coderland.reactica.model;

public class RideStartedEvent extends Event {

    private static final EventType eventType = EventType.RIDE_STARTED;
    private Ride ride;
    private int rideTime;
    private int capacity;



    public RideStartedEvent() {
    }

    public RideStartedEvent(Ride ride, int rideTime, int capacity) {
        this.ride = ride;
        this.rideTime = rideTime;
        this.capacity = capacity;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public int getRideTime() {
        return rideTime;
    }

    public void setRideTime(int rideTime) {
        this.rideTime = rideTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
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

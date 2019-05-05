package com.redhat.coderland.reactica.model;

public class RideCompletedEvent extends Event {

    private static final EventType eventType = EventType.RIDE_COMPLETED;

    private Ride ride;
    private int leaveTime;



    public RideCompletedEvent() {
    }

    public RideCompletedEvent(Ride ride,int leaveTime) {
        this.ride = ride;
        this.leaveTime = leaveTime;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public int getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(int leaveTime) {
        this.leaveTime = leaveTime;
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

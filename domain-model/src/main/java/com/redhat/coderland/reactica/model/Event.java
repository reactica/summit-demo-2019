package com.redhat.coderland.reactica.model;

public abstract class Event {
    /**
     * User added on the queue.
     */
    public static final String USER_IN_LINE = "user-in-line";

    /**
     * User on ride.
     */
    public static final String USER_ON_RIDE = "user-on-ride";

    /**
     * User has completed the ride (ride completed)
     */
    public static final String USER_COMPLETED = "user-ride-completed";

    /**
     * User leaving the ride
     */
    public static final String USER_LEAVING = "user-ride-leaving";

    /**
     * A ride just started.
     */
    public static final String RIDE_STARTED = "ride-started";

    /**
     * A ride just completed.
     */
    public static final String RIDE_COMPLETED = "ride-completed";


    public enum EventType {USER_IN_LINE,USER_ON_RIDE,USER_COMPLETED_RIDE, RIDE_STARTED, RIDE_COMPLETED, USER_LEAVING_RIDE}

    public abstract EventType getEventType();

    public abstract String getQueueName();

}

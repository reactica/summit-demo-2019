package com.redhat.coderland.ride.eventgen;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@ApplicationScoped
public class RandomRideService {


    @Inject
    @ConfigProperty(name = "coderland.rides", defaultValue = "roller-coaster")
    String configRides;

    @Inject @ConfigProperty(name = "coderland.config.ridetime.min",defaultValue="30")
    Integer rideTimeMin;

    @Inject @ConfigProperty(name = "coderland.config.ridetime.max",defaultValue="60")
    Integer rideTimeMax;

    @Inject @ConfigProperty(name = "coderland.config.leavingtime.min",defaultValue="5")
    Integer leaveTimeMin;

    @Inject @ConfigProperty(name = "coderland.config.leavingtime.max",defaultValue="10")
    Integer leaveTimeMax;

    @Inject @ConfigProperty(name = "coderland.config.ride.capacity",defaultValue="10")
    Integer rideCapacity;



    public RandomRideService() {
    }

    public List<String> getRides() {
        return Collections.unmodifiableList(Arrays.asList(configRides.split(",")));

    }

    public String getRandomRideId() {
        int randomRideId = (int) Math.floor(Math.random() * getRides().size());
        return getRides().get(randomRideId);
    }

    public int getRandomRideTime() {
        return (int) Math.floor(Math.random() * (rideTimeMax-rideTimeMin) + rideTimeMin);
    }

    public int getRandomLeaveTime() {
        return (int) Math.floor(Math.random() * (leaveTimeMax-leaveTimeMin) + leaveTimeMin);
    }

    public int getCapacity() {
        return rideCapacity;
    }






}

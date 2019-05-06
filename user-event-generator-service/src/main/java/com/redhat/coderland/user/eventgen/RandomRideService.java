package com.redhat.coderland.user.eventgen;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@ApplicationScoped
public class RandomRideService {


    @Inject @ConfigProperty(name = "coderland.rides")
    Optional<String> configRides;

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


    private List<String> rides;



    public RandomRideService() {
        //this.rides = Arrays.asList(configRides.orElse("roller-coaster,screamer").split(","));

    }

    public List<String> getRides() {
        return Collections.unmodifiableList(this.rides);

    }

    public String getRandomRideId() {
        int randomRideId = (int) Math.floor(Math.random() * this.rides.size());
        return this.rides.get(randomRideId);
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


    @PostConstruct
    public void start() {
        this.rides = Arrays.asList(configRides.orElse("roller-coaster,screamer").split(","));
    }






}

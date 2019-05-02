package com.redhat.coderland.eventstore;

import com.redhat.coderland.reactica.model.Ride;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.List;


@ApplicationScoped
public class RandomRideService {

    // TODO: Replace this with configuraiton data
    private List<Ride> RIDES = Arrays.asList(new Ride[]{ new Ride("roller-coaster"), new Ride("screamer") });


    public List<Ride> getRIDES() {
        return RIDES;
    }

    public Ride getRandomRide() {
        int randomRideId = (int) Math.floor(Math.random() * RIDES.size());
        return RIDES.get(randomRideId);
    }






}

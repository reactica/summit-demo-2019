package com.redhat.coderland.ride.eventgen;

import com.redhat.coderland.reactica.model.*;
import io.vertx.core.json.JsonObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class RideEventHelper {

  @Inject
  RandomRideService randomRideService;

  public RideStartedEvent createRandomRideStartedEvent() {
    Ride ride = new Ride();
    ride.setAttractionId(randomRideService.getRandomRideId());
    return new RideStartedEvent(ride,randomRideService.getRandomRideTime(),randomRideService.getCapacity());
  }

  public RideCompletedEvent createRideCompletedEvent(Ride ride) {
    return new RideCompletedEvent(ride,randomRideService.getRandomLeaveTime());
  }

}

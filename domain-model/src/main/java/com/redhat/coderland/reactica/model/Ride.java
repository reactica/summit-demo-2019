package com.redhat.coderland.reactica.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * This Ride class represents a single ride. Everytime an attraction
 */
public class Ride {

  public enum State {COMPELETED,RUNNING,WAITING,UNKNOWN}

  private String rideId = UUID.randomUUID().toString();
  private String attractionId;
  private List<User> userOnTheRide = new ArrayList<User>();

  private State state = State.UNKNOWN;

  public Ride() {
  }

  public String getRideId() {
    return rideId;
  }

  public void setRideId(String rideId) {
    this.rideId = rideId;
  }

  public String getAttractionId() {
    return attractionId;
  }

  public void setAttractionId(String attractionId) {
    this.attractionId = attractionId;
  }

  public List<User> getUserOnTheRide() {
    return userOnTheRide;
  }

  public void setUserOnTheRide(List<User> userOnTheRide) {
    this.userOnTheRide = userOnTheRide;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }
}

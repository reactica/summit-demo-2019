package com.redhat.coderland.reactica.model;


public class Ride {

  public enum State {COMPELETED,RUNNING,WAITING,UNKNOWN}

  private String name;

  private State state = State.UNKNOWN;

  public Ride() {
  }

  public Ride(String name) {
    this.name = name;
  }

  public Ride(String name, State state) {
    this.name = name;
    this.state = state;
  }


  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}

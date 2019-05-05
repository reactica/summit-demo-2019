package com.redhat.coderland.eventstore;


import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.bridge.EventBusToAmqp;
import com.redhat.coderland.reactica.model.Event;
import com.redhat.coderland.reactica.model.RideCompletedEvent;
import com.redhat.coderland.reactica.model.RideStartedEvent;
import com.redhat.coderland.reactica.model.User;
import io.quarkus.scheduler.Scheduled;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@ApplicationScoped
public class RideSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RideSimulator.class);


    @Inject @ConfigProperty(name = "bridge.amqp.host")
    Optional<String> host;

    @Inject @ConfigProperty(name = "bridge.amqp.port")
    Optional<Integer> port;

    @Inject @ConfigProperty(name = "bridge.amqp.user")
    Optional<String> user;

    @Inject @ConfigProperty(name = "bridge.amqp.password")
    Optional<String> password;

    @Inject @ConfigProperty(name = "coderland.config.rollercoaster.ridetime",defaultValue="30")
    Optional<Integer> rollerCoasterRideTimeInSeconds;

    @Inject @ConfigProperty(name = "coderland.config.rollercoaster.leavingtime",defaultValue="5")
    Optional<Integer> userLeavingTimeInSeconds;

    @Inject @ConfigProperty(name = "coderland.config.rollercoaster.capacity",defaultValue="10")
    Optional<Integer> rideCapacityRollerCoaster;


    @Inject
    Vertx vertx;

    @Inject
    EventHelper eventHelper;

    boolean isBridgeStarted = false;



    @Scheduled(every = "{coderland.config.generateevent.ride.interval}")
    public void generateRideEvent() {
        if(isBridgeStarted) {
                RideStartedEvent rideStartedEvent = eventHelper.createRandomRideStartedEvent();
                vertx.eventBus().send(Event.RIDE_STARTED,JsonObject.mapFrom(rideStartedEvent));
                vertx.setTimer(rideStartedEvent.getRideTime()*1000, t -> {
                    RideCompletedEvent rideCompletedEvent = eventHelper.createRideCompletedEvent(rideStartedEvent.getRide());
                    vertx.eventBus().send(Event.RIDE_COMPLETED,JsonObject.mapFrom(rideCompletedEvent));
                });
        } else {
            LOGGER.info("Bridge not started yet. Skipping a schedule");
        }
    }


    @PostConstruct
    public void start() {
        deployAMQPVerticle()
                .andThen(Completable.fromAction(() -> this.isBridgeStarted=true ))
                .doOnError(t -> LOGGER.error("Failed to start bridge with cause: {}", t.getCause(),t))
                .subscribe();
    }


    private Completable deployAMQPVerticle() {
        EventBusToAmqp ride_started_events = new EventBusToAmqp();
        ride_started_events.setAddress(Event.RIDE_STARTED);
        ride_started_events.setQueue(Event.RIDE_STARTED);

        EventBusToAmqp ride_completed_events = new EventBusToAmqp();
        ride_completed_events.setAddress(Event.RIDE_COMPLETED);
        ride_completed_events.setQueue(Event.RIDE_COMPLETED);


        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost(host.orElse("localhost"))
                .setPort(port.orElse(5672))
                .setUser(user.orElse("user"))
                .setPassword(password.orElse("user123"))
                .addEventBusToAmqp(ride_started_events)
                .addEventBusToAmqp(ride_completed_events);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }


}

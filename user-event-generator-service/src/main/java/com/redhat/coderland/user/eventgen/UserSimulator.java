package com.redhat.coderland.user.eventgen;


import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.bridge.EventBusToAmqp;
import com.redhat.coderland.reactica.model.Event;
import io.quarkus.scheduler.Scheduled;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;


@ApplicationScoped
public class UserSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSimulator.class);


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
    UserEventHelper userEventHelper;

    private boolean isBridgeStarted = false;


    @Scheduled(every = "{coderland.config.generateevent.user.interval}")
    public void genereteNewUsers() {
        if(isBridgeStarted) {
            Event event = userEventHelper.createRandomUserInLineEvent();
            vertx.eventBus().send(event.getQueueName(), JsonObject.mapFrom(event));
        } else {
            LOGGER.info("Skipping creating event since the bridge isn't started yet");
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
        EventBusToAmqp user_queue = new EventBusToAmqp();
        user_queue.setAddress(Event.USER_IN_LINE);
        user_queue.setQueue(Event.USER_IN_LINE);


        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost(host.orElse("localhost"))
                .setPort(port.orElse(5672))
                .setUser(user.orElse("user"))
                .setPassword(password.orElse("user123"))
                .addEventBusToAmqp(user_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }


}

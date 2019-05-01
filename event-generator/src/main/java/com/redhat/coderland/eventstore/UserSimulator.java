package com.redhat.coderland.eventstore;


import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.bridge.EventBusToAmqp;
import com.redhat.coderland.reactica.model.Event;
import io.quarkus.scheduler.Scheduled;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


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


    @Inject
    Vertx vertx;

    @Inject
    EventHelper eventHelper;


    private Future<Void> bridgeStarted;

    //TODO: Maybe remove the delay since we are waiting for the bridge Future.
    @Scheduled(every = "1s",delayUnit = TimeUnit.SECONDS,delay = 1)
    public void genereteUsers() {
        if(bridgeStarted.failed()) {
            LOGGER.error("Failed to start bridge with cause: " + bridgeStarted.cause());
        } else {
            LOGGER.info("Generating Event");
            Event event = eventHelper.createRandomUserInLineEvent();
            vertx.eventBus().send(event.getQueueName(), JsonObject.mapFrom(event));
        }
    }


    @PostConstruct
    public void start() {
        bridgeStarted = Future.future();
        deployAMQPVerticle().subscribe(CompletableHelper.toObserver(bridgeStarted));
    }



    private Completable deployAMQPVerticle() {
        EventBusToAmqp user_queue = new EventBusToAmqp();
        user_queue.setAddress(Event.USER_IN_LINE);
        user_queue.setQueue(Event.USER_IN_LINE);

//        // Consume by the billboard
//        EventBusToAmqp enter_queue = new EventBusToAmqp();
//        enter_queue.setAddress("to-enter-event-queue");
//        enter_queue.setQueue("ENTER_EVENT_QUEUE");
//
//        EventBusToAmqp ride_event_queue = new EventBusToAmqp();
//        ride_event_queue.setAddress("to-ride-event-queue");
//        ride_event_queue.setQueue("RIDE_EVENT_QUEUE");

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost(host.orElse("localhost"))
                .setPort(port.orElse(5672))
                .setUser(user.orElse(null))
                .setPassword(password.orElse(null))
//                .addEventBusToAmqp(enter_queue)
//                .addEventBusToAmqp(ride_event_queue)
                .addEventBusToAmqp(user_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }


}

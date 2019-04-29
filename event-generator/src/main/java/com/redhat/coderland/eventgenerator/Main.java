package com.redhat.coderland.eventgenerator;


import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.bridge.EventBusToAmqp;
import io.quarkus.scheduler.Scheduled;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.*;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.Vertx;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;


@ApplicationScoped
public class Main {

    @Inject
    Vertx vertx;

    private Future<Void> bridgeStarted;

    @Scheduled(every = "5s",delayUnit = TimeUnit.SECONDS,delay = 1)
    public void generete() {
        System.out.println("generating");
        if(bridgeStarted.failed()) {
            System.out.println("Failed to start bridge with cause: " + bridgeStarted.cause());
        } else {
            vertx.eventBus().send("to-user-queue",new JsonObject().put("test","success"));
        }
    }


    @PostConstruct
    public void start() {
        bridgeStarted = Future.future();
        deployAMQPVerticle().subscribe(CompletableHelper.toObserver(bridgeStarted));
    }

    private Completable deployAMQPVerticle() {
        EventBusToAmqp user_queue = new EventBusToAmqp();
        user_queue.setAddress("to-user-queue");
        user_queue.setQueue("USER_QUEUE");

        // Consume by the billboard
        EventBusToAmqp enter_queue = new EventBusToAmqp();
        enter_queue.setAddress("to-enter-event-queue");
        enter_queue.setQueue("ENTER_EVENT_QUEUE");

        EventBusToAmqp ride_event_queue = new EventBusToAmqp();
        ride_event_queue.setAddress("to-ride-event-queue");
        ride_event_queue.setQueue("RIDE_EVENT_QUEUE");

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost("localhost")
                .setPort(5672)
                .setUser("user")
                .setPassword("user123")
                .addEventBusToAmqp(enter_queue)
                .addEventBusToAmqp(user_queue)
                .addEventBusToAmqp(ride_event_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }
}

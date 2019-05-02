package com.redhat.coderland.eventstore;


import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.bridge.EventBusToAmqp;
import com.redhat.coderland.reactica.model.Event;
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
import java.util.List;
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

    @Inject @ConfigProperty(name = "ride.config.rollercoaster.ridetime",defaultValue="10")
    Optional<Integer> rollerCoasterRideTimeInSeconds;

    @Inject @ConfigProperty(name = "ride.config.rollercoaster.ridetime",defaultValue="5")
    Optional<Integer> userLeavingTimeInSeconds;


    @Inject
    Vertx vertx;

    @Inject
    EventHelper eventHelper;

    @Inject
    RemoteCache<String, User> cache;


    private Future<Void> bridgeStarted;

    @Scheduled(every = "{coderland.generateevent.newuserintervall}")
    public void genereteNewUsers() {
        if(bridgeStarted.failed()) {
            LOGGER.error("Failed to start bridge with cause: " + bridgeStarted.cause());
        } else {
            LOGGER.info("Generating USER Event");
            Event event = eventHelper.createRandomUserInLineEvent();
            vertx.eventBus().send(event.getQueueName(), JsonObject.mapFrom(event));
        }
    }

    @Scheduled(every = "{coderland.generateevent.rollercoaster.ride.intervall}", delay=2, delayUnit = TimeUnit.SECONDS)
    public void putRollerCoasterUsersOnRide() {
        if(bridgeStarted.failed()) {
            LOGGER.error("Failed to start bridge with cause: " + bridgeStarted.cause());
        } else {
            LOGGER.info("Generating RIDE Event");
            QueryFactory queryFactory = Search.getQueryFactory(cache);
            Query query = queryFactory.from(User.class)
                    .having("rideId").equal("roller-coaster")
                    .and()
                    .having("currentState").in(User.STATE_IN_QUEUE)
                    .orderBy("enterQueueTime", SortOrder.ASC)
                    .maxResults(5)
                    .build();


            query.<User>list().forEach(user -> {
                Event event = eventHelper.createUserOnRollerCoasterEvent(user);
                JsonObject jsonEvent = JsonObject.mapFrom(event);
                LOGGER.info("Sending event {} for user {}", Event.USER_ON_RIDE, jsonEvent);
                vertx.eventBus().send(Event.USER_ON_RIDE, jsonEvent);
                vertx.setTimer(rollerCoasterRideTimeInSeconds.get() * 1000, i -> {
                    LOGGER.info("Sending event {} for user {}", Event.USER_COMPLETED, jsonEvent);
                    vertx.eventBus().send(Event.USER_COMPLETED, jsonEvent);
                    vertx.setTimer(userLeavingTimeInSeconds.get() * 1000, j -> {
                        LOGGER.info("Sending event {} for user {}", Event.USER_LEAVING, jsonEvent);
                        vertx.eventBus().send(Event.USER_LEAVING, jsonEvent);
                    });
                });
            });
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

        EventBusToAmqp user_on_ride_queue = new EventBusToAmqp();
        user_on_ride_queue.setAddress(Event.USER_ON_RIDE);
        user_on_ride_queue.setQueue(Event.USER_ON_RIDE);


        EventBusToAmqp user_completed_queue = new EventBusToAmqp();
        user_completed_queue.setAddress(Event.USER_COMPLETED);
        user_completed_queue.setQueue(Event.USER_COMPLETED);

        EventBusToAmqp user_leaving_queue = new EventBusToAmqp();
        user_leaving_queue.setAddress(Event.USER_COMPLETED);
        user_leaving_queue.setQueue(Event.USER_COMPLETED);


        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost(host.orElse("localhost"))
                .setPort(port.orElse(5672))
                .setUser(user.orElse("user"))
                .setPassword(password.orElse("user123"))
                .addEventBusToAmqp(user_completed_queue)
                .addEventBusToAmqp(user_leaving_queue)
                .addEventBusToAmqp(user_on_ride_queue)
                .addEventBusToAmqp(user_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }


}

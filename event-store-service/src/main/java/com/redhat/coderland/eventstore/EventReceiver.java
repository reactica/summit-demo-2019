package com.redhat.coderland.eventstore;

import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpToEventBus;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.reactica.model.*;
import io.quarkus.runtime.StartupEvent;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class EventReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventReceiver.class);


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
    RemoteCache<String,User> cache;

    private Future<Void> bridgeStarted;


    @PostConstruct
    public void start() {

        bridgeStarted = Future.future();
        vertx.eventBus().<JsonObject>localConsumer(Event.USER_IN_LINE,message -> {
            JsonObject userEventJson = message.body();

            userEventJson.remove("eventType");
            userEventJson.remove("queueName");

            UserInLineEvent userEvent = userEventJson.mapTo(UserInLineEvent.class);
            User user = userEvent.getUser();
            Ride ride = userEvent.getRide();
            user.putInQueue();
            user.setRideId(ride.getName());
            LOGGER.info(">>> RECEIVED {} from {} for ride {}",Event.USER_IN_LINE, user.getName(),user.getRideId());
            cache.putAsync(user.getId(),user);

        });
        vertx.eventBus().<JsonObject>localConsumer(Event.USER_ON_RIDE,message -> {
            JsonObject userEventJson = message.body();
            userEventJson.remove("eventType");
            userEventJson.remove("queueName");

            UserOnRideEvent userEvent = userEventJson.mapTo(UserOnRideEvent.class);
            User user = userEvent.getUser();
            user.onRide();
            LOGGER.info(">>> RECEIVED {} from {} for ride {}",Event.USER_ON_RIDE, user.getName(),user.getRideId());
            cache.putAsync(user.getId(),user);
        });

        vertx.eventBus().<JsonObject>localConsumer(Event.USER_COMPLETED,message -> {
            JsonObject userEventJson = message.body();
            userEventJson.remove("eventType");
            userEventJson.remove("queueName");

            UserCompletedRideEvent userEvent = userEventJson.mapTo(UserCompletedRideEvent.class);
            User user = userEvent.getUser();
            user.completed();
            LOGGER.info(">>> RECEIVED {} from {} for ride {}",Event.USER_COMPLETED, user.getName(),user.getRideId());
            cache.putAsync(user.getId(),user);
        });

        vertx.eventBus().<JsonObject>localConsumer(Event.USER_LEAVING,message -> {
            JsonObject userEventJson = message.body();
            userEventJson.remove("eventType");
            userEventJson.remove("queueName");

            UserLeavingRideEvent userEvent = userEventJson.mapTo(UserLeavingRideEvent.class);
            User user = userEvent.getUser();
            LOGGER.info(">>> RECEIVED {} from {} for ride {}",Event.USER_LEAVING, user.getName(),user.getRideId());
            cache.removeAsync(user.getId());
        });

        deployAMQPVerticle().subscribe(CompletableHelper.toObserver(bridgeStarted));

    }

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application is starting...");
    }


    private Completable deployAMQPVerticle() {
        AmqpToEventBus user_in_line_bridge = new AmqpToEventBus();
        user_in_line_bridge.setAddress(Event.USER_IN_LINE);
        user_in_line_bridge.setQueue(Event.USER_IN_LINE);

        AmqpToEventBus user_on_ride_bridge = new AmqpToEventBus();
        user_on_ride_bridge.setAddress(Event.USER_ON_RIDE);
        user_on_ride_bridge.setQueue(Event.USER_ON_RIDE);

        AmqpToEventBus user_completed_ride_bridge = new AmqpToEventBus();
        user_completed_ride_bridge.setAddress(Event.USER_COMPLETED);
        user_completed_ride_bridge.setQueue(Event.USER_COMPLETED);

        AmqpToEventBus user_leaving_ride_bridge = new AmqpToEventBus();
        user_leaving_ride_bridge.setAddress(Event.USER_LEAVING);
        user_leaving_ride_bridge.setQueue(Event.USER_LEAVING);

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost(host.orElse("localhost"))
                .setPort(port.orElse(5672))
                .setUser(user.orElse("user"))
                .setPassword(password.orElse("user123"))
                .addAmqpToEventBus(user_leaving_ride_bridge)
                .addAmqpToEventBus(user_completed_ride_bridge)
                .addAmqpToEventBus(user_on_ride_bridge)
                .addAmqpToEventBus(user_in_line_bridge);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }
}

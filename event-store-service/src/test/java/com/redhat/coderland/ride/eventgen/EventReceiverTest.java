package com.redhat.coderland.ride.eventgen;

import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.bridge.EventBusToAmqp;
import com.redhat.coderland.reactica.model.Event;
import com.redhat.coderland.reactica.model.RideStartedEvent;
import com.redhat.coderland.reactica.model.User;
import com.redhat.coderland.reactica.model.UserInLineEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class EventReceiverTest {

//    private static final LinkedBlockingDeque<Event> USER_EVENTS = new LinkedBlockingDeque<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(EventReceiverTest.class);

    @Inject
    Vertx vertx;

    @Inject
    EventHelper eventHelper;

    @Inject
    RemoteCache<String, User> cache;

    @Test
    public void testUserSimulation() throws Exception {

        UserInLineEvent randomUserInLineEvent = eventHelper.createRandomUserInLineEvent();
        RideStartedEvent randomRideStartedEvent = eventHelper.createRandomRideStartedEvent();
        randomRideStartedEvent.getRide().setAttractionId(randomUserInLineEvent.getUser().getRideId());
        deployAMQPVerticle()
                .andThen(sendNewUserInLine(JsonObject.mapFrom(randomUserInLineEvent)))
                .andThen(checkThatUserWasCreated(JsonObject.mapFrom(randomUserInLineEvent)))
                .andThen(sendRideStartedEvent(JsonObject.mapFrom(randomRideStartedEvent)))
                .andThen(checkThatRideHasUpdatedTheUser(JsonObject.mapFrom(randomUserInLineEvent)))
                .blockingAwait();

    }

    private Completable checkThatRideHasUpdatedTheUser(JsonObject message) {
        return Completable.timer(5, TimeUnit.SECONDS).doOnComplete(() -> {
            String userid = message.getJsonObject("user").getString("id");
            LOGGER.info("Trying to get user with id {}",userid);
            User user = cache.get(userid);
            assertNotNull(user);
            assertEquals(User.STATE_ON_RIDE,user.getCurrentState());
        });
    }

    private Completable sendRideStartedEvent(JsonObject message) {
        return Completable.fromAction(() -> {
            LOGGER.info("> TEST: sending ride started event to the test-ride-event buss\n {}",message.encodePrettily());
            vertx.eventBus().send("test-ride-event",message);
        });
    }

    private Completable sendNewUserInLine(JsonObject message) {
        return Completable.fromAction(() -> {
            LOGGER.info("> TEST: sending event to the test-user-event buss\n {}",message.encodePrettily());
            vertx.eventBus().send("test-user-event", message);
        });

    }

    private Completable checkThatUserWasCreated(JsonObject message) {
        return Completable.timer(5, TimeUnit.SECONDS).doOnComplete(() -> {
            String userid = message.getJsonObject("user").getString("id");
            LOGGER.info("Trying to get user with id {}",userid);
            User user = cache.get(message.getJsonObject("user").getString("id"));
            assertNotNull(user);
        });

    }


    private Completable deployAMQPVerticle() {
        EventBusToAmqp user_queue = new EventBusToAmqp();
        user_queue.setAddress("test-user-event");
        user_queue.setQueue(Event.USER_IN_LINE);

        EventBusToAmqp ride_queue = new EventBusToAmqp();
        ride_queue.setAddress("test-ride-event");
        ride_queue.setQueue(Event.RIDE_STARTED);

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost("localhost")
                .setPort(5672)
                .setUser("user")
                .setPassword("user123")
                .addEventBusToAmqp(user_queue)
                .addEventBusToAmqp(ride_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }
}

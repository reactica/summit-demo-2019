package com.redhat.coderland.ride.eventgen;

import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpToEventBus;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.reactica.model.Event;
import com.redhat.coderland.reactica.model.RideStartedEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RideSimulatorTest {

    private static final LinkedBlockingDeque<Event> RIDE_EVENTS = new LinkedBlockingDeque<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(RideSimulatorTest.class);

    @Inject
    Vertx vertx;

    @Test
    public void testRideSimulation() throws Exception {
        LOGGER.info("TEST > Starting the UserSimulation Test");
        deployAMQPVerticle()
                .andThen(createConsumer())
                .subscribe();
        RIDE_EVENTS.clear();

        //Event is generatored every 10 seconds, but we need time to start the bridge
        Event event = RIDE_EVENTS.poll(15, TimeUnit.SECONDS);
        assertNotNull(event);
    }

    private Completable createConsumer() {
        return Completable.fromAction( () -> {
            vertx.eventBus().<JsonObject>localConsumer("test",message -> {
                JsonObject userEventJson = message.body();
                assertNotNull(userEventJson);

                assertEquals(userEventJson.getString("eventType"),Event.EventType.RIDE_STARTED.toString());
                userEventJson.remove("eventType");
                userEventJson.remove("queueName");

                Event userEvent = userEventJson.mapTo(RideStartedEvent.class);
                assertNotNull(userEvent);
                RIDE_EVENTS.push(userEvent);
            });
        });
    }


    private Completable deployAMQPVerticle() {
        AmqpToEventBus user_queue = new AmqpToEventBus();
        user_queue.setAddress("test");
        user_queue.setQueue(Event.RIDE_STARTED);

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost("localhost")
                .setPort(5672)
                .setUser("user")
                .setPassword("user123")
                .addAmqpToEventBus(user_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }
}

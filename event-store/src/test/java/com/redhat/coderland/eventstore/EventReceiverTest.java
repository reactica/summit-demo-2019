package com.redhat.coderland.eventstore;

import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.bridge.EventBusToAmqp;
import com.redhat.coderland.reactica.model.Event;
import com.redhat.coderland.reactica.model.User;
import io.quarkus.test.junit.QuarkusTest;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

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


    private Future<Void> testDone;

    @Test
    public void testUserSimulation() throws Exception {

        testDone = Future.future();
        Event randomUserInLineEvent = eventHelper.createRandomUserInLineEvent();
        JsonObject message = JsonObject.mapFrom(randomUserInLineEvent);
        deployAMQPVerticle()
                .andThen(sendMessage(message))
                .andThen(checkMessage(message))
                .subscribe(() -> System.out.println("OK"),
                        err -> Assertions.fail("Failed"));

    }

    private Completable sendMessage(JsonObject message) {
        return Completable.fromAction(() -> {
            LOGGER.info("> TEST: sending event to the test buss\n {}",message.encodePrettily());
            vertx.eventBus().send("test", message);
        });

    }

    private Completable checkMessage(JsonObject message) {
        return Completable.fromAction(() -> cache.getAsync(message.getJsonObject("user").getString("id")));
    }


    private Completable deployAMQPVerticle() {
        EventBusToAmqp user_queue = new EventBusToAmqp();
        user_queue.setAddress("test");
        user_queue.setQueue(Event.USER_IN_LINE);

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost("localhost")
                .setPort(5672)
                .setUser("user")
                .setPassword("user123")
                .addEventBusToAmqp(user_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }
}

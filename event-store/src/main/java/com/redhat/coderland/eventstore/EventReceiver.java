package com.redhat.coderland.eventstore;

import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpToEventBus;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.reactica.model.Event;
import com.redhat.coderland.reactica.model.Ride;
import com.redhat.coderland.reactica.model.User;
import com.redhat.coderland.reactica.model.UserInLineEvent;
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
        vertx.eventBus().<JsonObject>consumer(Event.USER_IN_LINE,message -> {
            LOGGER.info(">>> RECEIVED EVENT");
            JsonObject userEventJson = message.body();

            userEventJson.remove("eventType");
            userEventJson.remove("queueName");

            UserInLineEvent userEvent = userEventJson.mapTo(UserInLineEvent.class);
            User user = userEvent.getUser();
            Ride ride = userEvent.getRide();
            user.setRideId(ride.getName());
            LOGGER.info(">>> RECEIVED EVENT from {} for ride {}",user.getName(),ride.getName());
            cache.putAsync(user.getId(),user);
            LOGGER.info(">>> STORED EVENT from {} for ride {} in the data grid",user.getName(),ride.getName());

        });
        deployAMQPVerticle().subscribe(CompletableHelper.toObserver(bridgeStarted));

    }

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application is starting...");
    }


    private Completable deployAMQPVerticle() {
        AmqpToEventBus user_queue = new AmqpToEventBus();
        user_queue.setAddress(Event.USER_IN_LINE);
        user_queue.setQueue(Event.USER_IN_LINE);

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost(host.orElse("localhost"))
                .setPort(port.orElse(5672))
                .setUser(user.orElse("user"))
                .setPassword(password.orElse("user123"))
                .addAmqpToEventBus(user_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }
}

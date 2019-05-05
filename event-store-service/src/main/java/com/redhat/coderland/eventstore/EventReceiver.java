package com.redhat.coderland.eventstore;

import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpToEventBus;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.reactica.model.*;
import io.quarkus.runtime.StartupEvent;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
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
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
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


    @PostConstruct
    public void start() {

        deployAMQPVerticle()
                .andThen(startUserInLineReceiver())
                .andThen(startRideStartedEventReceiver())
                .andThen(startUserOnRideEventReceiver())
                .andThen(startRideCompletedEventReceiver())
                .andThen(startUserCompletedEventReceiver())
                .andThen(startLeavingEventReceiver())
                .subscribe();

    }


    private Completable startUserInLineReceiver() {
        return Completable.fromAction( () -> {
            LOGGER.info("STARTING USER RECEIVER LISTENING TO LOCAL BUS {}",Event.USER_IN_LINE);
            vertx.eventBus().<JsonObject>localConsumer(Event.USER_IN_LINE, message -> {
                LOGGER.info("RECEIVED USER IN LINE EVENT");
                JsonObject userEventJson = message.body();
                userEventJson.remove("eventType");
                userEventJson.remove("queueName");

                UserInLineEvent userEvent = userEventJson.mapTo(UserInLineEvent.class);
                User user = userEvent.getUser();
                user.putInQueue();
                LOGGER.info(">>> RECEIVED {} from {} for ride {}",Event.USER_IN_LINE, user.getName(),user.getRideId());
                cache.putAsync(user.getId(),user);
            });
        });
    }

    private Completable startRideStartedEventReceiver() {
        return Completable.fromAction(() -> {
            LOGGER.info("STARTING EVENT RECEIVER LISTENING TO LOCAL BUS {}",Event.RIDE_STARTED);
            vertx.eventBus().<JsonObject>localConsumer(Event.RIDE_STARTED, message -> {
                LOGGER.info("RECEIVED START RIDE EVENT");

                cache.entrySet().forEach(entry -> {
                    LOGGER.info(entry.getValue().toString());
                });

                JsonObject rideEventJson = message.body();

                rideEventJson.remove("eventType");
                rideEventJson.remove("queueName");

                RideStartedEvent rideStartedEvent = rideEventJson.mapTo(RideStartedEvent.class);
                String attraction = rideStartedEvent.getRide().getAttractionId();
                int capacity = rideStartedEvent.getCapacity();

                LOGGER.info("Searching for {} users on ride {}",capacity,attraction );

                QueryFactory queryFactory = Search.getQueryFactory(cache);

                Query query = queryFactory.from(User.class)
                        .having("rideId").equal(attraction)
                        .and()
                        .having("currentState").in(User.STATE_IN_QUEUE)
                        .orderBy("enterQueueTime", SortOrder.ASC)
                        .maxResults(capacity)
                        .build();

                List<User> users = query.<User>list();
                LOGGER.info("Found {} users on in queue for ride {}",users.size(),capacity);


                users.forEach(user -> {
                    vertx.eventBus().send(Event.USER_ON_RIDE, JsonObject.mapFrom(new UserOnRideEvent(user)));
                });
                Ride ride = rideStartedEvent.getRide();
                ride.setUserOnTheRide(users);
                RideCompletedEvent rideCompletedEvent = new RideCompletedEvent(ride,10); //TODO replace hard-coded leave Time.
                vertx.setTimer(rideStartedEvent.getRideTime()*1000, t -> {
                   vertx.eventBus().send(Event.RIDE_COMPLETED,rideCompletedEvent);
                });
            });
        });
    }

    private Completable startRideCompletedEventReceiver() {
        return Completable.fromAction( () -> {
            vertx.eventBus().<JsonObject>localConsumer(Event.RIDE_COMPLETED, message -> {
                LOGGER.info("RECEIVED RIDE COMPLETED EVENT");
                JsonObject rideCompletedEventJson = message.body();
                rideCompletedEventJson.remove("eventType");
                rideCompletedEventJson.remove("queueName");

                RideCompletedEvent rideEvent = rideCompletedEventJson.mapTo(RideCompletedEvent.class);
                rideEvent.getRide().getUserOnTheRide().forEach( user -> {
                    vertx.eventBus().send(Event.USER_COMPLETED,new UserCompletedRideEvent(user));
                });
            });
        });
    }

    private Completable startUserCompletedEventReceiver() {
        return Completable.fromAction( () -> {
            vertx.eventBus().<JsonObject>localConsumer(Event.USER_COMPLETED, message -> {
                LOGGER.info("RECEIVED USER COMPLETED EVENT");
                JsonObject userCompletedEventJson = message.body();
                userCompletedEventJson.remove("eventType");
                userCompletedEventJson.remove("queueName");

                UserCompletedRideEvent userEvent = userCompletedEventJson.mapTo(UserCompletedRideEvent.class);
                User user = userEvent.getUser();
                user.completed();
                cache.putAsync(user.getId(),user);
                vertx.setTimer(10*1000, t -> {
                   vertx.eventBus().send(Event.USER_LEAVING,new UserLeavingRideEvent(user));
                });
            });
        });
    }

    private Completable startLeavingEventReceiver() {
        return Completable.fromAction( () -> {
            vertx.eventBus().<JsonObject>localConsumer(Event.USER_LEAVING, message -> {
                LOGGER.info("RECEIVED USER COMPLETED EVENT");
                JsonObject userLeavingEventJson = message.body();
                userLeavingEventJson.remove("eventType");
                userLeavingEventJson.remove("queueName");

                UserLeavingRideEvent userEvent = userLeavingEventJson.mapTo(UserLeavingRideEvent.class);
                User user = userEvent.getUser();
                cache.remove(user.getId());
            });
        });
    }

    private Completable startUserOnRideEventReceiver() {
        return Completable.fromAction(() -> {
            vertx.eventBus().<JsonObject>localConsumer(Event.USER_ON_RIDE, message -> {
                JsonObject userEventJson = message.body();

                userEventJson.remove("eventType");
                userEventJson.remove("queueName");

                UserOnRideEvent userEvent = userEventJson.mapTo(UserOnRideEvent.class);
                User user = userEvent.getUser();
                user.onRide();
                LOGGER.info(">>> RECEIVED {} from {} for ride {}", Event.USER_ON_RIDE, user.getName(), user.getRideId());
                cache.putAsync(user.getId(), user);
            });
        });

    }

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application is starting...");
    }


    private Completable deployAMQPVerticle() {
        AmqpToEventBus user_in_line_event_bridge = new AmqpToEventBus();
        user_in_line_event_bridge.setAddress(Event.USER_IN_LINE);
        user_in_line_event_bridge.setQueue(Event.USER_IN_LINE);

        AmqpToEventBus ride_started_event_bridge = new AmqpToEventBus();
        ride_started_event_bridge.setAddress(Event.RIDE_STARTED);
        ride_started_event_bridge.setQueue(Event.RIDE_STARTED);

        AmqpToEventBus ride_completed_event_bridge = new AmqpToEventBus();
        ride_completed_event_bridge.setAddress(Event.RIDE_COMPLETED);
        ride_completed_event_bridge.setQueue(Event.RIDE_COMPLETED);

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost(host.orElse("localhost"))
                .setPort(port.orElse(5672))
                .setUser(user.orElse("user"))
                .setPassword(password.orElse("user123"))
                .addAmqpToEventBus(user_in_line_event_bridge)
                .addAmqpToEventBus(ride_started_event_bridge)
                .addAmqpToEventBus(ride_completed_event_bridge);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }
}

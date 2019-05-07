package com.redhat.coderland.qlc;

import com.redhat.coderland.reactica.model.User;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/queue-line-calculator/{rideId}")
@ApplicationScoped
public class QueueLineCalculatorService {

    private static Logger LOGGER = LoggerFactory.getLogger(QueueLineCalculatorService.class);

    public final static int DEFAULT_RIDE_CAPACITY = 5;

    private Map<String,Map<String,Session>> rideSessions = new ConcurrentHashMap<>();

    @Inject
    RemoteCache<String,User> cache;

    @Inject @ConfigProperty(name = "coderland.config.generateevent.ride.interval")
    Optional<String> interval;

    @Inject @ConfigProperty(name = "coderland.rides")
    Optional<String> rides;

    @Inject @ConfigProperty(name = "coderland.config.ride.capacity")
    Optional<Integer> defaultCapacityConfig;



    @OnOpen
    public void onOpen(Session session, @PathParam("rideId") String rideId) {
        LOGGER.info("Opening WebSocket Session with session id {}", session.getId());
        Map<String,Session> sessions = rideSessions.get(rideId);
        if(sessions==null) {
            synchronized (this) {
                if(sessions==null) {
                    sessions = new ConcurrentHashMap<>();
                    rideSessions.put(rideId, sessions);
                } else {
                    sessions = rideSessions.get(rideId);
                }
            }
        }
        sessions.put(session.getId(),session);
    }

    @OnClose
    public void onClose(Session session) {
        LOGGER.info("Closing WebSocket Session with session id {}", session.getId());

    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOGGER.error("Error opening WebSocket Session with session id {}", session.getId(),throwable);
    }

    @Scheduled(every = "5s")
    public void updateQueueLine() {
        rideSessions.forEach( (rideId,sessions) -> {
            int waitTimeInSeconds = getWaitTimeInSeconds(rideId);
            sessions.forEach((sessionId,session) -> {
                session.getAsyncRemote().sendText(getJsonMessage(waitTimeInSeconds,rideId));
            });
        });
    }

    private int getWaitTimeInSeconds(String rideId) {
        QueryFactory qf = Search.getQueryFactory(cache);

        Query query = qf.from(User.class)
                .having("rideId").equal(rideId)
                .and()
                .having("currentState").equal(User.STATE_IN_QUEUE)
                .build();
        int queueSize = query.getResultSize();

        int secondsBetweenRide = (int) Duration.parse("PT"+interval.orElse("10s")).getSeconds() * rides.orElse("roller-coaster").split(",").length;

        double ridesPerMinute = 60/secondsBetweenRide;
        //adjust to two decimals
        ridesPerMinute = Math.round(ridesPerMinute*100)/100;

        //Adjust for number of rides
        Optional<Integer> capacityConfig = ConfigProvider.getConfig().getOptionalValue("coderland.config." + rideId + ".capacity",Integer.class);
        int capacity = capacityConfig.orElse(defaultCapacityConfig.orElse(DEFAULT_RIDE_CAPACITY));
        return (int) Math.round(queueSize / (capacity * ridesPerMinute));
    }

    private String getJsonMessage(int waitTime, String rideId) {
        JsonObject jsonObject = Json.createObjectBuilder().add("rideId", rideId).add("estimatedWaitTime", waitTime).build();
        StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(jsonObject);
        return writer.toString();
    }

}

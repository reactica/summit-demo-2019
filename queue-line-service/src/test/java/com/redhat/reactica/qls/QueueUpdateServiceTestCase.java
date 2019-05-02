package com.redhat.reactica.qls;

import com.redhat.coderland.reactica.model.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.infinispan.client.hotrod.RemoteCache;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.websocket.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class QueueUpdateServiceTestCase {

  Logger LOGGER = LoggerFactory.getLogger("WS-TEST-CASE");

  private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

  private static User user1, user2, user3;
  private static Map<String, User> users;

  static {
    user1 = new User();
    user1.setId("1");
    user1.setName("Thomas");
    user1.setRideId("roller-coaster");
    user1.setEnterQueueTime(System.nanoTime());
    user1.setCurrentState("ON_RIDE");

    user2 = new User();
    user2.setId("2");
    user2.setName("Clement");
    user2.setRideId("screamer");
    user2.setEnterQueueTime(System.nanoTime());
    user2.setCurrentState("IN_QUEUE");

    user3 = new User();
    user3.setId("3");
    user3.setName("JAMES");
    user3.setRideId("roller-coaster");
    user3.setEnterQueueTime(System.nanoTime());
    user3.setCurrentState("COMPLETED_RIDE");

    users = Arrays.asList(new User[]{user1, user2, user3}).stream().collect(Collectors.toMap(User::getId, item -> item));

  }

  @TestHTTPResource("/queue-line-update/roller-coaster")
  URI uri;

  @Inject
  RemoteCache<String, User> cache;

  @Test
  public void testWebsocketChat() throws Exception {

    Thread.sleep(1000);
    MESSAGES.clear(); //Clear out old values


    try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {

      //Test adding a user
      cache.put(user1.getId(), user1);
      String message = MESSAGES.poll(1, TimeUnit.SECONDS);
      assertNotNull(message);
      JsonObject incomingEvent = new JsonObject(message);
      String action = incomingEvent.getString("action");
      assertEquals(action, "new");
      JsonObject incomingUser = incomingEvent.getJsonObject("user");
      assertTrue(user1.equals(incomingUser.mapTo(User.class)));


      //Test updating a user
      User user = cache.get(user1.getId());
      user.completed();
      cache.put(user.getId(), user);
      incomingEvent = new JsonObject(MESSAGES.poll(1, TimeUnit.SECONDS));
      action = incomingEvent.getString("action");
      assertEquals(action, "update");
      incomingUser = incomingEvent.getJsonObject("user");
      assertTrue(user.equals(incomingUser.mapTo(User.class)));

      //Test adding a user to a different ride
      cache.put(user2.getId(),user2);
      assertNull(MESSAGES.poll(1,TimeUnit.SECONDS));


      cache.removeAsync(user.getId());
      session.close();
    }
  }

  @ClientEndpoint
  public static class Client {
    Logger LOGGER = LoggerFactory.getLogger("WS-TEST-CLIENT");
    @OnOpen
    public void open() {
    }

    @OnMessage
    void message(String msg) {
      LOGGER.info(">>> MESSAGE FROM SERVER = {}",msg);
      MESSAGES.add(msg);
    }

  }
}

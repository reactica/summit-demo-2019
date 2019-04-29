package com.redhat.reactica.qls;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.redhat.coderland.reactica.model.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.websocket.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@QuarkusTest
public class QueueUpdateServiceTestCase {

  private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

  private static User user1, user2, user3;
  private static Map<String, User> users;

  static {
    user1 = new User();
    user1.setId("1");
    user1.setName("Thomas");
    user1.setRideId("reactica");
    user1.setEnterQueueTime(System.nanoTime());
    user1.setCurrentState("ON_RIDE");

    user2 = new User();
    user2.setId("2");
    user2.setName("Clement");
    user2.setRideId("reactica");
    user2.setEnterQueueTime(System.nanoTime());
    user2.setCurrentState("IN_QUEUE");

    user3 = new User();
    user3.setId("3");
    user3.setName("JAMES");
    user3.setRideId("reactica");
    user3.setEnterQueueTime(System.nanoTime());
    user3.setCurrentState("COMPLETED_RIDE");

    users = Arrays.asList(new User[]{user1, user2, user3}).stream().collect(Collectors.toMap(User::getId, item -> item));

  }

  @TestHTTPResource("/queue-line-update")
  URI uri;

  @Inject
  RemoteCache<String, User> cache;

  @Inject
  Gson gson;

  @Inject
  JsonParser parser;

  @Test
  public void testWebsocketChat() throws Exception {

    Thread.sleep(1000);
    MESSAGES.clear(); //Clear out old values


    try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {


      cache.put(user1.getId(), user1);
      JsonObject incomingEvent = parser.parse(MESSAGES.poll(1, TimeUnit.SECONDS)).getAsJsonObject();
      String action = incomingEvent.get("action").getAsString();
      Assertions.assertEquals(action, "new");
      Assertions.assertTrue(user1.equals(gson.fromJson(incomingEvent.getAsJsonObject("user"), User.class)));


      User user = cache.get(user1.getId());
      user.setCurrentState("COMPLETED_RIDE");
      cache.put(user.getId(), user);


      incomingEvent = parser.parse(MESSAGES.poll(1, TimeUnit.SECONDS)).getAsJsonObject();
      action = incomingEvent.get("action").getAsString();
      Assertions.assertEquals(action, "update");
      Assertions.assertTrue(user.equals(gson.fromJson(incomingEvent.getAsJsonObject("user"), User.class)));
      session.close();
    }
  }

  @ClientEndpoint
  public static class Client {

    @OnOpen
    public void open() {
    }

    @OnMessage
    void message(String msg) {
      System.out.println(">>> MESSAGE FROM SERVER = " + msg);
      MESSAGES.add(msg);
    }

  }
}

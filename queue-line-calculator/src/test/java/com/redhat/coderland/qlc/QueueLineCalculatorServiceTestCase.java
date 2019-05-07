package com.redhat.coderland.qlc;

import com.redhat.coderland.reactica.model.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.infinispan.client.hotrod.RemoteCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.*;
import java.io.StringReader;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class QueueLineCalculatorServiceTestCase {

  private static final int USERS = 100;
  Logger LOGGER = LoggerFactory.getLogger("WS-TEST-CASE");

  private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();


  @TestHTTPResource("/queue-line-calculator/roller-coaster")
  URI uri;

  @Inject
  RemoteCache<String, User> cache;

  @Test
  public void testWebsocketChat() throws Exception {

    MESSAGES.clear(); //Clear out old values

    Session session=null;
    try {
      session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri);
      String message = MESSAGES.poll(7, TimeUnit.SECONDS);
      assertNotNull(message);
      JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
      assertNotNull(jsonObject);
      int estimatedWaitTime = jsonObject.getInt("estimatedWaitTime");
      Duration duration = Duration.ZERO.plusMinutes(estimatedWaitTime);
      LOGGER.info("Estimated queue time is {}", duration.toString());
      assertEquals(duration.getSeconds(),5*60);

    } finally {
      if(session!=null && session.isOpen())
        session.close();
    }
  }

  @BeforeEach
  public void setupTest() {
    for(int i = 1; i< USERS; i++) {
      User user = createUser(i,"roller-coaster");
      cache.put(Integer.toString(i), user);
    }
  }

  @AfterEach
  public void tearDownTest() {
    for(int i = 1; i< USERS; i++) {
      cache.remove(Integer.toString(i));
    }
  }

  private User createUser(int i, String rideId) {
    User user = new User(Integer.toString(i),"Test User " + i);
    user.setRideId(rideId);
    user.putInQueue();
    return user;
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

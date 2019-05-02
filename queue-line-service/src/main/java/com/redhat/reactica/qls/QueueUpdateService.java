package com.redhat.reactica.qls;

import com.redhat.coderland.reactica.model.User;
import com.redhat.coderland.reactica.model.UserEvent;
import io.vertx.core.json.JsonObject;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/queue-line-update/{rideId}")
@ApplicationScoped
public class QueueUpdateService {

  Logger LOGGER = LoggerFactory.getLogger("QueueUpdateService");

  Map<Session,ContinuousQueryListener<String,User>> sessions = new ConcurrentHashMap<>();

  @Inject
  RemoteCache<String,User> cache;




  @OnOpen
  public void onOpen(Session session, @PathParam("rideId") String rideId) {
    LOGGER.info("Opening WebSocket Session with session id {} for ride {}", session.getId(),rideId);

    ContinuousQuery<String, User> continuousQuery = Search.getContinuousQuery(cache);


    QueryFactory queryFactory = Search.getQueryFactory(cache);
    Query query = queryFactory.from(User.class)
            .having("rideId").eq(rideId)
            .and()
            .having("currentState").in(User.STATE_IN_QUEUE,User.STATE_ON_RIDE,User.STATE_RIDE_COMPLETED)
            .build();

    ContinuousQueryListener<String,User> listener = new ContinuousQueryListener<String, User>() {
      @Override
      public void resultJoining(String id, User user) {
        LOGGER.info("A new user with id {} has been added to the data grid",user.getId());
        JsonObject userEvent = JsonObject.mapFrom(new UserEvent("new", user));
        LOGGER.info("Sending user event\n{}",userEvent.encodePrettily());
        session.getAsyncRemote().sendObject(userEvent.encode());
      }

      @Override
      public void resultUpdated(String id, User user) {
        LOGGER.info("A user with id {} has been updated in the data grid",user.getId());
        session.getAsyncRemote().sendObject(JsonObject.mapFrom(new UserEvent("update",user)).encode());
      }

      @Override
      public void resultLeaving(String id) {
        LOGGER.info("A user with id {} has been removed from the data grid",id);
        User user = new User();
        user.setId(id);
        session.getAsyncRemote().sendObject(JsonObject.mapFrom(new UserEvent("delete",user)).encode());
      }
    };

    sessions.put(session,listener);
    continuousQuery.addContinuousQueryListener(query, listener);
  }



  @OnClose
  public void onClose(Session session) {
    LOGGER.info("Closing WebSocket Session with session id {}", session.getId());
    ContinuousQuery<String, User> continuousQuery = Search.getContinuousQuery(cache);
    continuousQuery.removeContinuousQueryListener(sessions.get(session));
    sessions.remove(session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    LOGGER.error("Error opening WebSocket Session with session id {}", session.getId(),throwable);
    ContinuousQueryListener<String, User> listener = sessions.get(session);
    if(listener!=null) {
      ContinuousQuery<String, User> continuousQuery = Search.getContinuousQuery(cache);
      continuousQuery.removeContinuousQueryListener(listener);
    }
    sessions.remove(session);
  }


}

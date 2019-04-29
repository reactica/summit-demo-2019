package com.redhat.reactica.qls;

import com.google.gson.Gson;
import com.redhat.coderland.reactica.model.User;
import com.redhat.coderland.reactica.model.UserEvent;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/queue-line-update")
@ApplicationScoped
public class QueueUpdateService {

  Map<Session,ContinuousQueryListener<String,User>> sessions = new ConcurrentHashMap<>();


  @Inject
  Gson gson;

  @Inject
  RemoteCache<String,User> cache;




  @OnOpen
  public void onOpen(Session session) {

    ContinuousQuery<String, User> continuousQuery = Search.getContinuousQuery(cache);


    QueryFactory queryFactory = Search.getQueryFactory(cache);
    Query query = queryFactory.from(User.class)
            .having("rideId").eq("reactica")
            .and()
            .having("currentState").in(User.STATE_IN_QUEUE,User.STATE_ON_RIDE,User.STATE_RIDE_COMPLETED)
            .build();

    ContinuousQueryListener<String,User> listener = new ContinuousQueryListener<String, User>() {
      @Override
      public void resultJoining(String id, User user) {
        session.getAsyncRemote().sendObject(gson.toJson(new UserEvent("new",user)));
      }

      @Override
      public void resultUpdated(String id, User user) {
        session.getAsyncRemote().sendObject(gson.toJson(new UserEvent("update",user)));
      }

      @Override
      public void resultLeaving(String id) {
        User user = new User();
        user.setId(id);
        session.getAsyncRemote().sendObject(gson.toJson(new UserEvent("delete",user)));
      }
    };

    sessions.put(session,listener);
    continuousQuery.addContinuousQueryListener(query, listener);
  }



  @OnClose
  public void onClose(Session session) {
    ContinuousQuery<String, User> continuousQuery = Search.getContinuousQuery(cache);
    continuousQuery.removeContinuousQueryListener(sessions.get(session));
    sessions.remove(session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    ContinuousQueryListener<String, User> listener = sessions.get(session);
    if(listener!=null) {
      ContinuousQuery<String, User> continuousQuery = Search.getContinuousQuery(cache);
      continuousQuery.removeContinuousQueryListener(listener);
    }
    sessions.remove(session);
  }

  @PostConstruct
  public void start() {

  }

}

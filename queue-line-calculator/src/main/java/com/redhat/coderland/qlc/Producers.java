package com.redhat.coderland.qlc;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.redhat.coderland.reactica.model.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class Producers {

  @Inject @ConfigProperty(name = "infinispan.host",defaultValue = "127.0.0.1")
  String dgHost;

  @Inject @ConfigProperty(name = "infinispan.port",defaultValue = "11222")
  String dgPort;



  @Produces

  UserMarshaller userMarshaller() {
    return new UserMarshaller();
  }

  @Produces
  Gson createGsonObject() {
    return new Gson();
  }

  @Produces
  JsonParser createJsonParser() {
    return new JsonParser();
  }

  @Produces
  RemoteCache<String, User> getCache() throws IOException {
    ConfigurationBuilder clientBuilder = new ConfigurationBuilder();
    clientBuilder.addServer()
      .host(dgHost).port(Integer.parseInt(dgPort))
      .marshaller(new ProtoStreamMarshaller())
      .clientIntelligence(ClientIntelligence.BASIC);

    RemoteCacheManager manager = new RemoteCacheManager(clientBuilder.build());
    UserMarshaller marshaller = new UserMarshaller();
    SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(manager);
    FileDescriptorSource fds = new FileDescriptorSource();
    fds.addProtoFiles("META-INF/User.proto");
    serCtx.registerProtoFiles(fds);
    serCtx.registerMarshaller(marshaller);
    return manager.getCache();
  }
}

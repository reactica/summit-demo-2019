package com.redhat.coderland.qlc;

import com.redhat.coderland.reactica.model.User;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class UserMarshaller implements MessageMarshaller<User> {
  @Override
  public User readFrom(ProtoStreamReader reader) throws IOException {
    User user = new User();
    user.setId(reader.readString("id"));
    user.setName(reader.readString("name"));
    user.setRideId(reader.readString("rideId"));
    user.setCurrentState(reader.readString("currentState"));
    user.setEnterQueueTime(reader.readLong("enterQueueTime"));
    user.setCompletedRideTime(reader.readLong("completedRideTime"));
    return user;
  }

  @Override
  public void writeTo(ProtoStreamWriter writer, User user) throws IOException {
    writer.writeString("id", user.getId());
    writer.writeString("name", user.getName());
    writer.writeString("rideId", user.getRideId());
    writer.writeString("currentState", user.getCurrentState());
    writer.writeLong("enterQueueTime", user.getEnterQueueTime());
    writer.writeLong("completedRideTime", user.getCompletedRideTime());
  }

  @Override
  public Class<? extends User> getJavaClass() {
    return User.class;
  }

  @Override
  public String getTypeName() {
    return User.class.getName();
  }


}

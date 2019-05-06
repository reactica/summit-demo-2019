package com.redhat.coderland.bridge;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.amqp.*;
import io.vertx.reactivex.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AmqpVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger("AMQP Verticle");

    @Override
    public void start(Future<Void> completion) {
        LOGGER.debug("Starting AMQP verticle");
        AmqpConfiguration configuration = config().mapTo(AmqpConfiguration.class);
        AmqpClientOptions clientOptions = new AmqpClientOptions()
                .setHost(configuration.getHost())
                .setPort(configuration.getPort())
                .setUsername(configuration.getUser())
                .setPassword(configuration.getPassword());

        AmqpClient amqpClient = AmqpClient.create(this.vertx.getDelegate(), clientOptions);

        Future<AmqpConnection> future = Future.future();
        amqpClient.connect(connection  -> {
            if (connection.succeeded()) {
                LOGGER.debug("Connected to the AMQP broker {}:{}", configuration.getHost(),configuration.getPort());
                future.complete(connection.result());
            } else {
                LOGGER.error("Unable to connect to AMQP broker {}:{}", configuration.getHost(),configuration.getPort(), connection.cause());
                future.fail(connection.cause());
            }
        });
        future
                .compose(connection -> {
                    LOGGER.debug("Weaving event bus to AMQP connections");
                    configuration.getEventBusToAmqp().forEach(bridge -> {
                        LOGGER.info(String.format("Bridging Event Bus {%s} => AMQP {%s}", bridge.getAddress(), bridge.getQueue()));
                        connection.createSender(bridge.getQueue(),done -> {
                            if (done.failed()) {
                                LOGGER.error("Unable to create a sender for {}",bridge.getQueue(),done.cause());
                            } else {
                                AmqpSender sender = done.result();
                                vertx.eventBus().<JsonObject>consumer(bridge.getAddress(), msg -> {
                                    JsonObject properties = new JsonObject();
                                    msg.headers().names().forEach(key -> properties.put(key,msg.headers().get(key)));
                                    AmqpMessage message = AmqpMessage.create()
                                            .durable(true)
                                            .withJsonObjectAsBody(msg.body())
                                            .applicationProperties(properties)
                                            .build();
                                    sender.send(message);
                                });
                            }
                        });
                    });
                    return Future.succeededFuture(connection);
                })
                .compose(connection -> {
                    LOGGER.debug("Weaving AMQP to event bus connections");
                    // Start consumers to send AMQP messages to the event bus (AMQP -> Event Bus
                    List<AmqpToEventBus> bridges = configuration.getAmqpToEventBus();
                    bridges.forEach(bridge -> {
                        LOGGER.info(String.format("Bridging AMQP {%s} => Event Bus {%s}", bridge.getQueue(), bridge.getAddress()));
                        connection.createReceiver(bridge.getQueue(), new AmqpReceiverOptions().setDurable(true), done -> {
                            if (done.failed()) {
                                LOGGER.error("Unable to create a receiver for {}",bridge.getQueue(),done.cause());
                            } else {
                                AmqpReceiver receiver = done.result();
                                receiver.handler(message -> {
                                    LOGGER.debug(String.format(">>> RECEIVED MESSAGE on %s", bridge.getQueue()));
                                    JsonObject properties = message.applicationProperties();
                                    LOGGER.debug(String.format(">>> MESSAGE PROPERTIES are %s", properties.encodePrettily()));
                                    JsonObject body = message.bodyAsJsonObject();
                                    LOGGER.debug(String.format(">>> MESSAGE is %s", body.encodePrettily()));

                                    DeliveryOptions deliveryOptions = new DeliveryOptions();
                                    properties.stream().forEach(entry -> deliveryOptions.addHeader(entry.getKey(),entry.getValue().toString()));
                                    if (bridge.isPublish()) {
                                        LOGGER.debug(String.format(">>> PUBLISHING MESSAGE on %s", bridge.getQueue()));
                                        vertx.eventBus().publish(bridge.getAddress(), body, deliveryOptions);
                                    } else {
                                        LOGGER.debug(String.format(">>> SENDING MESSAGE on %s", bridge.getQueue()));
                                        vertx.eventBus().send(bridge.getAddress(), body, deliveryOptions);
                                    }
                                });
                            }
                        });
                    });
                    return Future.succeededFuture(connection);
                })
                .setHandler(res -> completion.handle(res.mapEmpty()));

    }
}

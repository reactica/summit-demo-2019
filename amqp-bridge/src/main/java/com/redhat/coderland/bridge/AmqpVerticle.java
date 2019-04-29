package com.redhat.coderland.bridge;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.*;
import io.vertx.reactivex.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class AmqpVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LogManager.getLogger("AMQP Verticle");

    @Override
    public void start(Future<Void> completion) {
        LOGGER.info("Starting AMQP verticle");
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
                LOGGER.info("Connected to the AMQP broker {}", configuration.getHost());
                future.complete(connection.result());
            } else {
                LOGGER.info("Unable to connect to AMQP broker {}", configuration.getHost(), connection.cause());
                future.fail(connection.cause());
            }
        });
        future
                .compose(connection -> {
                    LOGGER.info("Weaving event bus to AMQP connections");
                    configuration.getEventBusToAmqp().forEach(bridge -> {
                        LOGGER.info("Event Bus {} => AMQP {}", bridge.getAddress(), bridge.getQueue());
                        connection.createSender(bridge.getQueue(),done -> {
                            if (done.failed()) {
                                LOGGER.error("Unable to create a sender");
                            } else {
                                AmqpSender sender = done.result();
                                vertx.eventBus().<JsonObject>consumer(bridge.getAddress(), msg -> {
                                    JsonObject properties = new JsonObject();
                                    msg.headers().names().forEach(key -> properties.put(key,msg.headers().get(key)));
                                    AmqpMessage message = AmqpMessage.create()
                                            .address(bridge.getQueue())
                                            .withBody(msg.body().encode())
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
                    LOGGER.info("Weaving AMQP to event bus connections");
                    // Start consumers to send AMQP messages to the event bus (AMQP -> Event Bus
                    List<AmqpToEventBus> bridges = configuration.getAmqpToEventBus();
                    bridges.forEach(bridge -> {
                        LOGGER.info("AMQP {} => Event Bus {}", bridge.getQueue(), bridge.getAddress());
                        connection.createReceiver(bridge.getQueue(), done -> {
                            if (done.failed()) {
                                LOGGER.error("Unable to create a reciever");
                            } else {
                                AmqpReceiver receiver = done.result();
                                receiver.handler(message -> {
                                    JsonObject properties = message.applicationProperties();
                                    JsonObject body = message.bodyAsJsonObject();
                                    DeliveryOptions deliveryOptions = new DeliveryOptions();
                                    properties.stream().forEach(entry -> deliveryOptions.addHeader(entry.getKey(),entry.getValue().toString()));
                                    if (bridge.isPublish()) {
                                        vertx.eventBus().publish(bridge.getAddress(), body, deliveryOptions);
                                    } else {
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

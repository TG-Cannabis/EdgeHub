package com.tgcannabis.edgehub.mqtt;

import com.tgcannabis.edgehub.ws.WebSocketBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageHandler.class);
    private final WebSocketBroadcaster broadcaster;

    public MqttMessageHandler(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = message.getPayload().toString();
        logger.info("Received MQTT message: " + topic + " -> " + payload);

        broadcaster.broadcast(topic, payload);
    }
}

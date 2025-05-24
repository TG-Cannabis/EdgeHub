package com.tgcannabis.edgehub.mqtt;

import com.tgcannabis.edgehub.ws.WebSocketBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MqttMessageHandlerTest {
    private WebSocketBroadcaster broadcaster;
    private MqttMessageHandler handler;

    @BeforeEach
    void setup() {
        broadcaster = mock(WebSocketBroadcaster.class);
        handler = new MqttMessageHandler(broadcaster);
    }

    @Test
    void testHandleMessage_broadcastsToWebSocket() {
        String topic = "sensors/temperature";
        String payload = "{\"sensorId\":\"sensor_1\",\"value\":25.0}";

        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();

        handler.handleMessage(message);

        // Capture arguments sent to broadcaster
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(broadcaster).broadcast(topicCaptor.capture(), payloadCaptor.capture());

        assertEquals(topic, topicCaptor.getValue());
        assertEquals(payload, payloadCaptor.getValue());
    }
}

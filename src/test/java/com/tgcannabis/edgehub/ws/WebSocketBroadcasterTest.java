package com.tgcannabis.edgehub.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

class WebSocketBroadcasterTest {
    private SimpMessagingTemplate template;
    private WebSocketBroadcaster broadcaster;

    @BeforeEach
    void setup() {
        template = mock(SimpMessagingTemplate.class);
        broadcaster = new WebSocketBroadcaster(template);
    }

    @Test
    void testBroadcast_sendsMessageToCorrectTopic() {
        String topic = "alerts";
        String payload = "{\"sensor\":\"temperature\",\"value\":35}";

        broadcaster.broadcast(topic, payload);

        verify(template, times(1)).convertAndSend("/topic/" + topic, payload);
    }
}

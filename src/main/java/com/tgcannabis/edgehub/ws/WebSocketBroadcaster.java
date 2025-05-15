package com.tgcannabis.edgehub.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketBroadcaster {
    private final SimpMessagingTemplate template;

    public WebSocketBroadcaster(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void broadcast(String topic, String payload) {
        System.out.println("Broadcasting message to channel " + topic);
        template.convertAndSend("/topic/" + topic, payload);
    }
}

# WebSocket Messaging with Spring Boot and STOMP

This application provides a WebSocket endpoint for real-time messaging using **STOMP** over **SockJS**, designed to work
with frontend clients such as **Angular**.

**STOMP is required to interact with the WebSocket message broker in a structured way.**

## MQTT Subscribing configuration

Inside this project's configuration `src/main/resources/application.yaml` you can find the configuration for accessing
MQTT broker:

```yaml
mqtt:
  broker: tcp://localhost:1883
  client-id: edge-hub-client
  topics: sensor-data/#,alerts
```

## 📡 WebSocket Endpoint

The WebSocket STOMP endpoint (when running locally) is exposed at:

```bash
http://localhost:8085/ws
```

You can change the server port in `src/main/resources/application.yaml`

This application allow connections just from specified hosts that can be configured in `config/WebSocketConfig.java`

```java

@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOrigins("http://localhost:4200") // CORS: allow app origin
            .withSockJS();                              // Fallback for browsers that don't support native WebSocket
}
```

## 📬 Subscribing to topics

This application exposes dynamic access to topics in the following broadcasting configuration found in `ws/WebSocketBroadcaster.java`

```java
public void broadcast(String topic, String payload) {
    System.out.println("Broadcasting message to channel " + topic);
    template.convertAndSend("/topic/" + topic, payload);
}
```

For example, for monitoring apps, the following topics are prepared:

```bash
/topic/sensor-data
/topic/alerts
```

## Socket connection for Frontends

The connection must use **SockJS** and **STOMP** protocol. This is typically done using libraries like:

- [`@stomp/stompjs`](https://www.npmjs.com/package/@stomp/stompjs) (Angular/React/JS clients)
- [`stompjs`](https://www.npmjs.com/package/stompjs) (legacy JS clients)
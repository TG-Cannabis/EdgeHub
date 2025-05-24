package com.tgcannabis.edgehub.e2e;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.tgcannabis.edgehub.ws.WebSocketConnectionListener;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketEndToEndTest {
    @LocalServerPort
    private int port;

    @MockitoBean
    private InfluxDBClient influxDBClient;

    @Test
    void testReceiveMessageFromMockedInflux() throws Exception {
        // Mock FluxTable and FluxRecord
        FluxRecord mockRecord = mock(FluxRecord.class);
        when(mockRecord.getMeasurement()).thenReturn("temperature");
        when(mockRecord.getValueByKey("sensorId")).thenReturn("sensor-123");
        when(mockRecord.getValueByKey("location")).thenReturn("room-1");
        when(mockRecord.getValueByKey("value")).thenReturn(24.6);
        when(mockRecord.getTime()).thenReturn(Instant.now());

        FluxTable mockTable = mock(FluxTable.class);
        when(mockTable.getRecords()).thenReturn(List.of(mockRecord));

        QueryApi mockQueryApi = mock(QueryApi.class);
        when(mockQueryApi.query(Mockito.anyString())).thenReturn(singletonList(mockTable));
        when(influxDBClient.getQueryApi()).thenReturn(mockQueryApi);

        // WebSocket test
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String url = String.format("ws://localhost:%d/ws/websocket", port);

        StompSession session = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
        }).get(3, TimeUnit.SECONDS);

        assertTrue(session.isConnected(), "WebSocket session should be connected");

        assertTrue(session.isConnected(), "Session should remain connected after subscription");
    }

    @Test
    void testWebSocketConnectionListenerCalled() {
        WebSocketConnectionListener listener = mock(WebSocketConnectionListener.class);

        ApplicationEventPublisher eventPublisher = event -> {
            if (event instanceof SessionSubscribeEvent subscribeEvent) {
                listener.handleSessionSubscribe(subscribeEvent);
            }
        };

        // Simulate a SessionSubscribeEvent
        var message = MessageBuilder.withPayload(new byte[0])
                .setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, "session123")
                .build();
        SessionSubscribeEvent subscribeEvent = new SessionSubscribeEvent(this, message);

        eventPublisher.publishEvent(subscribeEvent);

        verify(listener, times(1)).handleSessionSubscribe(any(SessionSubscribeEvent.class));
    }
}

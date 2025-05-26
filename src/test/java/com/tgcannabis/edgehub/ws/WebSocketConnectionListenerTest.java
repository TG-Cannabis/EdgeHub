package com.tgcannabis.edgehub.ws;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebSocketConnectionListenerTest {
    private SimpMessagingTemplate template;
    private QueryApi queryApi;
    private WebSocketConnectionListener listener;

    @BeforeEach
    void setup() throws Exception {
        template = mock(SimpMessagingTemplate.class);
        InfluxDBClient influxDBClient = mock(InfluxDBClient.class);
        queryApi = mock(QueryApi.class);

        when(influxDBClient.getQueryApi()).thenReturn(queryApi);

        listener = new WebSocketConnectionListener(template, influxDBClient);

        // Manually inject @Value fields
        setField(listener, "bucket", "sensor_data");
        setField(listener, "durationMinutes", 10);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testHandleSessionSubscribe_sendsWebSocketMessages() {
        // Mock data
        FluxRecord record = mock(FluxRecord.class);
        when(record.getValueByKey("sensorId")).thenReturn("sensor_1");
        when(record.getMeasurement()).thenReturn("temperature");
        when(record.getValueByKey("location")).thenReturn("growlab");
        when(record.getValueByKey("value")).thenReturn(24.5);
        when(record.getTime()).thenReturn(Instant.parse("2024-01-01T10:00:00Z"));

        FluxTable table = mock(FluxTable.class);
        when(table.getRecords()).thenReturn(List.of(record));

        when(queryApi.query(anyString())).thenReturn(List.of(table));

        // Execute
        listener.handleSessionSubscribe(mock(SessionSubscribeEvent.class));

        // Verify WebSocket message
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(template).convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());

        assertTrue(destinationCaptor.getValue().contains("/topic/sensors/data"));
        assertTrue(payloadCaptor.getValue().contains("\"sensorId\":\"sensor_1\""));
        assertTrue(payloadCaptor.getValue().contains("\"sensorType\":\"temperature\""));
        assertTrue(payloadCaptor.getValue().contains("\"location\":\"growlab\""));
        assertTrue(payloadCaptor.getValue().contains("\"value\":24.50"));
    }
}

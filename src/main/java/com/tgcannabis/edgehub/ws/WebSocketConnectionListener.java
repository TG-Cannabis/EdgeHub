package com.tgcannabis.edgehub.ws;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.List;
import java.util.Objects;

@Component
public class WebSocketConnectionListener {
    private final SimpMessagingTemplate template;
    private final InfluxDBClient influxDBClient;

    @Value("${influx.bucket}")
    private String bucket;

    @Value("${sensor.duration}")
    private int durationMinutes;

    public WebSocketConnectionListener(SimpMessagingTemplate template, InfluxDBClient influxDBClient) {
        this.template = template;
        this.influxDBClient = influxDBClient;
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        String newDestination = (String) event.getMessage().getHeaders().get("simpDestination");
        if (!"/topic/sensors/data".equals(newDestination))
            return;

        String destination = "/topic/sensors/data";
        List<FluxTable> tables = getFluxTables();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String sensorId = (String) record.getValueByKey("sensorId");
                String sensorType = record.getMeasurement();
                String location = (String) record.getValueByKey("location");
                Double value = record.getValueByKey("value") instanceof Number
                        ? ((Number) Objects.requireNonNull(record.getValueByKey("value"))).doubleValue()
                        : null;
                String timestamp = record.getTime() != null ? record.getTime().toString() : "";

                if (sensorId != null && sensorType != null && value != null) {
                    String json = String.format(
                            "{\"sensorId\":\"%s\",\"sensorType\":\"%s\",\"location\":\"%s\",\"value\":%.2f,\"timestamp\":\"%s\"}",
                            sensorId, sensorType, location, value, timestamp
                    );
                    template.convertAndSend(destination, json);
                }
            }
        }
    }

    @NotNull
    private List<FluxTable> getFluxTables() {
        QueryApi queryApi = influxDBClient.getQueryApi();

        String recentFlux = String.format("""
                    from(bucket: "%s")
                      |> range(start: -%dm)
                      |> filter(fn: (r) => r._measurement != "")
                      |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                      |> sort(columns: ["_time"])
                """, bucket, durationMinutes);

        List<FluxTable> recentTables = queryApi.query(recentFlux);

        int totalRecords = recentTables.stream()
                .mapToInt(t -> t.getRecords().size())
                .sum();

        if (totalRecords >= 50) {
            return recentTables;
        }

        String fullFlux = String.format("""
                    from(bucket: "%s")
                      |> range(start: 0)
                      |> filter(fn: (r) => r._measurement != "")
                      |> filter(fn: (r) => r.location == "greenhouse1")
                      |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                      |> sort(columns: ["_time"])
                """, bucket);

        return queryApi.query(fullFlux);
    }

}

package com.tgcannabis.edgehub.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxConfig {

    @Bean
    public InfluxDBClient influxDBClient() {
        String url = "http://localhost:8086";
        char[] token = "dc4d53ba57bb6bae9dcab955fb62204e794208fa15cb824877a99768085d6c7c".toCharArray();
        String org = "tgcannabis";
        String bucket = "sensor_data";

        return InfluxDBClientFactory.create(
                url,
                token,
                org,
                bucket
        );
    }

}

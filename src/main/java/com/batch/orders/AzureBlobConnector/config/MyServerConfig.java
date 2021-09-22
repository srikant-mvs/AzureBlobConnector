package com.batch.orders.AzureBlobConnector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration("myServerConfig")
@ConfigurationProperties("myserver")
public class MyServerConfig {

    private String healthCheckUrl;
    private String processOrderUrl;
}

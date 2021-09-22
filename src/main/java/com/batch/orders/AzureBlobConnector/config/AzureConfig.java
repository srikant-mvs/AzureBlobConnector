package com.batch.orders.AzureBlobConnector.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration("azureConfig")
@ConfigurationProperties("azurestorageblobservice")
public class AzureConfig {

  private Map<String, AzureStorageBlobServiceConfig> config = new HashMap<>();

  private String uploadErrorFileFromPath;
  private String accountName;
  private String accountKey;

}

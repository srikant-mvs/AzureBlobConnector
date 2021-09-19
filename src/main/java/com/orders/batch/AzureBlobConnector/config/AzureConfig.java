package com.orders.batch.AzureBlobConnector.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration("azureBlobServiceConfig")
@ConfigurationProperties("azurestorageservice")
public class AzureBlobServiceConfig {

  private Map<String, AzureStorageContainerConfig> config = new HashMap<>();
  private String uploadErrorFileFromPath;

}

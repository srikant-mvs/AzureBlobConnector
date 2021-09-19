package com.orders.batch.AzureBlobConnector.config;

import lombok.Data;

@Data
public class AzureStorageServiceConfig {

  private String routeId;
  private boolean enabled;
  private String timer;
  private String requestContainer;
  private String responseContainer;
  private String errorContainer;

}

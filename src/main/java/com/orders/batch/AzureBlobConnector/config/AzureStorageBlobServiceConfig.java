package com.orders.batch.AzureBlobConnector.config;

import lombok.Data;

@Data
public class AzureStorageBlobServiceConfig {

  private String routeId;
  private boolean enabled;
  private String timer;
  private String requestContainer;
  private String responseContainer;
  private String errorContainer;
  private String blobType;

}

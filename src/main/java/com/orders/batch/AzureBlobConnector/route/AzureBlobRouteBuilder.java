package com.orders.batch.AzureBlobConnector.route;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.orders.batch.AzureBlobConnector.config.AzureConfig;
import com.orders.batch.AzureBlobConnector.config.AzureStorageBlobServiceConfig;
import com.orders.batch.AzureBlobConnector.constant.AzureBlobConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@DependsOn("azureConfig")
public class AzureBlobRouteBuilder extends RouteBuilder {

  @Autowired
  private AzureConfig azureConfig;

  @Bean
  public BlobServiceClient blobServiceClient() {
    String azureStorageBlobUri = String.format("https://%s.blob.core.windows.net", azureConfig.getAccountName());
    StorageSharedKeyCredential credential = new StorageSharedKeyCredential(azureConfig.getAccountName(),
        azureConfig.getAccountKey());
    return new BlobServiceClientBuilder()
        .endpoint(azureStorageBlobUri)
        .credential(credential)
        .buildClient();
  }

  /**
   * <b>Called on initialization to build the routes using the fluent builder syntax.</b>
   * <p/>
   * This is a central method for RouteBuilder implementations to implement the routes using the
   * Java fluent builder syntax.
   *
   * @throws Exception can be thrown during configuration
   */
  @Override
  public void configure() throws Exception {
    log.info(azureConfig.getConfig().toString());
    azureConfig.getConfig().forEach((consumerName, azureStorageBlobServiceConfig) -> createRoute(azureStorageBlobServiceConfig) );
  }

  private void createRoute(AzureStorageBlobServiceConfig azureStorageBlobServiceConfig) {
    String camelUri = new StringBuilder().append("azure-storage-blob://")
        .append(azureConfig.getAccountName()).append("/")
        .append(azureStorageBlobServiceConfig.getRequestContainer())
        .append("?blobServiceClient=#blobServiceClient&operation=listBlobs").toString();

    from("timer://testRoute?fixedRate=true&period=10s")
        .routeId(azureStorageBlobServiceConfig.getRouteId())
        .to(camelUri)
        .setHeader(AzureBlobConstants.AZURE_STORAGE_BLOB_SERVICE_CONFIG, constant(azureStorageBlobServiceConfig))
        .process("xmlBlobProcessor");
  }
}

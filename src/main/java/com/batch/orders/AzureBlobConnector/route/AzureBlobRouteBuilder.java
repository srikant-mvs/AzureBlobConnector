package com.batch.orders.AzureBlobConnector.route;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.batch.orders.AzureBlobConnector.config.AzureConfig;
import com.batch.orders.AzureBlobConnector.config.AzureStorageBlobServiceConfig;
import com.batch.orders.AzureBlobConnector.config.MyServerConfig;
import com.batch.orders.AzureBlobConnector.constant.AzureBlobConstants;
import com.batch.orders.AzureBlobConnector.exception.AzureBlobConnectorException;
import com.batch.orders.AzureBlobConnector.exception.ServerUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.throttling.ThrottlingExceptionRoutePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
@Slf4j
@DependsOn("azureConfig")
@RequiredArgsConstructor
public class AzureBlobRouteBuilder extends RouteBuilder {

  private final AzureConfig azureConfig;
  private final ProducerTemplate producerTemplate;
  private final MyServerConfig myServerConfig;

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
    log.debug(azureConfig.getConfig().toString());
    azureConfig.getConfig().forEach((consumerName, azureStorageBlobServiceConfig) -> createRoute(azureStorageBlobServiceConfig) );
  }

  private void createRoute(AzureStorageBlobServiceConfig azureStorageBlobServiceConfig) {
    String camelUri = new StringBuilder().append("azure-storage-blob://")
        .append(azureConfig.getAccountName()).append("/")
        .append(azureStorageBlobServiceConfig.getRequestContainer())
        .append("?blobServiceClient=#blobServiceClient&operation=listBlobs").toString();

    from("timer://"+azureStorageBlobServiceConfig.getRouteId()+"?fixedRate=true&period=10s")
        .routeId(azureStorageBlobServiceConfig.getRouteId())
        .to(camelUri)
        .routePolicy(getThrotllingExceptionRoutePolicy())
        .setHeader(AzureBlobConstants.AZURE_STORAGE_BLOB_SERVICE_CONFIG, constant(azureStorageBlobServiceConfig))
        .process("azureBlobProcessor");

    from("timer://pollAzureBlobSCORoute?fixedRate=true&period=10s")
            .routeId("pollAzureBlobSCORoute")
            .to("azure-storage-blob://myStorageAccountName/myContainerName" +
                    "?blobServiceClient=#blobServiceClient&operation=listBlobs")
            .routePolicy(getThrotllingExceptionRoutePolicy())
            .process("azureBlobProcessor");
  }

  private ThrottlingExceptionRoutePolicy getThrotllingExceptionRoutePolicy() {
    List<Class<?>> throtledExceptions = new ArrayList<>();
    throtledExceptions.add(ServerUnavailableException.class);
    ThrottlingExceptionRoutePolicy routePolicy = new ThrottlingExceptionRoutePolicy(1,1000, 5000, throtledExceptions);
    routePolicy.setHalfOpenHandler(this::isMyWebServerAvailable);
    return routePolicy;
  }

  private boolean isMyWebServerAvailable() {
      Exchange exchange = producerTemplate.request(myServerConfig.getHealthCheckUrl(), new Processor() {
        public void process(Exchange exchange) throws Exception {
          String headers = exchange.getIn().getHeaders().toString();
          log.info("event=IsMyWebServer Healthy, {}", headers);
        }
      });
      log.info("exchange, {}", exchange);
    return true;
  }
}

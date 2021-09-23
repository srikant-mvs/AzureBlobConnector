package com.batch.orders.AzureBlobConnector.processor;


import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.batch.orders.AzureBlobConnector.config.AzureStorageBlobServiceConfig;
import com.batch.orders.AzureBlobConnector.constant.AzureBlobConstants;
import com.batch.orders.AzureBlobConnector.exception.ServerUnavailableException;
import com.batch.orders.AzureBlobConnector.service.AzureBlobService;
import com.batch.orders.AzureBlobConnector.utils.AzureBlobUtils;
import com.batch.orders.request.BatchOrderRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;

@Data
@Slf4j
@Service("azureBlobProcessor")
@RequiredArgsConstructor
public class AzureBlobProcessor implements Processor {

  private final BlobServiceClient blobServiceClient;
  private final AzureBlobUtils azureBlobUtils;
  private final BeanFactory beanFactory;
  private BlobProcessor blobProcessor;
  private final AzureBlobService azureBlobService;

  /**
   * Processes the message exchange
   *
   * @param exchange the message exchange
   * @throws Exception if an internal processing error has occurred.
   */
  @Override
  public void process(Exchange exchange) throws Exception {
    ArrayList<BlobItem> orderBatchBlobItems = exchange.getIn().getBody(ArrayList.class);
    for(BlobItem blobItem : orderBatchBlobItems){
      AzureStorageBlobServiceConfig serviceConfig = exchange.getIn().getHeader(AzureBlobConstants.AZURE_STORAGE_BLOB_SERVICE_CONFIG,
          AzureStorageBlobServiceConfig.class);

      getBeanProcessor(serviceConfig);

      BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(serviceConfig.getRequestContainer());
      BlobClient blobClient = blobContainerClient.getBlobClient(blobItem.getName());
      BlobLeaseClient blobLeaseClient = azureBlobUtils.getBlobAccessCondition(blobClient, blobItem.getProperties());

      if(blobLeaseClient!=null && StringUtils.isNotBlank(blobLeaseClient.getLeaseId())) {
        long currentTime = System.currentTimeMillis();
        log.info("event=BatchOrderProcessStarted, startTime={} ms", currentTime);
        processBatchBlobs(blobClient,blobLeaseClient, serviceConfig);
        log.info("event=BatchOrderProcessCompleted, timeSpent={} ms", System.currentTimeMillis()-currentTime);
      }

    }
  }

  private void processBatchBlobs(BlobClient blobClient, BlobLeaseClient blobLeaseClient, AzureStorageBlobServiceConfig serviceConfig)
          throws IOException, SAXException, XMLStreamException, ServerUnavailableException {
    BlobInputStream blobInputStream = blobClient.openInputStream();
    String blobContentAsString = blobProcessor.processContent(blobInputStream);

    if(blobProcessor.isBlobValid(blobClient, blobLeaseClient, blobInputStream, serviceConfig, blobServiceClient)) {
      BatchOrderRequest batchOrderRequest = blobProcessor.getBatchOrderRequest(blobContentAsString);
      BlobContainerClient responseBlobContainerClient = blobServiceClient.getBlobContainerClient(serviceConfig.getResponseContainer());
      generateBatchOrderResponse(batchOrderRequest, blobClient, responseBlobContainerClient);
      azureBlobUtils.deleteBlob(blobClient, blobLeaseClient);
    }
  }

  private void generateBatchOrderResponse(BatchOrderRequest batchOrderRequest, BlobClient blobClient, BlobContainerClient responseBlobContainerClient)
          throws XMLStreamException, IOException, ServerUnavailableException {
    String batchOrderResponseXml = azureBlobService.getBatchOrderResponse(batchOrderRequest);
    log.debug("batchOrderResponseXml={}", batchOrderResponseXml);
    azureBlobUtils.uploadBlob(batchOrderResponseXml, blobClient.getBlobName(), responseBlobContainerClient);
  }

  private void getBeanProcessor(AzureStorageBlobServiceConfig serviceConfig) {
    try {
      blobProcessor = (BlobProcessor) beanFactory.getBean(serviceConfig.getBlobType()+"BlobProcessor");
    } catch(NoSuchBeanDefinitionException e) {
      log.error("BlobProcessor file type is configured incorrectly", e);
    }
  }

}

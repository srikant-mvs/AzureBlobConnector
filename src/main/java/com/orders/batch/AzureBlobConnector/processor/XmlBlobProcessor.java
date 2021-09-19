package com.orders.batch.AzureBlobConnector.processor;


import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.orders.batch.AzureBlobConnector.config.AzureStorageBlobServiceConfig;
import com.orders.batch.AzureBlobConnector.constant.AzureBlobConstants;
import com.orders.batch.AzureBlobConnector.utils.AzureBlobUtils;
import java.util.ArrayList;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Data
@Slf4j
@Service("xmlBlobProcessor")
@RequiredArgsConstructor
public class XmlBlobProcessor implements Processor {

  private final BlobServiceClient blobServiceClient;
  private final AzureBlobUtils azureBlobUtils;

  /**
   * Processes the message exchange
   *
   * @param exchange the message exchange
   * @throws Exception if an internal processing error has occurred.
   */
  @Override
  public void process(Exchange exchange) throws Exception {
    log.info("Inside XML Blob Processor");
    ArrayList<BlobItem> orderBatchBlobItems = exchange.getIn().getBody(ArrayList.class);

    for(BlobItem blobItem : orderBatchBlobItems){
      AzureStorageBlobServiceConfig serviceConfig = exchange.getIn().getHeader(AzureBlobConstants.AZURE_STORAGE_BLOB_SERVICE_CONFIG,
          AzureStorageBlobServiceConfig.class);
      BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(serviceConfig.getRequestContainer());
      BlobClient blobClient = blobContainerClient.getBlobClient(blobItem.getName());

      BlobLeaseClient blobLeaseClient = azureBlobUtils.getBlobAccessCondition(blobClient, blobItem.getProperties());

      if(blobLeaseClient!=null && StringUtils.isNotBlank(blobLeaseClient.getLeaseId())) {
        processBatchOrderBlobs(blobClient,blobLeaseClient);
      }

    }
  }

  private void processBatchOrderBlobs(BlobClient blobClient, BlobLeaseClient blobLeaseClient) {

    BinaryData binaryData = blobClient.downloadContent();
    log.info("BlobName={}, with content={}", blobClient.getBlobName(), binaryData.toString());
    azureBlobUtils.deleteBlob(blobClient, blobLeaseClient);
  }
}

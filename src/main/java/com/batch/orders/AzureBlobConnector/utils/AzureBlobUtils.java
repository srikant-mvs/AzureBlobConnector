package com.batch.orders.AzureBlobConnector.utils;

import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.batch.orders.AzureBlobConnector.config.AzureStorageBlobServiceConfig;
import com.batch.orders.AzureBlobConnector.constant.AzureBlobConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service("azureBlobUtils")
@Slf4j
public class AzureBlobUtils {

  public BlobLeaseClient getBlobAccessCondition(BlobClient blobClient,
      BlobItemProperties properties) {
    BlobLeaseClient blobLeaseClient = new BlobLeaseClientBuilder()
        .blobClient(blobClient)
        .buildClient();

    if(isBlobLeased(properties)) {
      log.warn("event=BlobAlreadyLeased{}, blobName={}, leaseStatus={}",
          AzureBlobConstants.LEASE_ACTIVE_ON_BLOB, blobClient.getBlobName(),
          getBlobLeaseStatus(properties));
    } else {
      blobLeaseClient.acquireLease(-1);
      log.info("event=AcquireLeaseOnBlob, LeaseId={}, BlobName={}", blobLeaseClient.getLeaseId(), blobClient.getBlobName());
    }
    return blobLeaseClient;
  }

  private boolean isBlobLeased(BlobItemProperties properties) {
    return getBlobLeaseState(properties).equalsIgnoreCase(LeaseStateType.LEASED.toString()) &&
        getBlobLeaseStatus(properties).equalsIgnoreCase(LeaseStatusType.LOCKED.toString());
  }

  private String getBlobLeaseStatus(BlobItemProperties properties) {
    return properties.getLeaseStatus().name();
  }

  private String getBlobLeaseState(BlobItemProperties properties) {
    return properties.getLeaseState().name();
  }

  public void deleteBlob(BlobClient blobClient,BlobLeaseClient blobLeaseClient) {
    BlobRequestConditions requestConditions = new BlobRequestConditions();
    requestConditions.setLeaseId(blobLeaseClient.getLeaseId());
    try {
      Response<Void> response = blobClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE,
          requestConditions, null, Context.NONE);
      log.info("event=DeleteBlobSuccessful, ResponseCode={} ", blobClient.getBlobName(), response.getStatusCode());
    } catch(BlobStorageException e) {
      log.error("event=DeleteBlobFailed, StatusCode={}, Exception={}",
          e.getValue().toString(), e.getStatusCode());
    }
  }

  public void uploadBlob(String responsePayload, String blobName, BlobContainerClient responseBlobContainerClient) {
    BlobClient uploadBlobClient = responseBlobContainerClient.getBlobClient(blobName);
    InputStream inputStream = new ByteArrayInputStream(responsePayload.getBytes(StandardCharsets.UTF_8));
    BlobParallelUploadOptions blobParallelUploadOptions = new BlobParallelUploadOptions(inputStream);
    Response<BlockBlobItem> response = uploadBlobClient.uploadWithResponse(blobParallelUploadOptions, null, Context.NONE);
    log.info("event=UploadBlobSuccessful, BlobName={}, ResponseCode={}", blobName, response.getStatusCode());
  }

  public void moveToErrorContainer(BlobClient blobClient, BlobLeaseClient blobLeaseClient, BlobContainerClient errorBlobContainerClient) {
    BlobClient errorBlobClient = errorBlobContainerClient.getBlobClient(blobClient.getBlobName());
    BlobInputStream blobInputStream = blobClient.openInputStream();
    BlobParallelUploadOptions blobParallelUploadOptions = new BlobParallelUploadOptions(blobInputStream);
    Response<BlockBlobItem> response = errorBlobClient.uploadWithResponse(blobParallelUploadOptions, null, Context.NONE);
    log.info("event=UploadBlobToErrorContainer, BlobName={}, ResponseCode={}", blobClient.getBlobName(), response.getStatusCode());

    deleteBlob(blobClient, blobLeaseClient);
  }
}

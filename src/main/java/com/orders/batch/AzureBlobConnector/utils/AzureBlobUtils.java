package com.orders.batch.AzureBlobConnector.utils;

import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.LeaseStateType;
import com.azure.storage.blob.models.LeaseStatusType;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.orders.batch.AzureBlobConnector.constant.AzureBlobConstants;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("azureBlobUtils")
@Slf4j
public class AzureBlobUtils {

  public BlobLeaseClient getBlobAccessCondition(BlobClient blobClient,
      BlobItemProperties properties) {
    BlobLeaseClient blobLeaseClient = new BlobLeaseClientBuilder()
        .blobClient(blobClient)
        .buildClient();

    if(isBlobLeased(properties)) {
      log.warn("{}, blobName={}, leaseStatus={}",
          AzureBlobConstants.LEASE_ACTIVE_ON_BLOB, blobClient.getBlobName(),
          getBlobLeaseStatus(properties));
    } else {
      blobLeaseClient.acquireLease(-1);
      log.info("LeaseId={}, acquired on the blob={}", blobLeaseClient.getLeaseId(), blobClient.getBlobName());
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
          requestConditions, Duration.ofSeconds(1), Context.NONE);
      log.info("event=DeleteBlobSuccessful, ResponseCode={} ", blobClient.getBlobName(), response.getStatusCode());
    } catch(BlobStorageException e) {
      log.error("event=DeleteBlobFailed, StatusCode={}, Exception={}",
          e.getValue().toString(), e.getStatusCode());
    }
  }
}

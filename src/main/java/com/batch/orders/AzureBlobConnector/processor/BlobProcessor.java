package com.batch.orders.AzureBlobConnector.processor;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.batch.orders.AzureBlobConnector.config.AzureStorageBlobServiceConfig;
import com.batch.orders.AzureBlobConnector.utils.AzureBlobUtils;
import com.batch.orders.request.BatchOrderRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

@Slf4j
@Service("blobProcessor")
public abstract class BlobProcessor {

  @Value("${schema.path}")
  private String schemaPath;
  @Autowired
  private AzureBlobUtils azureBlobUtils;

  abstract String processContent(InputStream inputSteam) throws IOException;

  public boolean isBlobValid(BlobClient blobClient, BlobLeaseClient blobLeaseClient, BlobInputStream blobInputStream, AzureStorageBlobServiceConfig serviceConfig, BlobServiceClient blobServiceClient) throws IOException {
    boolean isBlobValid=false;
    try {
      isBlobValid = validateBatchOrderRequest(blobInputStream);
    } catch(SAXException e) {
      log.error("event=ValidationFailed, unable to read Blob={} Content, Exception={}", blobClient.getBlobName(), e);
      BlobContainerClient errorBlobContainerClient = blobServiceClient.getBlobContainerClient(serviceConfig.getErrorContainer());
      azureBlobUtils.moveToErrorContainer(blobClient,blobLeaseClient,errorBlobContainerClient);
    }
    return isBlobValid;
  }

  public BatchOrderRequest getBatchOrderRequest(String blobContentAsString) {
    BatchOrderRequest batchOrderRequest=null;
    JAXBContext jaxbContext = null;
    try {
      jaxbContext = JAXBContext.newInstance(BatchOrderRequest.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      batchOrderRequest = (BatchOrderRequest) jaxbUnmarshaller.unmarshal(new StringReader(blobContentAsString));
    } catch (JAXBException e) {
      e.printStackTrace();
    }
    return batchOrderRequest;
  }

  private boolean validateBatchOrderRequest(InputStream inputStream) throws SAXException, IOException {
    final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    final URL schemaUrl = Thread.currentThread().getContextClassLoader().getResource(schemaPath);
    final Schema schema = schemaFactory.newSchema(schemaUrl);
    final Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    validator.validate(new StreamSource(inputStream));
    return true;
  }
}

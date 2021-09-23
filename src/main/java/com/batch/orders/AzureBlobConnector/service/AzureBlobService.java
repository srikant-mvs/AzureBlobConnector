package com.batch.orders.AzureBlobConnector.service;

import com.batch.orders.AzureBlobConnector.config.MyServerConfig;
import com.batch.orders.AzureBlobConnector.exception.ServerUnavailableException;
import com.batch.orders.request.BatchOrderRequest;
import com.batch.orders.response.*;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringWriter;

@Slf4j
@Service("azureBlobService")
@RequiredArgsConstructor
public class AzureBlobService {

    private final ProducerTemplate producerTemplate;
    private final MyServerConfig myServerConfig;

    public String getBatchOrderResponse(BatchOrderRequest batchOrderRequest) throws XMLStreamException, IOException, ServerUnavailableException {

        String purchaseOrder = generatePurchaseOrder();

        BatchOrderResponse batchOrderResponse = new ObjectFactory().createBatchOrderResponse();
        batchOrderResponse.setResponseHeader(getResponseHeader(batchOrderRequest));
        batchOrderResponse.setResponseBody(getResponseBody(purchaseOrder));
        return writeResponseToXml(batchOrderResponse);
    }

    private String generatePurchaseOrder() throws ServerUnavailableException {
        String purchaseOrder = "";
        try{
            purchaseOrder = producerTemplate.requestBody(myServerConfig.getProcessOrderUrl(), "generatePurchaseOrder", String.class);
            log.info("event=GeneratePurchaseOrderNumber, {}", purchaseOrder);
        } catch(CamelExecutionException e) {
            throw new ServerUnavailableException("Exception={}", e);
        }
        return purchaseOrder;
    }

    private String writeResponseToXml(BatchOrderResponse batchOrderResponse) throws XMLStreamException, IOException {
        String responseXml;
        XmlMapper xmlMapper = createXMLMapper();
        StringWriter stringWriter = new StringWriter();
        XMLOutputFactory.newFactory().createXMLEventWriter(stringWriter);
        xmlMapper.writeValue(stringWriter, batchOrderResponse);
        responseXml = stringWriter.toString();
        return responseXml;
    }

    private XmlMapper createXMLMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        xmlMapper.setAnnotationIntrospector(new XmlAnnotationIntrospector.Pair(introspector, introspector));
        xmlMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);

        return xmlMapper;
    }

    private Header getResponseHeader(BatchOrderRequest batchOrderRequest) {
        Header responseHeader = new ObjectFactory().createHeader();
        responseHeader.setRequestId(batchOrderRequest.getHeader().getRequestId());
        responseHeader.setDateTime(batchOrderRequest.getHeader().getDateTime());
        responseHeader.setSource("ABC");
        return responseHeader;
    }

   private Body getResponseBody(String purchaseOrder) {
        Body responseBody = new ObjectFactory().createBody();
        Response response = new ObjectFactory().createResponse();
        response.setPurchaseOrder(purchaseOrder);

        responseBody.setResponse(response);
        return responseBody;
    }
}

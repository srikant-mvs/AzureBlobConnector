package com.batch.orders.AzureBlobConnector.processor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@Slf4j
@Service("xmlBlobProcessor")
public class XmlBlobProcessor extends BlobProcessor {

  String inputStreamToText;

  @Override
  public String processContent(InputStream inputSteam) throws IOException {
    inputStreamToText = new String(inputSteam.readAllBytes(), StandardCharsets.UTF_8);
    log.debug("inputSteam converted to text={}", inputStreamToText);
    return inputStreamToText;
  }
}

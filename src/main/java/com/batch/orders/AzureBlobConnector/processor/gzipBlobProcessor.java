package com.batch.orders.AzureBlobConnector.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service("gzipBlobProcessor")
public class gzipBlobProcessor extends BlobProcessor{
    String inputStreamToText;

    @Override
    public String processContent(InputStream inputSteam) throws IOException {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputSteam)) {
            inputStreamToText = new String(gzipInputStream.readAllBytes(), StandardCharsets.UTF_8);
            log.debug("gzipInputStream converted to text={}", inputStreamToText);
        }
        return inputStreamToText;
    }
}

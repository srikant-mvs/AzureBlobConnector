package com.batch.orders.AzureBlobConnector.exception;

public class AzureBlobConnectorException extends Exception {

    private static final long serialVersionUID=1L;

    public AzureBlobConnectorException(String message) {
        super(message);
    }

    public AzureBlobConnectorException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

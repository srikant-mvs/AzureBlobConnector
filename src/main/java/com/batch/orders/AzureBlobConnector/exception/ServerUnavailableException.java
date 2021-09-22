package com.batch.orders.AzureBlobConnector.exception;

public class ServerUnavailableException extends Exception {

    private static final long serialVersionUID=1L;

    public ServerUnavailableException(String message) {
        super(message);
    }

    public ServerUnavailableException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

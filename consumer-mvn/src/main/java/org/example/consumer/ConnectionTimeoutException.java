package org.example.consumer;

public class ConnectionTimeoutException extends Exception {

    ConnectionTimeoutException(String message) {
        super(message);
    }
    /**
     * Stackless exception
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}

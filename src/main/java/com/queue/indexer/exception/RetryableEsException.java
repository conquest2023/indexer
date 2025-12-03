package com.queue.indexer.exception;

public class RetryableEsException extends RuntimeException {

    public RetryableEsException() {
    }

    public RetryableEsException(String message) {
        super(message);
    }

    public RetryableEsException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableEsException(Throwable cause) {
        super(cause);
    }

    public RetryableEsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
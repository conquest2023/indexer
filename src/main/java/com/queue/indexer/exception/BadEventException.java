package com.queue.indexer.exception;

public class BadEventException extends RuntimeException {
    public BadEventException(String message) { super(message); }
    public BadEventException(String message, Throwable cause) { super(message, cause); }
}
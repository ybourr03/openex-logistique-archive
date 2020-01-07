package com.oxit.flow.logistique.ecomtoas400.exception;

/**
 * Exception thrown when the receiverID header is invalid.
 */
public class InvalidReceiverIdException extends RuntimeException {
    public InvalidReceiverIdException(String message) {
        super(message);
    }
}

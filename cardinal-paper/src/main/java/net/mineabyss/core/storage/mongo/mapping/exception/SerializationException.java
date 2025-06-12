package net.mineabyss.core.storage.mongo.mapping.exception;

public class SerializationException extends Exception {
    public SerializationException(String message) {
        super(message);
    }
    
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
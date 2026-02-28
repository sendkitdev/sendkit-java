package dev.sendkit;

public class SendKitException extends RuntimeException {

    private final String name;
    private final Integer statusCode;

    public SendKitException(String message, String name, Integer statusCode) {
        super(message);
        this.name = name;
        this.statusCode = statusCode;
    }

    public String getName() {
        return name;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}

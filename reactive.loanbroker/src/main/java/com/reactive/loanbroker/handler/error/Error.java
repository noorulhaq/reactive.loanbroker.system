package com.reactive.loanbroker.handler.error;

/**
 * Created by husainbasrawala on 1/9/17.
 */
class Error {

    private int code;
    private String message;

    private Error(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Error unknownError(Throwable ex) {
        return new Error(1000, "Unknown error occurred");
    }

    public static Error loanAmountRequired() {
        return new Error(1001, "loanAmount is required");
    }

    public static Error mapException(Exception ex) {
        return new Error(1002, ex.getMessage());
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

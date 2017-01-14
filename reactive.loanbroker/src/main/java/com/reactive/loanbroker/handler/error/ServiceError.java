package com.reactive.loanbroker.handler.error;

/**
 * Created by Noor on 1/9/17.
 */
public class ServiceError {

    private int code;
    private String message;

    private ServiceError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ServiceError unknownError() {
        return new ServiceError(1000, "Unknown error occurred");
    }

    public static ServiceError unknownError(Throwable ex) {
        return new ServiceError(1000, "Unknown error occurred");
    }

    public static ServiceError loanAmountRequired() {
        return new ServiceError(1001, "loanAmount is required");
    }

    public static ServiceError noBankServiceAvailable() {
        return new ServiceError(1003, "No bank service is available at the moment");
    }

    public static ServiceError mapException(Exception ex) {
        return new ServiceError(1002, ex.getMessage());
    }

    public static ServiceError serviceTookMoreTime() {
        return new ServiceError(1003, "Service request took more time. Please try again.");
    }


    public int getCode() {
        return code;
    }


    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceError that = (ServiceError) o;

        if (getCode() != that.getCode()) return false;
        return getMessage() != null ? getMessage().equals(that.getMessage()) : that.getMessage() == null;
    }

    @Override
    public int hashCode() {
        int result = getCode();
        result = 31 * result + (getMessage() != null ? getMessage().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ServiceError{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}

package uz.pdp.paymentservice.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) { super(message); }
}

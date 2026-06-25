package uz.pdp.paymentservice.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) { super(message); }
}

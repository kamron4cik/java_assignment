package uz.pdp.stationservice.exception;

public class StationNotFoundException extends RuntimeException {
    public StationNotFoundException(String message) { super(message); }
}

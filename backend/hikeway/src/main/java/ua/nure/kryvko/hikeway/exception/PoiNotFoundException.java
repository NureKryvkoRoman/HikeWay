package ua.nure.kryvko.hikeway.exception;

public class PoiNotFoundException extends HikewayException {
    public PoiNotFoundException() {
        super("Point of interest not found");
    }
}

package ua.nure.kryvko.hikeway.exception;

public class InvalidPoiDataException extends HikewayException {
    private final String code;

    public InvalidPoiDataException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

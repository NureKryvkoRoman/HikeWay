package ua.nure.kryvko.hikeway.exception;

public class PoiContentNotFoundException extends HikewayException {
    private final String resource;

    public PoiContentNotFoundException(String resource) {
        super(resource + " not found");
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}

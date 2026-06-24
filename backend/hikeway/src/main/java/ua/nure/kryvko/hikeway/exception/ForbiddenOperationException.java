package ua.nure.kryvko.hikeway.exception;

public class ForbiddenOperationException extends HikewayException {
    public ForbiddenOperationException() {
        super("You do not own this resource");
    }
}

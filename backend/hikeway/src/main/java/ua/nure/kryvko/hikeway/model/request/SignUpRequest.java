package ua.nure.kryvko.hikeway.model.request;

public record SignUpRequest(String email, String password, String firstName, String lastName, String username) {
}

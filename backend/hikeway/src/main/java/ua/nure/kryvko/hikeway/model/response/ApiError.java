package ua.nure.kryvko.hikeway.model.response;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, Object> details
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of());
    }
}

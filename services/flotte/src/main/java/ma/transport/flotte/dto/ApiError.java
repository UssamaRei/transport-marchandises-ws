package ma.transport.flotte.dto;

import java.util.Map;

public record ApiError(String code, String message, Map<String, Object> details, String correlationId) {
}

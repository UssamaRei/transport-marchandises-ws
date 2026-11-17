package ma.transport.flotte.dto;

import java.util.List;

public record CamionsPageResponse(
        List<CamionResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

package ma.transport.flotte.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record CamionPatchRequest(JsonNode immatriculation, JsonNode capaciteKg) {
}

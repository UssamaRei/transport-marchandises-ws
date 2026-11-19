package ma.transport.bons.dto;

import java.time.LocalDate;

public record PreuveLivraisonResponse(String bonId, String transportId, String receptionnaire,
                                      LocalDate dateReception, String commentaire, String statut) { }

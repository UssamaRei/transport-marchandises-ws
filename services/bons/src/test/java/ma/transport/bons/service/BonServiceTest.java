package ma.transport.bons.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import ma.transport.bons.client.ChargementsClient;
import ma.transport.bons.dto.ChargementDetails;
import ma.transport.bons.dto.CreerBonRequest;
import ma.transport.bons.dto.PreuveLivraisonRequest;
import ma.transport.bons.exception.BonException;
import ma.transport.bons.model.BonLivraison;
import ma.transport.bons.model.LigneBon;
import ma.transport.bons.model.StatutBon;
import ma.transport.bons.repository.BonLivraisonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BonServiceTest {
    @Mock BonLivraisonRepository repository;
    @Mock ChargementsClient chargementsClient;
    private BonService service;

    @BeforeEach
    void setUp() { service = new BonService(repository, chargementsClient); }

    @Test
    void listeLesBonsSansChargerLesLignes() {
        BonLivraison bon = bonEmis("bon-1");
        when(repository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(bon)));
        var page = service.lister(0, 20, null);
        assertThat(page.getContent()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo("bon-1"));
        verify(chargementsClient, never()).getChargement(any());
    }

    @Test
    void refuseUnePaginationHorsLimites() {
        assertThatThrownBy(() -> service.lister(0, 101, null)).isInstanceOf(BonException.class)
                .satisfies(error -> assertThat(((BonException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void emetUnBonAvecSnapshotExactDuChargementValide() {
        when(repository.findByChargementId("load-1")).thenReturn(Optional.empty());
        when(chargementsClient.getChargement("load-1")).thenReturn(chargement("VALIDE"));
        when(repository.saveAndFlush(any(BonLivraison.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findDetailById(any())).thenAnswer(inv -> {
            ArgumentCaptor<BonLivraison> captor = ArgumentCaptor.forClass(BonLivraison.class);
            verify(repository).saveAndFlush(captor.capture());
            return Optional.of(captor.getValue());
        });

        BonLivraison bon = service.creer(new CreerBonRequest("load-1"));

        assertThat(bon.getNumero()).startsWith("BL-");
        assertThat(bon.getStatut()).isEqualTo(StatutBon.EMIS);
        assertThat(bon.getChargementId()).isEqualTo("load-1");
        assertThat(bon.getCamionSouhaiteId()).isEqualTo("truck-1");
        assertThat(bon.getOrigine()).isEqualTo("Casablanca");
        assertThat(bon.getDestination()).isEqualTo("Rabat");
        assertThat(bon.getDestinataireNom()).isEqualTo("Client Test");
        assertThat(bon.getDestinataireTelephone()).isEqualTo("0600000000");
        assertThat(bon.getPoidsTotalKg()).isEqualByComparingTo("25.000");
        assertThat(bon.getLignes()).singleElement().satisfies(line -> {
            assertThat(line.getDesignation()).isEqualTo("Cartons");
            assertThat(line.getQuantite()).isEqualTo(2);
            assertThat(line.getPoidsUnitaireKg()).isEqualByComparingTo("12.500");
            assertThat(line.getId()).isNotBlank();
        });
    }

    @Test
    void refuseUnChargementNonValideSansEcrireDeBon() {
        when(repository.findByChargementId("load-1")).thenReturn(Optional.empty());
        when(chargementsClient.getChargement("load-1")).thenReturn(chargement("BROUILLON"));
        assertThatThrownBy(() -> service.creer(new CreerBonRequest("load-1")))
                .isInstanceOf(BonException.class)
                .satisfies(error -> assertThat(((BonException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void refuseUnSecondBonPourLeMemeChargement() {
        when(repository.findByChargementId("load-1")).thenReturn(Optional.of(new BonLivraison()));
        assertThatThrownBy(() -> service.creer(new CreerBonRequest("load-1")))
                .isInstanceOf(BonException.class)
                .satisfies(error -> assertThat(((BonException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(chargementsClient, never()).getChargement(any());
    }

    @Test
    void enregistrePreuveEnUtcEtRejoueLaMemeDemandeSansDoublon() {
        BonLivraison bon = bonEmis("bon-1");
        when(repository.findByIdForUpdate("bon-1")).thenReturn(Optional.of(bon));
        PreuveLivraisonRequest request = preuve("transport-1", "livraison:transport-1", "Reçu complet");

        var first = service.confirmer("bon-1", request);
        var replay = service.confirmer("bon-1", request);

        assertThat(first).isEqualTo(replay);
        assertThat(first.statut()).isEqualTo("LIVRE");
        assertThat(first.dateReception()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(bon.getLivreLe()).isEqualTo(OffsetDateTime.of(2026, 11, 18, 0, 0, 0, 0, ZoneOffset.UTC));
        assertThat(bon.getTransportPreuveId()).isEqualTo("transport-1");
        verify(repository).flush();
    }

    @Test
    void refusePreuveDifferenteApresLivraison() {
        BonLivraison bon = bonEmis("bon-1");
        bon.setStatut(StatutBon.LIVRE);
        bon.setTransportPreuveId("transport-1");
        bon.setRecepteur("Mohamed");
        bon.setLivreLe(OffsetDateTime.of(2026, 11, 18, 0, 0, 0, 0, ZoneOffset.UTC));
        bon.setCommentaire("Original");
        when(repository.findByIdForUpdate("bon-1")).thenReturn(Optional.of(bon));

        assertThatThrownBy(() -> service.confirmer("bon-1", preuve("transport-1", "livraison:transport-1", "Modifié")))
                .isInstanceOf(BonException.class)
                .satisfies(error -> assertThat(((BonException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).flush();
    }

    @Test
    void refuseCleOperationnelleIncoherente() {
        assertThatThrownBy(() -> service.confirmer("bon-1", preuve("transport-1", "livraison:autre", null)))
                .isInstanceOf(BonException.class)
                .satisfies(error -> assertThat(((BonException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repository, never()).findByIdForUpdate(any());
    }

    @Test
    void renvoie404PourBonAbsent() {
        when(repository.findByIdForUpdate("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmer("missing", preuve("transport-1", "livraison:transport-1", null)))
                .isInstanceOf(BonException.class)
                .satisfies(error -> assertThat(((BonException) error).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private ChargementDetails chargement(String statut) {
        return new ChargementDetails("load-1", "Casablanca", "Rabat", "Client Test", "0600000000",
                "truck-1", new BigDecimal("25.000"), statut,
                List.of(new ChargementDetails.LigneChargementDetails("line-1", "Cartons", 2, new BigDecimal("12.500"))));
    }

    private BonLivraison bonEmis(String id) {
        BonLivraison bon = new BonLivraison();
        bon.setId(id);
        bon.setStatut(StatutBon.EMIS);
        return bon;
    }

    private PreuveLivraisonRequest preuve(String transport, String operationKey, String comment) {
        return new PreuveLivraisonRequest(transport, "Mohamed", LocalDate.of(2026, 11, 18), comment, operationKey);
    }
}

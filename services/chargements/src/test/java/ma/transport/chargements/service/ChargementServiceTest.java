package ma.transport.chargements.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import ma.transport.chargements.dto.LigneChargementRequest;
import ma.transport.chargements.exception.ChargementException;
import ma.transport.chargements.model.Camion;
import ma.transport.chargements.model.Chargement;
import ma.transport.chargements.model.LigneChargement;
import ma.transport.chargements.model.StatutChargement;
import ma.transport.chargements.proxy.CamionsProxy;
import ma.transport.chargements.repository.ChargementRepository;
import ma.transport.chargements.repository.LigneChargementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargementServiceTest {

    @Mock
    private ChargementRepository chargementRepository;
    @Mock
    private LigneChargementRepository ligneRepository;
    @Mock
    private CamionsProxy camionsProxy;
    @InjectMocks
    private ChargementService service;

    private Chargement chargement;

    @BeforeEach
    void setUp() {
        chargement = new Chargement();
        chargement.setId("chargement-1");
        chargement.setOrigine("Agadir");
        chargement.setDestination("Casablanca");
        chargement.setDestinataireNom("Client A");
        chargement.setStatut(StatutChargement.BROUILLON);
        chargement.setPoidsTotalKg(BigDecimal.ZERO);
    }

    @Test
    void ajoutLigneCalculeLePoidsTotal() {
        when(chargementRepository.findByIdForUpdate("chargement-1"))
                .thenReturn(Optional.of(chargement));
        when(chargementRepository.findById("chargement-1"))
                .thenReturn(Optional.of(chargement));
        when(ligneRepository.findByChargementId("chargement-1"))
                .thenReturn(List.of(ligne(2, "10"), ligne(3, "2.5")));
        LigneChargementRequest request = new LigneChargementRequest();
        request.setDesignation("Cartons");
        request.setQuantite(3);
        request.setPoidsUnitaireKg(new BigDecimal("2.5"));

        Chargement resultat = service.addLigne("chargement-1", request);

        assertEquals(0, new BigDecimal("27.5").compareTo(resultat.getPoidsTotalKg()));
        assertEquals(StatutChargement.BROUILLON, resultat.getStatut());
        ArgumentCaptor<LigneChargement> capture = ArgumentCaptor.forClass(LigneChargement.class);
        verify(ligneRepository).save(org.mockito.ArgumentMatchers.eq("chargement-1"), capture.capture());
        assertEquals("Cartons", capture.getValue().getDesignation());
        assertEquals(3, capture.getValue().getQuantite());
        assertEquals(new BigDecimal("2.5"), capture.getValue().getPoidsUnitaireKg());
        assertTrue(capture.getValue().getId() != null && !capture.getValue().getId().isBlank());
        verify(chargementRepository).update(chargement);
    }

    @Test
    void validationAccepteUnPoidsEgalALaCapacite() {
        when(chargementRepository.findByIdForUpdate("chargement-1"))
                .thenReturn(Optional.of(chargement));
        when(chargementRepository.findById("chargement-1"))
                .thenReturn(Optional.of(chargement));
        when(ligneRepository.findByChargementId("chargement-1"))
                .thenReturn(List.of(ligne(10, "10")));
        when(camionsProxy.findCamionById("camion-1")).thenReturn(camion("100"));

        Chargement resultat = service.validate("chargement-1", "camion-1");

        assertEquals(StatutChargement.VALIDE, resultat.getStatut());
        assertEquals("camion-1", resultat.getCamionSouhaiteId());
        assertEquals(0, new BigDecimal("100").compareTo(resultat.getPoidsTotalKg()));
        verify(camionsProxy).findCamionById("camion-1");
        verify(chargementRepository).update(chargement);
    }

    @Test
    void validationRefuseUnPoidsSuperieurALaCapacite() {
        when(chargementRepository.findByIdForUpdate("chargement-1"))
                .thenReturn(Optional.of(chargement));
        when(ligneRepository.findByChargementId("chargement-1"))
                .thenReturn(List.of(ligne(10, "10")));
        when(camionsProxy.findCamionById("camion-1")).thenReturn(camion("99.999"));

        ChargementException erreur = assertThrows(ChargementException.class,
                () -> service.validate("chargement-1", "camion-1"));

        assertEquals(HttpStatus.CONFLICT, erreur.getStatus());
        assertEquals(StatutChargement.BROUILLON, chargement.getStatut());
        verify(chargementRepository, never()).update(any(Chargement.class));
    }

    @Test
    void suppressionRefuseUnChargementValide() {
        chargement.setStatut(StatutChargement.VALIDE);
        when(chargementRepository.findByIdForUpdate("chargement-1"))
                .thenReturn(Optional.of(chargement));

        ChargementException erreur = assertThrows(ChargementException.class,
                () -> service.delete("chargement-1"));

        assertEquals(HttpStatus.CONFLICT, erreur.getStatus());
        verify(chargementRepository, never()).deleteById("chargement-1");
    }

    private LigneChargement ligne(int quantite, String poidsUnitaireKg) {
        LigneChargement ligne = new LigneChargement();
        ligne.setQuantite(quantite);
        ligne.setPoidsUnitaireKg(new BigDecimal(poidsUnitaireKg));
        return ligne;
    }

    private Camion camion(String capaciteKg) {
        Camion camion = new Camion();
        camion.setId("camion-1");
        camion.setCapaciteKg(new BigDecimal(capaciteKg));
        camion.setEtat("EN_SERVICE");
        camion.setArchive(false);
        camion.setDisponible(true);
        return camion;
    }
}

package ma.transport.chargements.service;

import feign.FeignException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.transport.chargements.dto.ChargementRequest;
import ma.transport.chargements.dto.ChargementResume;
import ma.transport.chargements.dto.LigneChargementRequest;
import ma.transport.chargements.exception.ChargementException;
import ma.transport.chargements.model.Camion;
import ma.transport.chargements.model.Chargement;
import ma.transport.chargements.model.LigneChargement;
import ma.transport.chargements.model.StatutChargement;
import ma.transport.chargements.proxy.CamionsProxy;
import ma.transport.chargements.repository.ChargementRepository;
import ma.transport.chargements.repository.LigneChargementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
public class ChargementService {

    private static final BigDecimal POIDS_MAXIMUM = new BigDecimal("99999999999.999");

    private final ChargementRepository chargementRepository;
    private final LigneChargementRepository ligneRepository;
    private final CamionsProxy camionsProxy;

    public ChargementService(ChargementRepository chargementRepository,
                             LigneChargementRepository ligneRepository,
                             CamionsProxy camionsProxy) {
        this.chargementRepository = chargementRepository;
        this.ligneRepository = ligneRepository;
        this.camionsProxy = camionsProxy;
    }

    @Transactional
    public Chargement create(ChargementRequest request) {
        Chargement chargement = new Chargement();
        chargement.setId(UUID.randomUUID().toString());
        copyInformations(request, chargement);
        chargement.setPoidsTotalKg(BigDecimal.ZERO);
        chargement.setStatut(StatutChargement.BROUILLON);
        chargementRepository.save(chargement);
        return findDetail(chargement.getId());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Chargement findById(String id) {
        return findDetail(id);
    }

    @Transactional(readOnly = true)
    public List<ChargementResume> findAll(int limit, int offset) {
        List<ChargementResume> result = new ArrayList<>();
        for (Chargement chargement : chargementRepository.findAll(limit, offset)) {
            ChargementResume resume = new ChargementResume();
            resume.setId(chargement.getId());
            resume.setOrigine(chargement.getOrigine());
            resume.setDestination(chargement.getDestination());
            resume.setPoidsTotalKg(chargement.getPoidsTotalKg());
            resume.setStatut(chargement.getStatut());
            result.add(resume);
        }
        return result;
    }

    @Transactional
    public Chargement update(String id, ChargementRequest request) {
        Chargement chargement = lockBrouillon(id);
        copyInformations(request, chargement);
        chargementRepository.update(chargement);
        return findDetail(id);
    }

    @Transactional
    public void delete(String id) {
        lockBrouillon(id);
        chargementRepository.deleteById(id);
    }

    @Transactional
    public Chargement addLigne(String id, LigneChargementRequest request) {
        Chargement chargement = lockBrouillon(id);
        LigneChargement ligne = toLigne(UUID.randomUUID().toString(), request);
        ligneRepository.save(id, ligne);
        updatePoids(chargement);
        return findDetail(id);
    }

    @Transactional
    public Chargement updateLigne(String id, String ligneId, LigneChargementRequest request) {
        Chargement chargement = lockBrouillon(id);
        int updated = ligneRepository.update(id, toLigne(ligneId, request));
        if (updated == 0) {
            throw new ChargementException(HttpStatus.NOT_FOUND, "Ligne introuvable dans ce chargement.");
        }
        updatePoids(chargement);
        return findDetail(id);
    }

    @Transactional
    public Chargement deleteLigne(String id, String ligneId) {
        Chargement chargement = lockBrouillon(id);
        if (ligneRepository.delete(id, ligneId) == 0) {
            throw new ChargementException(HttpStatus.NOT_FOUND, "Ligne introuvable dans ce chargement.");
        }
        updatePoids(chargement);
        return findDetail(id);
    }

    @Transactional
    public Chargement validate(String id, String camionId) {
        Chargement chargement = lockChargement(id);
        if (chargement.getStatut() == StatutChargement.VALIDE) {
            if (camionId.equals(chargement.getCamionSouhaiteId())) {
                return findDetail(id);
            }
            throw new ChargementException(HttpStatus.CONFLICT, "Ce chargement est deja valide avec un autre camion.");
        }

        if (isBlank(chargement.getOrigine()) || isBlank(chargement.getDestination())
                || isBlank(chargement.getDestinataireNom())) {
            throw new ChargementException(HttpStatus.BAD_REQUEST,
                    "Origine, destination et destinataire sont obligatoires pour valider.");
        }
        List<LigneChargement> lignes = ligneRepository.findByChargementId(id);
        if (lignes.isEmpty()) {
            throw new ChargementException(HttpStatus.BAD_REQUEST, "Ajouter au moins une ligne avant de valider.");
        }
        BigDecimal poids = calculatePoids(lignes);
        Camion camion = findCamion(camionId);
        checkCamion(camionId, camion, poids);

        chargement.setCamionSouhaiteId(camionId);
        chargement.setPoidsTotalKg(poids);
        chargement.setStatut(StatutChargement.VALIDE);
        chargementRepository.update(chargement);
        return findDetail(id);
    }

    private Chargement findDetail(String id) {
        Chargement chargement = chargementRepository.findById(id).orElseThrow(() ->
                new ChargementException(HttpStatus.NOT_FOUND, "Chargement introuvable."));
        chargement.setLignes(ligneRepository.findByChargementId(id));
        return chargement;
    }

    private Chargement lockChargement(String id) {
        return chargementRepository.findByIdForUpdate(id).orElseThrow(() ->
                new ChargementException(HttpStatus.NOT_FOUND, "Chargement introuvable."));
    }

    private Chargement lockBrouillon(String id) {
        // Toutes les modifications verrouillent le parent avant de toucher aux lignes.
        Chargement chargement = lockChargement(id);
        if (chargement.getStatut() != StatutChargement.BROUILLON) {
            throw new ChargementException(HttpStatus.CONFLICT, "Un chargement valide ne peut plus etre modifie.");
        }
        return chargement;
    }

    private void updatePoids(Chargement chargement) {
        List<LigneChargement> lignes = ligneRepository.findByChargementId(chargement.getId());
        chargement.setPoidsTotalKg(calculatePoids(lignes));
        chargementRepository.update(chargement);
    }

    private BigDecimal calculatePoids(List<LigneChargement> lignes) {
        BigDecimal total = BigDecimal.ZERO;
        for (LigneChargement ligne : lignes) {
            BigDecimal poidsLigne = ligne.getPoidsUnitaireKg().multiply(BigDecimal.valueOf(ligne.getQuantite()));
            total = total.add(poidsLigne);
        }
        if (total.compareTo(POIDS_MAXIMUM) > 0) {
            throw new ChargementException(HttpStatus.BAD_REQUEST, "Le poids total depasse la limite de stockage.");
        }
        return total;
    }

    private Camion findCamion(String camionId) {
        try {
            return camionsProxy.findCamionById(camionId);
        } catch (FeignException.NotFound exception) {
            throw new ChargementException(HttpStatus.BAD_REQUEST, "Le camion demande n'existe pas.");
        } catch (FeignException exception) {
            throw new ChargementException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service Camions est indisponible. Reessayer plus tard.");
        }
    }

    private void checkCamion(String camionId, Camion camion, BigDecimal poids) {
        if (camion == null || !camionId.equals(camion.getId()) || camion.getCapaciteKg() == null
                || camion.getCapaciteKg().signum() <= 0 || camion.getEtat() == null
                || camion.getArchive() == null || camion.getDisponible() == null) {
            throw new ChargementException(HttpStatus.BAD_GATEWAY, "La reponse du service Camions est incomplete ou invalide.");
        }
        if (!"EN_SERVICE".equals(camion.getEtat()) || camion.getArchive() || !camion.getDisponible()) {
            throw new ChargementException(HttpStatus.CONFLICT, "Le camion n'est pas disponible ou n'est pas en service.");
        }
        if (poids.compareTo(camion.getCapaciteKg()) > 0) {
            throw new ChargementException(HttpStatus.CONFLICT, "La capacite du camion est insuffisante.");
        }
    }

    private LigneChargement toLigne(String id, LigneChargementRequest request) {
        LigneChargement ligne = new LigneChargement();
        ligne.setId(id);
        ligne.setDesignation(request.getDesignation().trim());
        ligne.setQuantite(request.getQuantite());
        ligne.setPoidsUnitaireKg(request.getPoidsUnitaireKg());
        return ligne;
    }

    private void copyInformations(ChargementRequest request, Chargement chargement) {
        chargement.setOrigine(trim(request.getOrigine()));
        chargement.setDestination(trim(request.getDestination()));
        chargement.setDestinataireNom(trim(request.getDestinataireNom()));
        chargement.setDestinataireTelephone(trim(request.getDestinataireTelephone()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

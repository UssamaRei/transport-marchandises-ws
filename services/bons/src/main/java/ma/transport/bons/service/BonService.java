package ma.transport.bons.service;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.UUID;
import ma.transport.bons.client.ChargementsClient;
import ma.transport.bons.dto.ChargementDetails;
import ma.transport.bons.dto.PreuveLivraisonRequest;
import ma.transport.bons.dto.PreuveLivraisonResponse;
import ma.transport.bons.dto.CreerBonRequest;
import ma.transport.bons.dto.BonResumeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ma.transport.bons.exception.BonException;
import ma.transport.bons.model.BonLivraison;
import ma.transport.bons.model.LigneBon;
import ma.transport.bons.model.StatutBon;
import ma.transport.bons.repository.BonLivraisonRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BonService {
    private final BonLivraisonRepository repository;
    private final ChargementsClient chargementsClient;
    public BonService(BonLivraisonRepository repository, ChargementsClient chargementsClient) {
        this.repository = repository;
        this.chargementsClient = chargementsClient;
    }

    @Transactional
    public BonLivraison creer(CreerBonRequest request) {
        if (repository.findByChargementId(request.chargementId()).isPresent()) {
            throw new BonException(HttpStatus.CONFLICT, "Un bon existe déjà pour ce chargement.");
        }
        ChargementDetails source = chargementsClient.getChargement(request.chargementId());
        validerChargement(source, request.chargementId());
        BonLivraison bon = new BonLivraison();
        bon.setId(UUID.randomUUID().toString());
        bon.setNumero("BL-" + UUID.randomUUID());
        bon.setChargementId(source.id());
        bon.setCamionSouhaiteId(source.camionSouhaiteId());
        bon.setOrigine(source.origine());
        bon.setDestination(source.destination());
        bon.setDestinataireNom(source.destinataireNom());
        bon.setDestinataireTelephone(source.destinataireTelephone());
        bon.setPoidsTotalKg(source.poidsTotalKg());
        source.lignes().forEach(item -> {
            LigneBon ligne = new LigneBon();
            ligne.setId(UUID.randomUUID().toString());
            ligne.setDesignation(item.designation());
            ligne.setQuantite(item.quantite());
            ligne.setPoidsUnitaireKg(item.poidsUnitaireKg());
            bon.addLigne(ligne);
        });
        try {
            repository.saveAndFlush(bon);
            return repository.findDetailById(bon.getId()).orElseThrow();
        } catch (DataIntegrityViolationException error) {
            throw new BonException(HttpStatus.CONFLICT, "Un bon existe déjà pour ce chargement.");
        }
    }

    @Transactional(readOnly = true)
    public Page<BonResumeResponse> lister(int page, int size, StatutBon statut) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BonException(HttpStatus.BAD_REQUEST, "Page invalide ou taille hors limites : 1 à 100.");
        }
        PageRequest pagination = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<BonLivraison> bons = statut == null ? repository.findAll(pagination) : repository.findByStatut(statut, pagination);
        return bons.map(BonResumeResponse::from);
    }

    @Transactional(readOnly = true)
    public BonLivraison obtenir(String id) {
        return repository.findDetailById(id).orElseThrow(() -> new BonException(HttpStatus.NOT_FOUND, "Bon introuvable."));
    }

    @Transactional
    public PreuveLivraisonResponse confirmer(String id, PreuveLivraisonRequest request) {
        validerPreuve(id, request);
        BonLivraison bon = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BonException(HttpStatus.NOT_FOUND, "Bon introuvable."));
        if (bon.getStatut() == StatutBon.LIVRE) {
            if (memePreuve(bon, request)) return response(bon);
            throw new BonException(HttpStatus.CONFLICT, "Une preuve différente est déjà enregistrée pour ce bon.");
        }
        bon.setTransportPreuveId(request.transportId());
        bon.setRecepteur(request.receptionnaire());
        bon.setLivreLe(request.dateReception().atStartOfDay().atOffset(ZoneOffset.UTC));
        bon.setCommentaire(request.commentaire());
        bon.setStatut(StatutBon.LIVRE);
        try {
            repository.flush();
        } catch (DataIntegrityViolationException error) {
            throw new BonException(HttpStatus.CONFLICT, "Le transport ne correspond pas à ce bon ou possède déjà une preuve.");
        }
        return response(bon);
    }

    private void validerChargement(ChargementDetails source, String requestedId) {
        if (source == null) throw new BonException(HttpStatus.BAD_GATEWAY, "Réponse vide du service Chargements.");
        if (!requestedId.equals(source.id())) throw new BonException(HttpStatus.BAD_GATEWAY, "Identifiant incohérent dans la réponse Chargements.");
        if (!"VALIDE".equals(source.statut())) throw new BonException(HttpStatus.CONFLICT, "Seul un chargement VALIDE peut produire un bon.");
        if (blank(source.origine()) || blank(source.destination()) || blank(source.destinataireNom())
                || blank(source.camionSouhaiteId()) || source.poidsTotalKg() == null || source.poidsTotalKg().signum() <= 0
                || source.poidsTotalKg().compareTo(new BigDecimal("99999999999.999")) > 0
                || source.lignes() == null || source.lignes().isEmpty()) {
            throw new BonException(HttpStatus.BAD_GATEWAY, "Le chargement validé retourné est incomplet.");
        }
        if (source.origine().length() > 500 || source.destination().length() > 500
                || source.destinataireNom().length() > 160
                || (source.destinataireTelephone() != null && source.destinataireTelephone().length() > 40)) {
            throw new BonException(HttpStatus.BAD_GATEWAY, "Le chargement contient des champs hors limites.");
        }
        for (ChargementDetails.LigneChargementDetails ligne : source.lignes()) {
            if (ligne == null || blank(ligne.designation()) || ligne.designation().length() > 240 || ligne.quantite() <= 0
                    || ligne.poidsUnitaireKg() == null || ligne.poidsUnitaireKg().signum() <= 0) {
                throw new BonException(HttpStatus.BAD_GATEWAY, "Une ligne du chargement est invalide.");
            }
        }
    }

    private void validerPreuve(String id, PreuveLivraisonRequest demande) {
        if (id == null || id.isBlank() || id.length() > 64 || demande == null || demande.transportId() == null
                || demande.transportId().isBlank() || demande.transportId().length() > 64
                || demande.receptionnaire() == null || demande.receptionnaire().isBlank() || demande.receptionnaire().length() > 160
                || demande.dateReception() == null || (demande.commentaire() != null && demande.commentaire().length() > 1000)
                || demande.operationKey() == null || demande.operationKey().length() > 160
                || !("livraison:" + demande.transportId()).equals(demande.operationKey())) {
            throw new BonException(HttpStatus.BAD_REQUEST, "Données de preuve de livraison invalides.");
        }
    }

    private boolean memePreuve(BonLivraison bon, PreuveLivraisonRequest demande) {
        return demande.transportId().equals(bon.getTransportPreuveId())
                && demande.receptionnaire().equals(bon.getRecepteur())
                && demande.dateReception().equals(bon.getLivreLe().toInstant().atZone(ZoneOffset.UTC).toLocalDate())
                && java.util.Objects.equals(demande.commentaire(), bon.getCommentaire());
    }
    private PreuveLivraisonResponse response(BonLivraison bon) {
        return new PreuveLivraisonResponse(bon.getId(), bon.getTransportPreuveId(), bon.getRecepteur(),
                bon.getLivreLe().toInstant().atZone(ZoneOffset.UTC).toLocalDate(), bon.getCommentaire(), bon.getStatut().name());
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}

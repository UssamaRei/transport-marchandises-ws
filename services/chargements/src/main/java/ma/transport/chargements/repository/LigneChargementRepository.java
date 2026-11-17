package ma.transport.chargements.repository;

import java.util.List;
import ma.transport.chargements.model.LigneChargement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LigneChargementRepository {

    private final JdbcTemplate jdbc;

    public LigneChargementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LigneChargement> findByChargementId(String chargementId) {
        String sql = "SELECT id, designation, quantite, poids_unitaire_kg FROM ligne_chargement WHERE chargement_id = ? ORDER BY id";
        return jdbc.query(sql, new LigneChargementRowMapper(), chargementId);
    }

    public void save(String chargementId, LigneChargement ligne) {
        String sql = """
                INSERT INTO ligne_chargement (id, chargement_id, designation, quantite, poids_unitaire_kg)
                VALUES (?, ?, ?, ?, ?)
                """;
        jdbc.update(sql, ligne.getId(), chargementId, ligne.getDesignation(),
                ligne.getQuantite(), ligne.getPoidsUnitaireKg());
    }

    public int update(String chargementId, LigneChargement ligne) {
        String sql = """
                UPDATE ligne_chargement SET designation = ?, quantite = ?, poids_unitaire_kg = ?
                WHERE id = ? AND chargement_id = ?
                """;
        return jdbc.update(sql, ligne.getDesignation(), ligne.getQuantite(),
                ligne.getPoidsUnitaireKg(), ligne.getId(), chargementId);
    }

    public int delete(String chargementId, String ligneId) {
        String sql = "DELETE FROM ligne_chargement WHERE id = ? AND chargement_id = ?";
        return jdbc.update(sql, ligneId, chargementId);
    }
}

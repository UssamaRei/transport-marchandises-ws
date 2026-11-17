package ma.transport.chargements.repository;

import java.util.List;
import java.util.Optional;
import ma.transport.chargements.model.Chargement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChargementRepository {

    private static final String COLUMNS = "id, origine, destination, destinataire_nom, "
            + "destinataire_telephone, camion_souhaite_id, poids_total_kg, statut, version, created_at";

    private final JdbcTemplate jdbc;

    public ChargementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(Chargement chargement) {
        String sql = """
                INSERT INTO chargement (id, origine, destination, destinataire_nom,
                    destinataire_telephone, poids_total_kg, statut)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        jdbc.update(sql, chargement.getId(), chargement.getOrigine(), chargement.getDestination(),
                chargement.getDestinataireNom(), chargement.getDestinataireTelephone(),
                chargement.getPoidsTotalKg(), chargement.getStatut().name());
    }

    public Optional<Chargement> findById(String id) {
        String sql = "SELECT " + COLUMNS + " FROM chargement WHERE id = ?";
        List<Chargement> chargements = jdbc.query(sql, new ChargementRowMapper(), id);
        return chargements.stream().findFirst();
    }

    public Optional<Chargement> findByIdForUpdate(String id) {
        String sql = "SELECT " + COLUMNS + " FROM chargement WHERE id = ? FOR UPDATE";
        List<Chargement> chargements = jdbc.query(sql, new ChargementRowMapper(), id);
        return chargements.stream().findFirst();
    }

    public List<Chargement> findAll(int limit, int offset) {
        String sql = "SELECT " + COLUMNS + " FROM chargement ORDER BY created_at DESC, id LIMIT ? OFFSET ?";
        return jdbc.query(sql, new ChargementRowMapper(), limit, offset);
    }

    public void update(Chargement chargement) {
        String sql = """
                UPDATE chargement SET origine = ?, destination = ?, destinataire_nom = ?,
                    destinataire_telephone = ?, camion_souhaite_id = ?, poids_total_kg = ?,
                    statut = ?, version = version + 1
                WHERE id = ?
                """;
        jdbc.update(sql, chargement.getOrigine(), chargement.getDestination(),
                chargement.getDestinataireNom(), chargement.getDestinataireTelephone(),
                chargement.getCamionSouhaiteId(), chargement.getPoidsTotalKg(),
                chargement.getStatut().name(), chargement.getId());
    }

    public void deleteById(String id) {
        jdbc.update("DELETE FROM chargement WHERE id = ?", id);
    }
}

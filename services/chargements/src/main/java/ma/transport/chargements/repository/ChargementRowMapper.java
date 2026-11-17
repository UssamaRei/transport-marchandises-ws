package ma.transport.chargements.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import ma.transport.chargements.model.Chargement;
import ma.transport.chargements.model.StatutChargement;
import org.springframework.jdbc.core.RowMapper;

public class ChargementRowMapper implements RowMapper<Chargement> {

    @Override
    public Chargement mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Chargement chargement = new Chargement();
        chargement.setId(resultSet.getString("id"));
        chargement.setOrigine(resultSet.getString("origine"));
        chargement.setDestination(resultSet.getString("destination"));
        chargement.setDestinataireNom(resultSet.getString("destinataire_nom"));
        chargement.setDestinataireTelephone(resultSet.getString("destinataire_telephone"));
        chargement.setCamionSouhaiteId(resultSet.getString("camion_souhaite_id"));
        chargement.setPoidsTotalKg(resultSet.getBigDecimal("poids_total_kg"));
        chargement.setStatut(StatutChargement.valueOf(resultSet.getString("statut")));
        chargement.setVersion(resultSet.getInt("version"));
        chargement.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        return chargement;
    }
}

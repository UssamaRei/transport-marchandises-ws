package ma.transport.chargements.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import ma.transport.chargements.model.LigneChargement;
import org.springframework.jdbc.core.RowMapper;

public class LigneChargementRowMapper implements RowMapper<LigneChargement> {

    @Override
    public LigneChargement mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        LigneChargement ligne = new LigneChargement();
        ligne.setId(resultSet.getString("id"));
        ligne.setDesignation(resultSet.getString("designation"));
        ligne.setQuantite(resultSet.getInt("quantite"));
        ligne.setPoidsUnitaireKg(resultSet.getBigDecimal("poids_unitaire_kg"));
        return ligne;
    }
}

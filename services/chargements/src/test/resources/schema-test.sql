-- Sous-ensemble de la migration commune pour les tests H2 uniquement.
CREATE TABLE camion (
    id varchar(64) PRIMARY KEY,
    immatriculation varchar(40) NOT NULL UNIQUE,
    capacite_kg numeric(14,3) NOT NULL
);
CREATE TABLE chargement (
    id varchar(64) PRIMARY KEY,
    origine varchar(500),
    destination varchar(500),
    destinataire_nom varchar(160),
    destinataire_telephone varchar(40),
    camion_souhaite_id varchar(64) REFERENCES camion(id),
    poids_total_kg numeric(14,3) NOT NULL DEFAULT 0 CHECK (poids_total_kg >= 0),
    statut varchar(20) NOT NULL DEFAULT 'BROUILLON' CHECK (statut IN ('BROUILLON', 'VALIDE')),
    version integer NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (statut <> 'VALIDE' OR (
        origine IS NOT NULL AND trim(origine) <> '' AND
        destination IS NOT NULL AND trim(destination) <> '' AND
        destinataire_nom IS NOT NULL AND trim(destinataire_nom) <> '' AND
        camion_souhaite_id IS NOT NULL AND poids_total_kg > 0
    ))
);
CREATE TABLE ligne_chargement (
    id varchar(64) PRIMARY KEY,
    chargement_id varchar(64) NOT NULL REFERENCES chargement(id) ON DELETE CASCADE,
    designation varchar(240) NOT NULL CHECK (trim(designation) <> ''),
    quantite integer NOT NULL CHECK (quantite > 0),
    poids_unitaire_kg numeric(14,3) NOT NULL CHECK (poids_unitaire_kg > 0)
);

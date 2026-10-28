-- Proposition PostgreSQL pour la base Chargements uniquement.
CREATE TABLE chargement (
    id varchar(64) PRIMARY KEY,
    origine varchar(500),
    destination varchar(500),
    destinataire_nom varchar(160),
    destinataire_telephone varchar(40),
    camion_souhaite_id varchar(64), -- Reference API Flotte, pas de FK interbase
    poids_total_kg numeric(14,3) NOT NULL DEFAULT 0 CHECK (poids_total_kg >= 0),
    statut varchar(20) NOT NULL DEFAULT 'BROUILLON'
        CHECK (statut IN ('BROUILLON','VALIDE')),
    version integer NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (statut <> 'VALIDE' OR (
        origine IS NOT NULL AND btrim(origine) <> '' AND
        destination IS NOT NULL AND btrim(destination) <> '' AND
        destinataire_nom IS NOT NULL AND btrim(destinataire_nom) <> '' AND
        camion_souhaite_id IS NOT NULL AND poids_total_kg > 0
    ))
);

CREATE TABLE ligne_chargement (
    id varchar(64) PRIMARY KEY,
    chargement_id varchar(64) NOT NULL REFERENCES chargement(id) ON DELETE CASCADE,
    designation varchar(240) NOT NULL CHECK (btrim(designation) <> ''),
    quantite integer NOT NULL CHECK (quantite > 0),
    poids_unitaire_kg numeric(14,3) NOT NULL CHECK (poids_unitaire_kg > 0)
);
CREATE INDEX lignes_par_chargement ON ligne_chargement(chargement_id);

-- Regles applicatives : recalculer le total dans la transaction des lignes,
-- imposer au moins une ligne pour valider, puis interdire toute modification
-- ou suppression du valide. Incrementer version pour detecter la concurrence.

CREATE TABLE idempotence (
    cle varchar(160) PRIMARY KEY,
    request_hash varchar(64) NOT NULL,
    statut varchar(20) NOT NULL CHECK (statut IN ('EN_COURS','TERMINE')),
    resource_id varchar(64),
    http_status smallint CHECK (http_status BETWEEN 100 AND 599),
    reponse jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

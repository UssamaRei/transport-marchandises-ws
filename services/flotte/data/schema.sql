-- Proposition PostgreSQL pour la base Flotte uniquement.
-- Aucun acces SQL aux bases des autres modules.
CREATE TABLE camion (
    id varchar(64) PRIMARY KEY,
    immatriculation varchar(40) NOT NULL UNIQUE,
    capacite_kg numeric(14,3) NOT NULL CHECK (capacite_kg > 0),
    etat varchar(20) NOT NULL DEFAULT 'EN_SERVICE'
        CHECK (etat IN ('EN_SERVICE','MAINTENANCE')),
    archive boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE reservation_camion (
    id varchar(64) PRIMARY KEY,
    camion_id varchar(64) NOT NULL REFERENCES camion(id),
    transport_id varchar(64) NOT NULL UNIQUE, -- Reference API externe
    poids_kg numeric(14,3) NOT NULL CHECK (poids_kg > 0),
    statut varchar(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (statut IN ('ACTIVE','LIBEREE')),
    motif_liberation varchar(20) CHECK (motif_liberation IN ('LIVRAISON','ANNULATION')),
    created_at timestamptz NOT NULL DEFAULT now(),
    released_at timestamptz,
    CHECK ((statut = 'ACTIVE' AND released_at IS NULL AND motif_liberation IS NULL)
        OR (statut = 'LIBEREE' AND released_at IS NOT NULL AND motif_liberation IS NOT NULL)),
    CHECK (released_at IS NULL OR released_at >= created_at)
);

CREATE UNIQUE INDEX une_reservation_active_par_camion
    ON reservation_camion(camion_id) WHERE statut = 'ACTIVE';

-- Le backend doit verrouiller camion pour reserver ET pour changer son etat,
-- sa capacite ou son archivage. La capacite se controle dans cette transaction.

CREATE TABLE idempotence (
    cle varchar(160) PRIMARY KEY,
    request_hash varchar(64) NOT NULL, -- Methode + chemin + corps normalise
    statut varchar(20) NOT NULL CHECK (statut IN ('EN_COURS','TERMINE')),
    resource_id varchar(64),
    http_status smallint CHECK (http_status BETWEEN 100 AND 599),
    reponse jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

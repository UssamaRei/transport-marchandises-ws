-- Proposition PostgreSQL pour la base Bons uniquement.
CREATE TABLE bon_livraison (
    id varchar(64) PRIMARY KEY,
    numero varchar(64) NOT NULL UNIQUE,
    chargement_id varchar(64) NOT NULL UNIQUE, -- Reference API Chargements
    camion_souhaite_id varchar(64) NOT NULL, -- Copie d'une reference Flotte
    origine varchar(500) NOT NULL,
    destination varchar(500) NOT NULL,
    destinataire_nom varchar(160) NOT NULL,
    destinataire_telephone varchar(40),
    poids_total_kg numeric(14,3) NOT NULL CHECK (poids_total_kg > 0),
    statut varchar(20) NOT NULL DEFAULT 'EMIS' CHECK (statut IN ('EMIS','LIVRE')),
    transport_preuve_id varchar(64) UNIQUE, -- Reference API Transports
    recepteur varchar(160),
    livre_le timestamptz,
    commentaire varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((statut = 'EMIS' AND transport_preuve_id IS NULL AND recepteur IS NULL AND livre_le IS NULL)
        OR (statut = 'LIVRE' AND transport_preuve_id IS NOT NULL
            AND recepteur IS NOT NULL AND btrim(recepteur) <> '' AND livre_le IS NOT NULL))
);

CREATE TABLE ligne_bon (
    id varchar(64) PRIMARY KEY,
    bon_id varchar(64) NOT NULL REFERENCES bon_livraison(id),
    designation varchar(240) NOT NULL,
    quantite integer NOT NULL CHECK (quantite > 0),
    poids_unitaire_kg numeric(14,3) NOT NULL CHECK (poids_unitaire_kg > 0)
);
CREATE INDEX lignes_par_bon ON ligne_bon(bon_id);

-- L'application cree le bon et au moins une ligne dans une seule transaction.
-- Un numero doit etre genere sans course concurrente (sequence ou equivalent).
-- Les donnees copiees ne sont pas modifiables apres emission.

CREATE TABLE idempotence (
    cle varchar(160) PRIMARY KEY,
    request_hash varchar(64) NOT NULL,
    statut varchar(20) NOT NULL CHECK (statut IN ('EN_COURS','TERMINE')),
    resource_id varchar(64),
    http_status smallint CHECK (http_status BETWEEN 100 AND 599),
    reponse jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Proposition PostgreSQL pour la base Transports uniquement.
CREATE TABLE transport (
    id varchar(64) PRIMARY KEY,
    bon_id varchar(64) NOT NULL, -- Reference API Bons
    camion_id varchar(64) NOT NULL, -- Reference API Flotte
    reservation_id varchar(64) UNIQUE, -- Reference API Flotte, absente en preparation
    poids_kg numeric(14,3) NOT NULL CHECK (poids_kg > 0),
    destination varchar(500) NOT NULL,
    depart_prevu timestamptz NOT NULL,
    depart_reel timestamptz,
    arrivee_reelle timestamptz,
    statut varchar(32) NOT NULL DEFAULT 'PREPARATION' CHECK (statut IN (
        'PREPARATION','PLANIFIE','EN_COURS','CLOTURE_EN_ATTENTE',
        'LIVRE','ANNULATION_EN_ATTENTE','ANNULE'
    )),
    sync_etat varchar(20) NOT NULL DEFAULT 'A_REPRENDRE'
        CHECK (sync_etat IN ('OK','A_REPRENDRE')),
    operation_key varchar(160) NOT NULL UNIQUE,
    preuve_confirmee boolean NOT NULL DEFAULT false,
    liberation_confirmee boolean NOT NULL DEFAULT false,
    livraison_payload jsonb,
    motif_annulation varchar(500),
    derniere_erreur text,
    version integer NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (statut IN ('PREPARATION','ANNULATION_EN_ATTENTE','ANNULE') OR reservation_id IS NOT NULL),
    CHECK (arrivee_reelle IS NULL OR (depart_reel IS NOT NULL AND arrivee_reelle >= depart_reel)),
    CHECK (statut <> 'LIVRE' OR (preuve_confirmee AND liberation_confirmee
        AND depart_reel IS NOT NULL AND arrivee_reelle IS NOT NULL))
);
CREATE UNIQUE INDEX un_transport_non_annule_par_bon
    ON transport(bon_id) WHERE statut <> 'ANNULE';
CREATE INDEX transports_par_camion ON transport(camion_id, statut);

CREATE TABLE evenement_suivi (
    id varchar(64) PRIMARY KEY,
    transport_id varchar(64) NOT NULL REFERENCES transport(id),
    type varchar(24) NOT NULL CHECK (type IN ('DEPART','POINTAGE','ARRIVEE','COMMENTAIRE')),
    description varchar(1000) NOT NULL,
    date_evenement timestamptz NOT NULL,
    latitude numeric(10,7) CHECK (latitude BETWEEN -90 AND 90),
    longitude numeric(11,7) CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX evenements_par_transport ON evenement_suivi(transport_id, date_evenement, id);

-- Les transitions d'etat, la confirmation de toute annulation et les appels
-- distants se controlent dans l'application. Garder les etapes en base pour
-- reprendre apres un redemarrage ; verrouiller/versionner le transport.

CREATE TABLE idempotence (
    cle varchar(160) PRIMARY KEY,
    request_hash varchar(64) NOT NULL,
    statut varchar(20) NOT NULL CHECK (statut IN ('EN_COURS','TERMINE')),
    resource_id varchar(64),
    http_status smallint CHECK (http_status BETWEEN 100 AND 599),
    reponse jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

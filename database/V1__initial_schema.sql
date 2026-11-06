-- Base PostgreSQL commune aux quatre backends. Exécuter une seule fois, en transaction.
-- Identifiants String : UUID générés par défaut ; poids en kg, dates en UTC.
CREATE TABLE public.camion (
    id varchar(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    immatriculation varchar(40) NOT NULL UNIQUE,
    capacite_kg numeric(14,3) NOT NULL CHECK (capacite_kg > 0),
    etat varchar(20) NOT NULL DEFAULT 'EN_SERVICE' CHECK (etat IN ('EN_SERVICE', 'MAINTENANCE')),
    archive boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.chargement (
    id varchar(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    origine varchar(500),
    destination varchar(500),
    destinataire_nom varchar(160),
    destinataire_telephone varchar(40),
    camion_souhaite_id varchar(64) REFERENCES public.camion(id),
    poids_total_kg numeric(14,3) NOT NULL DEFAULT 0 CHECK (poids_total_kg >= 0),
    statut varchar(20) NOT NULL DEFAULT 'BROUILLON' CHECK (statut IN ('BROUILLON', 'VALIDE')),
    version integer NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (statut <> 'VALIDE' OR (
        origine IS NOT NULL AND btrim(origine) <> '' AND
        destination IS NOT NULL AND btrim(destination) <> '' AND
        destinataire_nom IS NOT NULL AND btrim(destinataire_nom) <> '' AND
        camion_souhaite_id IS NOT NULL AND poids_total_kg > 0
    ))
);

CREATE TABLE public.ligne_chargement (
    id varchar(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    chargement_id varchar(64) NOT NULL REFERENCES public.chargement(id) ON DELETE CASCADE,
    designation varchar(240) NOT NULL CHECK (btrim(designation) <> ''),
    quantite integer NOT NULL CHECK (quantite > 0),
    poids_unitaire_kg numeric(14,3) NOT NULL CHECK (poids_unitaire_kg > 0)
);
CREATE INDEX lignes_par_chargement ON public.ligne_chargement(chargement_id);

CREATE TABLE public.bon_livraison (
    id varchar(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    numero varchar(64) NOT NULL UNIQUE DEFAULT ('BL-' || gen_random_uuid()::text),
    chargement_id varchar(64) NOT NULL UNIQUE REFERENCES public.chargement(id),
    camion_souhaite_id varchar(64) NOT NULL REFERENCES public.camion(id),
    origine varchar(500) NOT NULL,
    destination varchar(500) NOT NULL,
    destinataire_nom varchar(160) NOT NULL,
    destinataire_telephone varchar(40),
    poids_total_kg numeric(14,3) NOT NULL CHECK (poids_total_kg > 0),
    statut varchar(20) NOT NULL DEFAULT 'EMIS' CHECK (statut IN ('EMIS', 'LIVRE')),
    transport_preuve_id varchar(64) UNIQUE,
    recepteur varchar(160),
    livre_le timestamptz,
    commentaire varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((statut = 'EMIS' AND transport_preuve_id IS NULL AND recepteur IS NULL AND livre_le IS NULL)
        OR (statut = 'LIVRE' AND transport_preuve_id IS NOT NULL
            AND recepteur IS NOT NULL AND btrim(recepteur) <> '' AND livre_le IS NOT NULL))
);

CREATE TABLE public.ligne_bon (
    id varchar(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    bon_id varchar(64) NOT NULL REFERENCES public.bon_livraison(id),
    designation varchar(240) NOT NULL CHECK (btrim(designation) <> ''),
    quantite integer NOT NULL CHECK (quantite > 0),
    poids_unitaire_kg numeric(14,3) NOT NULL CHECK (poids_unitaire_kg > 0)
);
CREATE INDEX lignes_par_bon ON public.ligne_bon(bon_id);

CREATE TABLE public.transport (
    id varchar(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    bon_id varchar(64) NOT NULL REFERENCES public.bon_livraison(id),
    camion_id varchar(64) NOT NULL REFERENCES public.camion(id),
    reservation_id varchar(64) UNIQUE,
    poids_kg numeric(14,3) NOT NULL CHECK (poids_kg > 0),
    destination varchar(500) NOT NULL,
    depart_prevu timestamptz NOT NULL,
    depart_reel timestamptz,
    arrivee_reelle timestamptz,
    statut varchar(32) NOT NULL DEFAULT 'PREPARATION' CHECK (statut IN (
        'PREPARATION', 'PLANIFIE', 'EN_COURS', 'CLOTURE_EN_ATTENTE',
        'LIVRE', 'ANNULATION_EN_ATTENTE', 'ANNULE'
    )),
    sync_etat varchar(20) NOT NULL DEFAULT 'A_REPRENDRE' CHECK (sync_etat IN ('OK', 'A_REPRENDRE')),
    operation_key varchar(160) NOT NULL UNIQUE,
    preuve_confirmee boolean NOT NULL DEFAULT false,
    liberation_confirmee boolean NOT NULL DEFAULT false,
    version integer NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, bon_id),
    CHECK (statut IN ('PREPARATION', 'ANNULATION_EN_ATTENTE', 'ANNULE') OR reservation_id IS NOT NULL),
    CHECK (arrivee_reelle IS NULL OR (depart_reel IS NOT NULL AND arrivee_reelle >= depart_reel)),
    CHECK (statut <> 'LIVRE' OR (preuve_confirmee AND liberation_confirmee
        AND depart_reel IS NOT NULL AND arrivee_reelle IS NOT NULL))
);
CREATE UNIQUE INDEX un_transport_non_annule_par_bon ON public.transport(bon_id) WHERE statut <> 'ANNULE';
CREATE INDEX transports_par_camion ON public.transport(camion_id, statut);

CREATE TABLE public.reservation_camion (
    id varchar(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    camion_id varchar(64) NOT NULL REFERENCES public.camion(id),
    transport_id varchar(64) NOT NULL UNIQUE REFERENCES public.transport(id),
    poids_kg numeric(14,3) NOT NULL CHECK (poids_kg > 0),
    statut varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (statut IN ('ACTIVE', 'LIBEREE')),
    motif_liberation varchar(20) CHECK (motif_liberation IN ('LIVRAISON', 'ANNULATION')),
    created_at timestamptz NOT NULL DEFAULT now(),
    released_at timestamptz,
    UNIQUE (id, transport_id, camion_id),
    CHECK ((statut = 'ACTIVE' AND released_at IS NULL AND motif_liberation IS NULL)
        OR (statut = 'LIBEREE' AND released_at IS NOT NULL AND motif_liberation IS NOT NULL)),
    CHECK (released_at IS NULL OR released_at >= created_at)
);
CREATE UNIQUE INDEX une_reservation_active_par_camion ON public.reservation_camion(camion_id) WHERE statut = 'ACTIVE';

ALTER TABLE public.transport ADD CONSTRAINT transport_reservation_coherente
    FOREIGN KEY (reservation_id, id, camion_id) REFERENCES public.reservation_camion(id, transport_id, camion_id);
ALTER TABLE public.bon_livraison ADD CONSTRAINT preuve_du_bon
    FOREIGN KEY (transport_preuve_id, id) REFERENCES public.transport(id, bon_id);

CREATE TABLE public.evenement_suivi (
    id varchar(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    transport_id varchar(64) NOT NULL REFERENCES public.transport(id),
    type varchar(24) NOT NULL CHECK (type IN ('DEPART', 'POINTAGE', 'ARRIVEE', 'COMMENTAIRE')),
    description varchar(1000) NOT NULL,
    date_evenement timestamptz NOT NULL,
    latitude numeric(10,7) CHECK (latitude BETWEEN -90 AND 90),
    longitude numeric(11,7) CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX evenements_par_transport ON public.evenement_suivi(transport_id, date_evenement, id);

-- À assurer dans les backends : poids recalculé, au moins une ligne pour valider/émettre,
-- chargement validé immuable, contrôle de capacité et réservation atomique,
-- reprise des opérations sans doublon et preuve/libération avant LIVRE.

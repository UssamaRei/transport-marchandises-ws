CREATE TABLE public.camion (
    id varchar(64) PRIMARY KEY,
    immatriculation varchar(40) NOT NULL UNIQUE,
    capacite_kg numeric(14,3) NOT NULL CHECK (capacite_kg > 0),
    etat varchar(20) NOT NULL CHECK (etat IN ('EN_SERVICE', 'MAINTENANCE')),
    archive boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.transport (
    id varchar(64) PRIMARY KEY
);

CREATE TABLE public.reservation_camion (
    id varchar(64) PRIMARY KEY,
    camion_id varchar(64) NOT NULL REFERENCES public.camion(id),
    transport_id varchar(64) NOT NULL UNIQUE REFERENCES public.transport(id),
    poids_kg numeric(14,3) NOT NULL CHECK (poids_kg > 0),
    statut varchar(20) NOT NULL CHECK (statut IN ('ACTIVE', 'LIBEREE')),
    motif_liberation varchar(20) CHECK (motif_liberation IN ('LIVRAISON', 'ANNULATION')),
    created_at timestamptz NOT NULL DEFAULT now(),
    released_at timestamptz,
    CHECK ((statut = 'ACTIVE' AND released_at IS NULL AND motif_liberation IS NULL)
        OR (statut = 'LIBEREE' AND released_at IS NOT NULL AND motif_liberation IS NOT NULL)),
    CHECK (released_at IS NULL OR released_at >= created_at)
);

CREATE UNIQUE INDEX une_reservation_active_par_camion
    ON public.reservation_camion(camion_id) WHERE statut = 'ACTIVE';

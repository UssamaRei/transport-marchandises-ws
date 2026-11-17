-- Conserver la preuve preparee avant un appel a Bons, afin de reprendre une livraison.
-- Appliquer apres V1, avec le compte proprietaire et dans une transaction.
ALTER TABLE public.transport
    ADD COLUMN IF NOT EXISTS preuve_receptionnaire varchar(160),
    ADD COLUMN IF NOT EXISTS preuve_date_reception date,
    ADD COLUMN IF NOT EXISTS preuve_commentaire varchar(1000);

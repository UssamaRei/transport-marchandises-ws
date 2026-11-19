\getenv test_password POSTGRES_PASSWORD
CREATE ROLE camions_app LOGIN PASSWORD :'test_password';
CREATE ROLE chargements_app LOGIN PASSWORD :'test_password';
CREATE ROLE bons_app LOGIN PASSWORD :'test_password';
CREATE ROLE transports_app LOGIN PASSWORD :'test_password';
GRANT USAGE ON SCHEMA public TO camions_app, chargements_app, bons_app, transports_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON camion, reservation_camion TO camions_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON chargement, ligne_chargement TO chargements_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON bon_livraison, ligne_bon TO bons_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON transport, evenement_suivi TO transports_app;
CREATE DATABASE flotte_test;

# Bilan d'intégration — 19 novembre 2026

Branche : `integration-systeme`, basée sur `main` au commit `1f9bd71`, avec
l'implémentation Bons de la branche `bons-livraison` (`36cdd3a`).
Aucune fusion vers `main`.

## Résultats

**186 tests Maven et 16 scénarios d'intégration réussis. Aucun test ignoré.**

| Module | Tests Maven | Échecs / erreurs / ignorés |
|---|---:|---|
| Camions | 46 | 0 / 0 / 0 |
| Chargements | 36 | 0 / 0 / 0 |
| Bons de livraison | 7 | 0 / 0 / 0 |
| Transports / Suivi | 97 | 0 / 0 / 0 |

Les 16 scénarios exécutent les quatre applications réelles, sans frontend ni
simulation d'API métier. Ils utilisent Keycloak (connexion PKCE et comptes
techniques) et une seule base PostgreSQL 18 jetable, avec V1/V2 et les quatre
comptes DB limités à leurs tables. La base Neon de l'équipe n'a pas été modifiée.

Le parcours vérifié : créer un camion et un chargement, ajouter les marchandises,
valider, émettre le bon, planifier et réserver, déclarer le départ, ajouter un
pointage, enregistrer l'arrivée, confirmer la preuve puis libérer le camion.

Autres contrôles : annulation et nouvelle planification, capacité, chargement
immuable, sources absentes, doublons, concurrence, états interdits, dates précises,
JWT et permissions, privilèges DB et cohérence finale. Des arrêts réels de Bons
et Camions vérifient la reprise de planification et de livraison sans doublon.

## Changements par étudiant

| Étudiant / module | Modification apportée |
|---|---|
| Hassan — Camions | Ajout de la vérification des JWT Keycloak et des rôles humains/techniques. Un lecteur ne peut plus modifier les données. Ajout de quatre tests de sécurité et adaptation des tests existants pour s'authentifier. Règles de capacité et réservation conservées. |
| Oussama — Chargements | Aucune modification du code métier. Vérification des 36 tests existants et des échanges avec les vrais services Camions et Bons. |
| Hmad — Bons | Intégration de son backend, puis correction de `createdAt`, qui était nul dans la réponse de création. Hibernate récupère maintenant la date générée par PostgreSQL. |
| Ayoub — Transports | Ajout du packaging Spring Boot pour lancer le JAR. Normalisation des dates en UTC à la microseconde pour accepter les répétitions identiques après stockage en PostgreSQL. Ajout de deux tests sur les dates. |

En commun : lanceur et Compose de test, journaux et bilan JSON, configuration
Keycloak interne de Camions dans le Compose local, et correction de liens de
documentation dont une référence à un script SQL absent. Aucune migration ajoutée.

## Reproduire

```powershell
python tests/integration/run.py
```

Prérequis et options : [README](README.md). Les résultats détaillés sont générés
dans `.tools/systeme-test/`, ignoré par Git. Les conteneurs de test sont supprimés
automatiquement à la fin.

# Tester les quatre backends ensemble

[Résultats et changements par étudiant](BILAN.md).

Prérequis : Docker Desktop démarré, Java 21, Maven et Python 3.11 ou plus.
Depuis la racine du dépôt :

```powershell
python tests/integration/run.py
```

Le script exécute les tests Maven, construit les quatre JAR et teste leurs échanges
HTTP réels avec Keycloak. Aucune interface web n'est nécessaire et aucune API métier
n'est simulée. Les données sont créées par les API.

Une seule base **PostgreSQL 18 locale et jetable** contient le schéma V1/V2.
Chaque service utilise son propre compte, limité à ses tables. Les identifiants
Neon et le Keycloak habituel ne sont pas utilisés. Les secrets de test sont générés
dans `.secrets/systeme-test.env`, ignoré par Git.

Scénarios : livraison complète, annulation et nouvelle tentative, poids/capacité,
permissions et JWT, doublons, concurrence, états interdits, puis reprise après
arrêt réel de Bons ou Camions. Les contrôles SQL finaux vérifient la cohérence des
preuves et réservations ; ils ne créent pas les données métier.

Les conteneurs de test sont supprimés à la fin, même en cas d'échec.
Les journaux Maven, Docker et le bilan JSON restent dans `.tools/systeme-test/`.
Pour les conserver temporairement :

```powershell
python tests/integration/run.py --keep
python tests/integration/run.py --down
```

Ports de test : Camions `18081`, Chargements `18082`, Bons `18083`, Transports
`18084`, Keycloak `18090`, PostgreSQL `15432`. Le projet Docker est
`ws-systeme-test`. `--skip-build` réutilise les JAR existants après une vérification
précédente ; après une modification Java, relancer sans cette option.

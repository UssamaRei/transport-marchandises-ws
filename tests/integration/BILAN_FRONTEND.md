# Intégration du frontend — 19 novembre 2026

Branche : `integration-frontend`. Elle rassemble `main` (`a678a9b`), les correctifs
testés de `integration-systeme` et le frontend Bons de `bons-livraison` (`fb97760`).
Les tests JWT signés ajoutés par Hassan ont été conservés pendant la fusion.
Aucune fusion vers `main` n'a été effectuée.

## Ce qui fonctionne dans le navigateur

Créer et modifier un camion → préparer et valider un chargement → émettre son bon
→ choisir un camion et planifier → déclarer le départ, un pointage et l'arrivée
→ enregistrer la réception → retrouver le bon livré et le camion disponible.

Les modules partagent la connexion Keycloak et la même navigation. Les liens
transmettent le chargement ou le bon sélectionné ; il n'est plus nécessaire de
copier leurs identifiants pour effectuer ce parcours. Les maquettes d'origine
restent séparées à `/maquettes`.

## Modifications

| Partie | Modification |
|---|---|
| Camions | Écrans réels de liste, disponibilité, création, détail et modification. Backend et authentification de Hassan conservés. |
| Chargements | Lien depuis un chargement validé vers l'émission de son bon. |
| Bons | Frontend de Hmad intégré ; ajout d'une liste paginée des bons côté API et interface. Correction de la sélection, de la pagination et de l'affichage sur mobile. Liens vers le chargement et la planification. |
| Transports | Choix des bons et camions depuis les API. Liens vers leurs fiches. Conservation de la preuve en cas de panne et de rechargement. Le formulaire reste monté et est désactivé pendant une actualisation pour préserver les valeurs saisies. |
| Lancement | `demarrer-local.ps1` et le Compose local démarrent les quatre backends et attendent leur disponibilité avant de lancer l'interface. |

## Vérification

- **190 tests Maven réussis**, aucun ignoré : Camions 48, Chargements 36, Bons 9, Transports 97.
- **17 scénarios API réussis**, dont les réservations concurrentes, les reprises et la pagination des bons.
- **3 tests navigateur réussis** : parcours complet, panne réelle de Bons avec reprise après rechargement, puis lecture seule, mobile à 390 px, erreurs et déconnexion.
- Construction TypeScript/Vite réussie et captures inspectées.
- Lanceur normal vérifié en lecture seule sur la base partagée : les quatre API
  répondent `200`, sans écriture métier ni erreur navigateur.

Les écritures de test utilisent uniquement la pile locale jetable : quatre vrais
backends, Keycloak et une base PostgreSQL 18 commune avec les comptes limités.
Aucune API métier n'est simulée. La base Neon n'est pas modifiée par ces tests.

Lancer la vérification complète, avec Chrome installé et le port 5173 libre :

```powershell
python tests/integration/run_frontend.py
```

Les résultats JSON, journaux et captures sont générés dans `.tools/frontend-systeme/`.
Le lanceur supprime les conteneurs de test à la fin. Pour utiliser l'application
avec les connexions habituelles de l'équipe : `./demarrer-local.ps1`.

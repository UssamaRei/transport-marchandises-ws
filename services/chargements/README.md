# Chargements - Sina Oussama

Préparer les marchandises, calculer leur poids et valider un chargement avec un camion adapté.
Backend **Java 21 / Spring Boot / PostgreSQL**, sur le port **8082**.

## Ce qui fonctionne

- Créer, consulter, modifier et supprimer un brouillon.
- Ajouter, modifier et supprimer ses lignes de marchandises.
- Recalculer le poids total : somme des quantités × poids unitaires.
- Valider après vérification du camion par **OpenFeign**.
- Fournir le détail et les lignes au backend Bons de livraison.

Un chargement `VALIDE` ne se modifie plus. La validation ne réserve pas le camion :
Transports le réservera à la planification, avec un nouveau contrôle de disponibilité.
Tables gérées : `chargement`, `ligne_chargement` dans la base commune.

## Démarrer

Prérequis : **JDK 21**. Le wrapper télécharge Maven au premier lancement.

1. Ouvrir ce dossier comme projet Maven dans l’IDE.
2. Configurer les trois variables `SPRING_DATASOURCE_*` du fichier privé `chargements_app.env`.
3. Définir `CAMIONS_SERVICE_URL` avec l’adresse de Camions (défaut : `http://localhost:8081`).
4. Lancer `ChargementsApplication`, ou `./mvnw spring-boot:run` (`.\mvnw.cmd` sous Windows).

Sous PowerShell, charger le fichier privé et démarrer en une commande :

```powershell
.\run-local.ps1 -EnvFile "$HOME\Desktop\Neon-WS-Credentials\chargements_app.env"
```

Le script charge seulement les variables nécessaires au backend. Un fichier `.env` n’est pas
chargé par Spring Boot sans ce script. Voir [.env.example](.env.example).
Ne jamais enregistrer les identifiants dans Git. Les tables existent déjà sur Neon ;
l’application ne crée ni ne modifie le schéma.

- Swagger : [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)
- OpenAPI : [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs)

Le CRUD des brouillons fonctionne même si Camions est arrêté. La validation a besoin de son API.
Son contrat ci-dessous doit être repris par le module Camions.

## API exposée

Préfixe : `/api/v1/chargements`. Corps et réponses en JSON.

| Méthode et route | Résultat |
|---|---|
| `POST` sur le préfixe | Créer un brouillon (`201`, en-tête `Location`) |
| `GET` sur le préfixe avec `?limit=20&offset=0` | Lister les résumés, sans lignes (maximum 100 par page) |
| `GET /{id}` | Lire le détail complet, notamment pour Bons de livraison |
| `PUT /{id}` | Remplacer les informations du brouillon |
| `DELETE /{id}` | Supprimer le brouillon et ses lignes (`204`) |
| `POST /{id}/lignes` | Ajouter une ligne (`201`) |
| `PUT /{id}/lignes/{ligneId}` | Modifier une ligne |
| `DELETE /{id}/lignes/{ligneId}` | Supprimer une ligne |
| `POST /{id}/validation` | Valider avec le camion demandé |

Les routes de lignes renvoient le chargement complet avec son nouveau poids.
Pour la création et la liste, utiliser `/api/v1/chargements` sans slash final.

### Essai rapide dans Swagger ou Postman

**1. Créer** avec `POST /api/v1/chargements` :

```json
{
  "origine": "Agadir",
  "destination": "Casablanca",
  "destinataireNom": "Client A",
  "destinataireTelephone": "0600000000"
}
```

On peut aussi créer un brouillon vide avec `{}`. Noter son `id`.
`PUT /{id}` remplace ces quatre champs ; un champ omis devient `null`.

**2. Ajouter une ligne** avec `POST /api/v1/chargements/{id}/lignes` :

```json
{ "designation": "Cartons", "quantite": 10, "poidsUnitaireKg": 12.5 }
```

Le poids total devient `125.000` kg. Quantité : entier positif ; poids : positif, maximum trois décimales.
L’ID de la ligne est dans `lignes`. Le client ne fournit ni le poids total ni le statut.

**3. Valider** avec `POST /api/v1/chargements/{id}/validation` :

```json
{ "camionId": "ID_D_UN_CAMION_EXISTANT" }
```

Origine, destination, destinataire et au moins une ligne sont obligatoires pour valider.
Une nouvelle validation avec le même camion renvoie le résultat existant.

## API consommée : Camions

OpenFeign appelle **`GET {CAMIONS_SERVICE_URL}/api/v1/camions/{id}`**.
Le module Camions doit renvoyer au minimum :

```json
{
  "id": "ID_DU_CAMION_DEMANDE",
  "capaciteKg": 10000,
  "etat": "EN_SERVICE",
  "archive": false,
  "disponible": true
}
```

`disponible` est calculé par Camions à partir des réservations actives ; ce n’est pas une colonne à ajouter.
Les champs supplémentaires, par exemple `immatriculation`, sont acceptés.
Validation refusée si le camion est archivé, en maintenance, indisponible ou trop petit.
Les informations du camion viennent de son API ; le backend ne lit pas sa table.

Erreurs : `400` saisie/chargement incomplet ou camion inexistant, `404` chargement/ligne absent,
`409` état incompatible ou capacité insuffisante, `502` réponse Camions incomplète,
`503` échec d’appel Camions ou base indisponible. Chaque erreur contient `status`, `message`, `erreurs`.

## Où lire le code ?

| Dossier | Responsabilité |
|---|---|
| `controller` | Routes REST et réponses d’erreur |
| `service` | Règles métier et transactions |
| `repository` | SQL avec `JdbcTemplate` et `RowMapper` |
| `model` | Chargement, ligne et réponse Camions |
| `dto` | Corps de requêtes, résumé et erreur |
| `proxy` | Interface OpenFeign vers Camions |
| `config` | Activation des clients OpenFeign |

Pour suivre un exemple : `ChargementController.validate` → `ChargementService.validate` →
`CamionsProxy` et `ChargementRepository`. Les dépendances sont injectées par constructeur.
Repères de lecture : REST (chapitre 10), OpenFeign (11.1), JDBC (12), transactions (13).

`@Transactional` garde les lignes et le poids cohérents. Les écritures verrouillent le chargement
avec `SELECT ... FOR UPDATE` : une modification simultanée ne peut pas changer un chargement déjà validé.
Les identifiants sont des `String`, les poids des `BigDecimal` et les dates des `OffsetDateTime`.

## Dépendances

| Dépendance Maven | Usage |
|---|---|
| `spring-boot-starter-web` | REST / JSON |
| `spring-boot-starter-jdbc` | `JdbcTemplate`, transactions, pool de connexions |
| `spring-boot-starter-validation` | Validation des requêtes |
| `spring-cloud-starter-openfeign` | Consommation de Camions |
| `postgresql` | Driver PostgreSQL |
| `springdoc-openapi-starter-webmvc-ui` | Swagger / OpenAPI |
| `spring-boot-starter-test`, `h2` | Tests, sans accès Neon par défaut |

Versions : Spring Boot `3.5.16`, Spring Cloud `2025.0.3`, Springdoc `2.8.17`.
Ce module utilise JDBC ; les variables `SPRING_JPA_*` ne sont pas nécessaires.

## Vérifier et construire

```powershell
.\mvnw.cmd verify
```

Les tests utilisent H2 et un serveur HTTP Camions simulé ; le vrai client OpenFeign est exécuté.
Ils couvrent le parcours, les refus, le rollback et une modification concurrente.
H2 ne remplace pas une vérification sur PostgreSQL.

Un test PostgreSQL facultatif utilise `TEST_DATABASE_URL` (JDBC), `TEST_DATABASE_USERNAME`,
`TEST_DATABASE_PASSWORD` et `TEST_SQL_INIT_MODE=never`. Employer une base de test isolée
avec le schéma complet et un compte autorisé à créer les fixtures Camions.
Chaque test supprime seulement ses propres fixtures ; ne pas utiliser la base commune.

Construction Docker depuis ce dossier : `docker build -t ws-chargements .`
Puis `docker run --rm --env-file .env -p 8082:8082 ws-chargements` avec les variables privées.
Dans Docker Desktop, utiliser `CAMIONS_SERVICE_URL=http://host.docker.internal:8081`
si Camions tourne sur Windows.

[Retour au projet](../../README.md)

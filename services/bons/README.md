# Bons de livraison

Service Spring Boot du module de livraison, port `8083`. Il émet un bon depuis un chargement validé, conserve un snapshot des données et des lignes, puis enregistre une preuve de réception.

## Routes

Toutes les routes API commencent par `/api/v1`.

| Méthode et route | Rôle autorisé | Résultat |
|---|---|---|
| `POST /api/v1/bons` | `ADMIN`, `AGENT` | Émet un bon depuis un chargement `VALIDE`, réponse `201` |
| `GET /api/v1/bons/{id}` | `ADMIN`, `AGENT`, `LECTEUR`, `SERVICE_TRANSPORTS` | Retourne le détail et les lignes du snapshot |
| `POST /api/v1/bons/{id}/preuve-livraison` | `ADMIN`, `AGENT`, `SERVICE_TRANSPORTS` | Enregistre ou rejoue une preuve, réponse `200` |
| `GET /health` | public | Vérifie que le service répond |

Swagger UI : <http://localhost:8083/swagger-ui.html> ; OpenAPI : <http://localhost:8083/v3/api-docs>.

### Émettre un bon

```http
POST /api/v1/bons
Content-Type: application/json
Authorization: Bearer <JWT>

{"chargementId":"<id-du-chargement>"}
```

Bons appelle `GET /api/v1/chargements/{id}` avec le client technique `bons-service`. Un chargement absent donne `404`, un chargement non `VALIDE` donne `409`, un chargement valide incomplet donne `502`. Un seul bon est autorisé par chargement ; une nouvelle tentative de création donne `409`. Les champs d'adresse, destinataire, camion, poids et lignes sont copiés dans le bon et ne changent plus si le chargement source évolue.

### Lire un bon

```http
GET /api/v1/bons/<id>
Authorization: Bearer <JWT>
```

La réponse contient `id`, `numero`, `chargementId`, les adresses, `destination`, `poidsTotalKg`, `statut`, les champs de preuve, `createdAt` et `lignes`. Elle inclut les champs `id`, `statut`, `destination` et `poidsTotalKg` attendus par Transports.

### Confirmer la livraison

```http
POST /api/v1/bons/<id>/preuve-livraison
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "transportId": "<id-transport>",
  "receptionnaire": "Mohamed",
  "dateReception": "2026-11-17",
  "commentaire": "Marchandises reçues",
  "operationKey": "livraison:<id-transport>"
}
```

`dateReception` suit le format ISO `yyyy-MM-dd`. `commentaire` est facultatif (`null` accepté). Les limites sont : transport 64 caractères, réceptionnaire non blanc 160, commentaire 1 000 et clé 160. `operationKey` doit être exactement `livraison:` suivi de `transportId`.

Le bon est verrouillé pendant l'opération. La preuve et le statut `LIVRE` sont écrits dans la même transaction. Le premier appel stocke `transportId` dans `transport_preuve_id`, le réceptionnaire dans `recepteur`, le commentaire, et la date à minuit UTC dans `livre_le`. La réponse restitue la date civile UTC. Une répétition avec le même transport et contenu renvoie la preuve existante sans doublon (`200`). Un transport différent, un contenu différent, ou un transport qui ne référence pas ce bon donne `409`. Une saisie invalide donne `400`, un bon absent `404`.

Réponse de preuve :

```json
{
  "bonId": "<id-bon>",
  "transportId": "<id-transport>",
  "receptionnaire": "Mohamed",
  "dateReception": "2026-11-17",
  "commentaire": "Marchandises reçues",
  "statut": "LIVRE"
}
```

## Sécurité

Le serveur valide la signature, l'émetteur, la date d'expiration et l'audience `bons-api` du JWT. Les rôles viennent du claim Keycloak `roles`. Le client OAuth2 `bons-service` utilise `BONS_CLIENT_SECRET` en flux `client_credentials` pour lire Chargements avec l'audience `chargements-api`. Transports utilise son client technique `transports-service` pour lire et confirmer le bon. Aucun secret ne doit être placé dans le frontend ni commité.

## Lancement local

Prérequis : JDK 21, Maven, Keycloak en local. La configuration réelle est gardée dans le fichier privé `services/bons/.env`, ignoré par Git. Le lanceur importe ce fichier dans l'environnement du processus Spring :

```powershell
Copy-Item .env.example .env # uniquement lors d'une première configuration
# Renseigner les identifiants DB et BONS_CLIENT_SECRET dans .env, puis :
.\run-local.ps1
```

Ne pas commiter `.env`. Pour une utilisation partagée, privilégier un compte DB Bons à privilèges limités.

Le backend utilise seulement `bon_livraison` et `ligne_bon`, avec `spring.jpa.hibernate.ddl-auto=validate` et `spring.sql.init.mode=never`. Il ne crée ni ne modifie le schéma. Les migrations V1/V2 sont déjà appliquées sur la base partagée : ne pas les rejouer depuis ce module. Les comptes DB de module ne doivent pas recevoir de privilèges de migration.

## Contrat de données source

Chargements retourne `id`, `origine`, `destination`, `destinataireNom`, `destinataireTelephone`, `camionSouhaiteId`, `poidsTotalKg`, `statut` et `lignes` (`id`, `designation`, `quantite`, `poidsUnitaireKg`). Le source doit être `VALIDE`, posséder au moins une ligne et avoir des champs conformes aux tailles du schéma. Le backend Bons vérifie ces conditions avant d'écrire.

[Retour au projet](../../README.md)

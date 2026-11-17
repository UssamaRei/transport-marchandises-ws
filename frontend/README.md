# Frontend Transit

Design importé des maquettes Figma. Les cinq vues **Chargements** utilisent les vraies API :
brouillon, marchandises, poids total, validation et consultation. Connexion Keycloak avec PKCE,
renouvellement du JWT, déconnexion et accès en lecture seule pour LECTEUR.

## Lancer

Le lancement complet demande Java 21, Maven, Node.js 22.12+ et Docker Desktop.

Sur ce PC, depuis la racine du dépôt, ouvrir Docker Desktop puis exécuter :

```powershell
.\demarrer-local.ps1
```

Le script construit les deux backends, lance Camions, Chargements et Keycloak, puis le frontend.
Il utilise les fichiers privés `camions_app.env` et `chargements_app.env` du dossier
`Desktop/Neon-WS-Credentials`. Un autre dossier peut être fourni avec `-CredentialsDirectory`.
Ces backends utilisent la vraie base commune ; les données saisies dans l'interface y sont enregistrées.
Ctrl+C arrête Vite ; les backends restent dans Docker Desktop.

### Frontend seul

Prérequis : Node.js 22.12+ et npm. Démarrer d'abord Keycloak et les backends
[Chargements](../services/chargements/README.md) et [Camions](../services/flotte/README.md).

```powershell
cd frontend
npm install
npm run dev
```

Ouvrir <http://localhost:5173>. Le bouton **Se connecter** ouvre la page Keycloak.
Les comptes de développement sont décrits dans [auth/README.md](../auth/README.md).
Ne jamais mettre de mot de passe ou de secret de compte technique dans le frontend.

Par défaut : API Chargements `8082`, Camions `8081`, Keycloak `8090`.
Les routes `/api/v1/chargements` et `/api/v1/camions` passent par le proxy Vite en développement.
Copier `.env.example` vers `.env.local` uniquement pour changer les adresses.
Les redirections Keycloak doivent autoriser l'adresse du frontend, y compris la déconnexion.

Les autres écrans Figma sont conservés à **`/maquettes`**, avec une bannière indiquant qu'ils
utilisent des données de démonstration. Ils ne modifient pas la base et ne prouvent pas
qu'un backend est implémenté. La recherche de chargements filtre la page affichée.

## Vérifier et construire

```powershell
npm run typecheck
npm run build
```

Le résultat est dans `dist/`. En hébergement, servir `index.html` pour les routes de navigation
et acheminer `/api/v1/chargements` et `/api/v1/camions` vers leurs backends respectifs,
ou définir `VITE_API_BASE_URL` et configurer CORS côté backends. Le proxy Vite n'est pas un serveur de production.

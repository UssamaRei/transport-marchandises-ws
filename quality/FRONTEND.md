# Validation du frontend pour le DAT

## Tests de logique et de composants

Depuis le dossier `frontend` :

```powershell
npm.cmd ci
npm.cmd run test:coverage
npm.cmd run typecheck:tests
npm.cmd run build
```

Vitest et React Testing Library exécutent les composants React dans jsdom. Les
appels API et Keycloak sont simulés dans cette suite pour tester la logique du
frontend : session expirée, renouvellement, permissions, erreurs, filtres,
confirmation, capacité limite et conservation d'une preuve après coupure.
Ces tests ne prouvent pas à eux seuls le fonctionnement des backends.

Le rapport HTML Istanbul est dans `frontend/coverage/index.html`. Il inclut les
fichiers non testés de `src`. Seuls le point d'entrée `main.tsx`, les déclarations
de types et les maquettes de démonstration sont exclus. Les lignes du routage
`App.tsx` restent dans le dénominateur, même si elles ne sont pas exercées ici.
La couverture mesurée concerne cette suite ; les parcours navigateur ne sont
pas fusionnés artificiellement avec ce chiffre.

Configuration : `frontend/vitest.config.ts`. Tests : `frontend/tests/unit/`.
Référence de configuration : <https://vitest.dev/guide/coverage>.
Les versions des outils sont verrouillées dans `package-lock.json` ; jsdom 29
est utilisé pour rester compatible avec le Node 22.15 installé sur cette machine.

## Parcours dans le navigateur

La campagne DAT distingue les contrôles réels dans le navigateur de la suite
Vitest. Le frontend est lancé contre la pile Docker locale `ws-systeme-test`
et les ports 18081–18084, avec Keycloak sur 18090 ; aucune donnée Neon n'est modifiée.

Depuis la racine, démarrer la pile avec la commande de la campagne API, puis
depuis `frontend`, lancer Vite avec ces variables d'environnement :

```powershell
$env:VITE_KEYCLOAK_URL='http://localhost:18090'
$env:VITE_API_BASE_URL=''
$env:CAMIONS_PROXY_TARGET='http://127.0.0.1:18081'
$env:CHARGEMENTS_PROXY_TARGET='http://127.0.0.1:18082'
$env:BONS_PROXY_TARGET='http://127.0.0.1:18083'
$env:TRANSPORTS_PROXY_TARGET='http://127.0.0.1:18084'
npm.cmd run dev -- --host localhost
```

Comptes de test : AGENT et LECTEUR, avec les mots de passe du fichier privé
`.secrets/systeme-test.env`. Ne pas joindre ce fichier aux preuves du DAT.

Les scénarios Playwright déjà présents dans `frontend/tests/*.spec.ts` restent
disponibles via `npm run test:e2e`, avec `E2E_ISOLATED_STACK=1`. Leur présence ne
signifie pas qu'ils ont été relancés : consulter les preuves datées de chaque
campagne pour savoir quels contrôles ont effectivement été exécutés.

## Correction trouvée pendant la campagne

En affichage compact, le CSS masque le texte des liens principaux du menu.
Ces liens n'avaient alors plus de nom accessible, car leurs icônes sont masquées
aux lecteurs d'écran. Les cinq liens de `src/Shell.tsx` possèdent désormais un
`aria-label` explicite. Le test de régression masque les textes du menu et vérifie
que chaque lien conserve son nom. Aucun backend ni contrat API n'est modifié par
cette correction.

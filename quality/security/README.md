# Analyse de sécurité locale pour le DAT

Cette campagne vise seulement la pile Docker isolée `ws-systeme-test`.
Neon et les services de démonstration sur 8081–8084 sont exclus.

## Préparer et exécuter

Depuis la racine du dépôt, avec Docker Desktop, Java, Maven, Node et Python :

```powershell
python tests/integration/run.py --keep
npm.cmd --prefix frontend run build
docker pull ghcr.io/zaproxy/zaproxy:stable
docker pull nginx:stable-alpine
python quality/security/zap_runtime.py
python quality/security/check_access.py
python quality/security/run_zap.py
```

La première commande prépare un jeu fictif avec les vrais services et exécute
les tests d’intégration. Le scan sélectionne des identifiants existants dans
chaque API ; il exige au moins un camion, un chargement, un bon et un transport.

ZAP est contrôlé sur `127.0.0.1:18085`, avec une clé privée. Un Nginx sans port
publié sur l’hôte sert uniquement le bundle pour l’analyse statique HTTP.
Il ne représente pas une configuration de production retenue pour le projet.

Les secrets, JWT et rapports bruts restent dans `.secrets/stage4`. Ne pas joindre
ce dossier au DAT. Les rapports partageables retirent les jetons. Par défaut,
les preuves sont enregistrées dans `Desktop/WS/DAT_Transport/Preuves/Tache_4_Securite`.

## Ce qui est vérifié

- JWT absent, altéré ou non signé ; rôles AGENT et LECTEUR ; compte technique Bons.
- Origines CORS autorisées ou refusées, en-têtes métier et rejet de TRACE.
- Refus du grant password pour le client web utilisant PKCE.
- Analyse passive de réponses API authentifiées et des ressources frontend.
- Cinq scans GET limités aux paramètres de requête, avec trois règles :
  traversée de répertoires, XSS réfléchi et injection SQL.

Le JWT LECTEUR est renouvelé pendant la campagne. Le script HTTP sender ne
l’ajoute qu’aux quatre hôtes API et ports attendus. L’analyse ne désactive pas
l’authentification des backends.

## Limites et résultats

L’intensité est LOW, avec deux threads et une pause de 40 ms. Les écritures,
corps JSON, segments de chemin, TLS et attaques DOM dans un navigateur ne sont
pas examinés par cette campagne. Les résultats ne constituent pas un pentest
exhaustif. Consulter le protocole JSON et les exports datés avant de reprendre
des nombres dans le DAT.

Un caractère nul dans `camionId` provoquait HTTP 500 dans Transports. Le service
rejette désormais les caractères de contrôle avant le repository et renvoie
HTTP 400. Les preuves avant/après et les tests ajoutés sont conservés dans le
dossier de la tâche 4. Les alertes de dépendances Java de la tâche 1 restent
à traiter séparément.

Arrêter les deux outils de scan après utilisation :

```powershell
docker stop ws-dat-zap ws-dat-frontend
```

Références : [images Docker officielles ZAP](https://www.zaproxy.org/docs/docker/about/),
[API ZAP](https://www.zaproxy.org/docs/api/),
[règles de scan actives](https://www.zaproxy.org/docs/desktop/addons/active-scan-rules/).

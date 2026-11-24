# Validation API et performance du DAT

Cette campagne utilise les quatre vrais services, Keycloak et une base PostgreSQL
locale jetable. Elle ne doit pas être dirigée vers Neon ou les données de démonstration.
La collection interdit les hôtes et ports différents de `127.0.0.1:18081–18084`.

## Préparer la pile isolée

Depuis la racine du dépôt, avec Docker Desktop, Java 21, Maven, Node et Python :

```powershell
python tests/integration/run.py --keep
npm.cmd install --prefix .tools/api-quality-tools --save-exact newman@6.2.3
```

La première commande construit les services et exécute les tests d'intégration ;
`--keep` conserve ensuite la pile pour les tests API. Les secrets sont générés dans
`.secrets/systeme-test.env`, ignoré par Git. Ne pas partager ce dossier.

Installer Apache JMeter 5.6.3 depuis l'archive officielle :
<https://dlcdn.apache.org/jmeter/binaries/apache-jmeter-5.6.3.zip>.
Comparer son SHA-512 au fichier officiel
<https://www.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.zip.sha512>,
puis extraire pour obtenir `.tools/apache-jmeter-5.6.3/bin/ApacheJMeter.jar`.

## Exécuter

```powershell
python quality/api/run_stage2.py all --output .tools/api-performance-nouvelle-campagne
```

Les actions `postman` et `performance` peuvent aussi être lancées séparément.
Exécuter `postman` d'abord : il crée les quatre ressources utilisées pour les lectures.
Choisir un nouveau dossier pour chaque campagne ; JMeter refuse d'écraser un niveau existant.
Ne pas exécuter simultanément les tests d'intégration : certains arrêtent volontairement
des services pour vérifier leur reprise.

## Preuves produites

- Newman : 43 requêtes et 75 assertions, export JSON, JUnit XML et journal.
- JMeter : trois niveaux de 1, 5 et 10 utilisateurs ; rapports HTML natifs et mesures JTL.
- Les exports partageables Newman retirent les jetons. Les originaux privés,
  environnements et propriétés JMeter restent dans `.secrets/stage2`.

La collection peut aussi être importée dans Postman avec l'environnement exemple
fourni, dont les valeurs sensibles sont vides. Le runner automatise l'obtention de
JWT locaux par PKCE et compte technique, sans utiliser Postman Cloud.

## Protocole et limites

Chaque utilisateur répète 20 fois quatre GET authentifiés, avec une pause de 200 ms
avant chaque appel, une montée en charge de 1/3/5 secondes selon le niveau, et des
délais de connexion/réponse de 3/10 secondes. Chaque réponse doit avoir HTTP 200 et
l'identifiant JSON attendu. Un JWT frais est obtenu avant chaque niveau, hors mesure.
Une prélecture vérifie les ressources ; aucune phase d'échauffement en charge dédiée
n'est isolée. Les quatre mêmes ressources sont relues, sans écriture pendant la charge.

Le générateur et les services partagent le même PC. Il s'agit d'une charge légère,
pas d'une recherche de saturation ni d'un engagement de performance en production.
Les seuils Apdex natifs de JMeter ne sont pas un SLA du projet.

La liste paginée Camions utilise `items`, tandis que Bons et Transports utilisent
`content` : les assertions respectent les contrats réellement exposés.

À la fin, arrêter uniquement la pile isolée :

```powershell
docker compose -p ws-systeme-test --env-file .secrets/systeme-test.env -f tests/integration/compose.yaml stop
```

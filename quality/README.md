# Qualité du projet — SINA Oussama

Depuis la racine du dépôt, avec Java 21, Maven, Python 3 et Node.js installés :

```powershell
python quality/run.py checkstyle
python quality/run.py auth
python quality/run.py snyk
```

Les rapports HTML, les sorties originales et les bilans JSON sont dans `.tools/qualite/`.
Pour choisir un autre dossier : ajouter `--output "C:/chemin/rapports"`.
Le premier appel Snyk installe localement les versions fixées des outils dans `.tools/quality-tools/`.
La connexion Snyk se fait dans le navigateur ; aucun jeton ne doit être ajouté au dépôt.

## Périmètre

- **Checkstyle 10.26.1 / plugin Maven 3.6.0 :** les sources Java de production des quatre services, avec la même configuration `quality/checkstyle.xml`. Les tests, les ressources et les fichiers générés sont exclus. Il s'agit de règles propres au projet, pas d'une certification Google ou Sun.
- **Snyk CLI 1.1309.0 :** les dépendances Maven des quatre services et celles du frontend npm, avec `--dev`. Le scan concerne les dépendances déclarées et leurs dépendances transitives, pas le code applicatif ni les images Docker. Aucun `snyk monitor` n'est exécuté.
- **Rapports Snyk :** `snyk-to-html` 3.7.12 convertit les résultats JSON originaux. Les comptes du bilan sont dédupliqués par identifiant Snyk dans chaque projet ; ne pas additionner les projets pour compter les failles uniques du système.

Un scan terminé peut signaler des vulnérabilités : cela n'est pas un échec d'authentification.
Le script retourne un code non nul si une analyse échoue **ou si des alertes subsistent**.
Le contrôle d'un seul service reste possible : `mvn -f services/chargements/pom.xml -Pqualite validate`.
Les analyses ne démarrent aucun backend et n'accèdent pas à la base métier partagée.

## Tests Java et couverture

Le profil `qualite` ajoute JaCoCo 0.8.16 et le rapport JUnit Surefire 3.5.6.
Depuis le dossier d'un service :

```powershell
mvn -Pqualite test surefire-report:report-only
```

La campagne exécute les tests une seule fois. JaCoCo produit `target/site/jacoco/index.html`
et Surefire produit `target/reports/surefire.html`. La couverture porte sur les classes
de production du module, sans exclusion ajoutée pour augmenter le pourcentage.
Les mesures de lignes et de branches doivent être distinguées ; aucun seuil minimal
de couverture n'est imposé sans exigence explicite du projet.

Les tests de réservation Camions demandent un PostgreSQL **neuf et isolé** : la base
doit s'appeler `flotte_test` et être joignable sur localhost. Une base déjà initialisée
fait échouer leur script de création des tables. Utiliser Testcontainers avec un Docker
compatible, ou renseigner `FLOTTE_TEST_DATABASE_URL`, `FLOTTE_TEST_DATABASE_USERNAME`
et `FLOTTE_TEST_DATABASE_PASSWORD` vers une instance locale jetable. Ne jamais utiliser Neon.
Chargements utilise H2 et des serveurs HTTP de test ; les autres suites utilisent
leurs configurations et doublures propres.

## Documentation des outils

- [Checkstyle avec Maven](https://maven.apache.org/plugins/maven-checkstyle-plugin/usage.html)
- [Commande Snyk test](https://docs.snyk.io/developer-tools/snyk-cli/commands/test)
- [Authentification Snyk](https://docs.snyk.io/developer-tools/snyk-cli/authenticate-to-use-the-cli)
- [Conversion du rapport Snyk](https://docs.snyk.io/developer-tools/snyk-cli/scan-and-maintain-projects-using-the-cli/cli-tools/snyk-to-html)
- [Propriétés de versions Spring Boot](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/properties.html)

# Présentation d'avancement — 8 minutes

Le message de l'enseignant demande **10 minutes maximum**, limitées aux axes fonctionnel, technique et organisationnel. La séance annoncée est le jeudi 29 octobre 2026, de 8 h 30 à 12 h 15.

## 1. Fonctionnel — 2 minutes

« Notre application organise le transport de marchandises. Elle permet de gérer les camions, préparer les chargements, créer les bons de livraison et suivre les transports. »

Exemple : 8 tonnes de marchandises doivent être livrées de Rabat à Casablanca. L'opérateur prépare le chargement, crée le document, affecte un camion adapté et suit sa livraison.

## 2. Technique — 4 minutes

Quatre backends, un par étudiant ; communication REST/JSON ; une base logique par module ; Docker et Compose pour réunir l'ensemble. Les frameworks doivent être complétés avec les **vrais choix de l'équipe**.

Chaque étudiant explique :

> « Mon backend gère … Il fournit … au module … Il appelle le module … pour … »

Afficher [l'architecture](02-architecture.md) et donner un exemple d'appel, sans entrer dans les routes ou les schémas SQL.

## 3. Organisationnel — 2 minutes

Présenter les noms et modules attribués, le chef de projet, le dépôt GitHub commun, le suivi des tâches et la manière de tester les échanges. Distinguer le travail déjà vérifié de ce qui est prévu.

## Avancement honnête au démarrage de ce dépôt

- Le thème et une proposition de découpage sont documentés.
- Les contrats, schémas et diagrammes fournissent une base commune à valider.
- Les noms des étudiants, frameworks et l'état réel du code restent à compléter.
- Les backends métier ne sont pas implémentés dans cette version du dépôt.

Le chef de projet doit expliquer pourquoi chaque module a son rôle, pourquoi REST permet les échanges entre technologies et comment l'équipe coordonne les contrats.

[Version simple en trois pages (PDF)](presentation/Projet_Transport_Version_Simple.pdf)

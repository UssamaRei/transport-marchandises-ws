# Diagrammes du projet

Les documents Markdown contiennent des blocs Mermaid visibles directement dans GitHub. Ce dossier fournit aussi les sources `.mmd` modifiables et des aperçus SVG.

| Diagramme | Mermaid | Image | Explication |
|---|---|---|---|
| Parcours fonctionnel | [Source](01-parcours.mmd) | [Apercu SVG](rendered/01-parcours.svg) | [Document](../../README.md) |
| Architecture generale | [Source](02-architecture.mmd) | [Apercu SVG](rendered/02-architecture.svg) | [Document](../../docs/02-architecture.md) |
| Classes metier communes | [Source](03-classes-globales.mmd) | [Apercu SVG](rendered/03-classes-globales.svg) | [Document](../../docs/03-modele-donnees.md) |
| Sequence complete de livraison | [Source](04-sequence.mmd) | [Apercu SVG](rendered/04-sequence.svg) | [Document](../../docs/04-communication-api.md) |
| Classes Flotte | [Source](05-classes-flotte.mmd) | [Apercu SVG](rendered/05-classes-flotte.svg) | [Document](../../services/flotte/README.md) |
| Classes Chargements | [Source](06-classes-chargements.mmd) | [Apercu SVG](rendered/06-classes-chargements.svg) | [Document](../../services/chargements/README.md) |
| Classes Bons | [Source](07-classes-bons.mmd) | [Apercu SVG](rendered/07-classes-bons.svg) | [Document](../../services/bons/README.md) |
| Classes Transports | [Source](08-classes-transports.mmd) | [Apercu SVG](rendered/08-classes-transports.svg) | [Document](../../services/transports/README.md) |
| Etats du transport | [Source](09-etats-transports.mmd) | [Apercu SVG](rendered/09-etats-transports.svg) | [Document](../../services/transports/README.md) |
| Base relationnelle Flotte | [Source](10-erd-flotte.mmd) | [Apercu SVG](rendered/10-erd-flotte.svg) | [Document](../../docs/diagrams/modeles-relationnels.md) |
| Base relationnelle Chargements | [Source](11-erd-chargements.mmd) | [Apercu SVG](rendered/11-erd-chargements.svg) | [Document](../../docs/diagrams/modeles-relationnels.md) |
| Base relationnelle Bons | [Source](12-erd-bons.mmd) | [Apercu SVG](rendered/12-erd-bons.svg) | [Document](../../docs/diagrams/modeles-relationnels.md) |
| Base relationnelle Transports | [Source](13-erd-transports.mmd) | [Apercu SVG](rendered/13-erd-transports.svg) | [Document](../../docs/diagrams/modeles-relationnels.md) |

Le diagramme de classes global est conceptuel. Les quatre modèles relationnels restent séparés : une référence à un autre service ne devient pas une clé étrangère SQL commune.

Pour modifier un schéma, mettre à jour son bloc dans le document puis la source `.mmd` correspondante. `manifest.json` indique la correspondance ; le validateur repère les divergences. Regénérer l'aperçu SVG avec un outil Mermaid compatible après modification. Les images sont des exports, les blocs Mermaid et `.mmd` restent éditables.

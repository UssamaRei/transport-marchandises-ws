# Connexion Transit

Thème Keycloak `transit`, adapté au frontend : fond clair, carte blanche, logo,
couleurs et polices identiques. Les polices sont servies localement, sans requête
Google Fonts pendant la connexion. Le formulaire et ses scripts restent ceux de
Keycloak ; seul son habillage change.

Le Compose monte ce dossier dans `/opt/keycloak/themes/transit`. Le realm exporté
sélectionne le thème et le français pour les nouvelles installations. Sur un realm
déjà importé : **Realm Settings → Themes → Login theme → transit** ; puis choisir
le français dans les paramètres de localisation. Le réglage est conservé dans le
volume Keycloak et ne nécessite pas de recréer les utilisateurs.

Les caches de thème sont désactivés dans le Compose de développement. Pour une
mise à jour Keycloak, comparer `login/template.ftl` au layout fourni dans la
nouvelle version, puis vérifier connexion, erreur de saisie et déconnexion.

Sources : [personnalisation Keycloak](https://www.keycloak.org/ui-customization/themes).
Le layout dérive de `theme/base/login/template.ftl` de Keycloak **26.8.2**, sous
licence Apache-2.0 (fichier `LICENSE-Keycloak.txt`). DM Sans et Manrope proviennent
de Google Fonts, sous licences OFL conservées dans `login/resources/fonts/`.

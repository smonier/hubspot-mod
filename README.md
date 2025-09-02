# Module HubSpot pour Jahia

Un module Jahia permettant l'intégration avec HubSpot pour la gestion des formulaires et des contacts.

## Fonctionnalités

- Composant de formulaire HubSpot (`hubnt_hubspotFormComponent`)
- Formulaire de génération de leads HubSpot (`hubnt_hubspotLeadForm`)
- Intégration avec l'API HubSpot pour les contacts et formulaires
- Support multilingue (français, allemand, espagnol, anglais)
- Interface utilisateur avec PrimeReact

## Configuration

Le module utilise un fichier de configuration situé dans `src/main/resources/META-INF/configurations/org.jahia.se.modules.hubspot.credentials.cfg`.

### Paramètres de configuration

- `hubspot.apiEndPoint` : Point d'accès API pour les contacts
- `hubspot.apiSchema` : Schéma de l'API (https)
- `hubspot.apiUrl` : URL de base de l'API HubSpot
- `hubspot.forms.apiEndPoint` : Point d'accès API pour les formulaires
- `hubspot.portalId` : ID du portail HubSpot
- `hubspot.secret` : Clé secrète HubSpot
- `hubspot.token` : Token d'authentification HubSpot

## Structure du projet
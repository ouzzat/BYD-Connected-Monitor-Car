# Fiche Projet — Super-application BYD tout-en-un

*Marchés cibles : France & Maroc — Application tierce indépendante (non éditée par BYD)*

---

## 1. Nom de l'application

### **ByDrive**

**Justification :** Le nom associe directement l'identité de la marque véhicule (**BY**D) au geste quotidien de l'utilisateur (**Drive**), sans reprendre le logo ni prétendre être un produit officiel BYD — une précaution nécessaire pour une application tierce utilisant des API constructeur sous licence. Il se prononce et se lit à l'identique en français et en arabe dialectal marocain (transcription phonétique simple : بايدرايف), ce qui facilite une identité de marque unique sur les deux marchés sans adaptation. Le nom est court (7 lettres), disponible en tant que nom d'app store, et évoque à la fois la mobilité électrique et un compagnon de conduite actif plutôt qu'un simple tableau de bord passif.

---

## 2. Tagline

> **« Toute votre BYD, dans le creux de la main. »**

*(Variante marché marocain, bilingue : « Votre BYD, connectée à votre quotidien. » / كل ما يخص سيارتك BYD، بين يديك.)*

---

## 3. Description générale

ByDrive est le compagnon intelligent des propriétaires de véhicules BYD électriques et hybrides en France et au Maroc, conçu pour transformer une simple télécommande constructeur en un véritable copilote quotidien. L'application centralise en un point unique tout ce que le conducteur doit gérer autour de sa voiture : recharge et autonomie, entretien, navigation, sécurité, économies d'énergie et vie communautaire — là où les applications constructeur se limitent souvent au strict contrôle du véhicule. Elle s'appuie sur les API officielles BYD pour les données et commandes véhicule, enrichies par une couche d'intelligence artificielle qui transforme la donnée brute en recommandations actionnables (quand recharger, quand entretenir, comment économiser). Pensée dès l'origine pour deux marchés aux réalités très différentes — infrastructure de recharge mature en France, écosystème naissant au Maroc — ByDrive s'adapte localement en langue, en contenu et en fonctionnalités plutôt que d'imposer une expérience unique. L'ambition est de devenir le hub quotidien incontournable de la communauté BYD francophone et maroco-francophone, au même titre qu'une super-app dans d'autres verticales (santé, banque, mobilité).

---

## 4. Utilisateurs cibles

### Profil socio-démographique

| Segment | France | Maroc |
|---|---|---|
| **Âge dominant** | 35-55 ans | 30-50 ans |
| **CSP** | Cadres, professions libérales, primo-accédants VE | Cadres supérieurs, entrepreneurs, expatriés/MRE (Marocains Résidant à l'Étranger) |
| **Zone géographique** | Grandes agglomérations et périurbain (Île-de-France, Lyon, Bordeaux, Nice) | Casablanca, Rabat, Marrakech, Tanger |
| **Rapport au véhicule** | Deuxième voiture électrique ou remplacement thermique, sensibilité écologique affirmée | Achat premium/statutaire, véhicule principal, sensibilité au rapport qualité-prix face aux marques allemandes |
| **Multilinguisme** | Français quasi exclusif | Arabe dialectal (darija) à l'oral, français à l'écrit dominant, anglais en progression |

### Segments comportementaux

1. **Le pragmatique urbain (FR)** — trajets courts, recharge à domicile ou en copropriété, cherche à optimiser son coût kWh et anticiper les bornes publiques pour les longs trajets (vacances).
2. **Le pionnier électrique (FR & MA)** — early adopter, technophile, moteur de la communauté, ambassadeur naturel de la marque, friand de data et de benchmarks de conduite.
3. **Le conducteur statutaire (MA)** — véhicule comme signe de réussite, attentif à l'entretien préventif et à la revente, moins technophile mais sensible à la simplicité et à la sécurité (anti-vol, tracking).
4. **Le flotte / pro (FR & MA)** — indépendants, PME, VTC, gestion de plusieurs véhicules BYD, besoin de reporting de coûts et d'autonomie fiable.
5. **La famille multi-conducteurs (FR & MA)** — partage du véhicule entre conjoints/enfants, besoin de contrôle parental, géolocalisation et alertes de sécurité.

### Besoins non satisfaits aujourd'hui
- Absence d'écosystème unifié : bornes de recharge, entretien, communauté et sécurité sont aujourd'hui dispersés entre l'app constructeur, Chargemap/PlugShare, le carnet d'entretien papier, et des groupes Facebook/WhatsApp informels.
- Manque cruel d'informations fiables et localisées en darija/français sur l'entretien et la recharge BYD au Maroc, marché encore jeune pour la marque.
- Absence de gamification ou de valorisation des comportements vertueux (éco-conduite, recharge intelligente) dans les apps constructeur existantes.

---

## 5. Modules & Fonctionnalités clés

### 5.1 Recharge & Autonomie

**Problème résolu :** L'anxiété d'autonomie et la fragmentation des réseaux de recharge (surtout au Maroc où le réseau public est embryonnaire) rendent la planification de trajet stressante et le coût de recharge peu lisible.

**Fonctionnalités principales :**
1. Cartographie en temps réel des bornes de recharge les plus proches (publiques, semi-publiques, réseaux partenaires), avec disponibilité, puissance, type de connecteur et prix au kWh.
2. Planificateur d'itinéraire longue distance avec arrêts de recharge optimisés selon l'autonomie réelle du véhicule, la météo et le style de conduite.
3. Pilotage de la recharge à domicile : démarrage/arrêt à distance, programmation sur heures creuses, suivi de la consommation et du coût par session.
4. Historique et prévision d'autonomie personnalisée (courbe de dégradation batterie, impact du froid/chaud, du style de conduite).
5. Réservation de borne (là où l'API du partenaire le permet) et paiement intégré multi-réseaux.

**Intégration technologique :**
- **IA** : modèle prédictif d'autonomie restante ajusté en continu (météo, dénivelé, historique de conduite, charge auxiliaire type climatisation) — bien plus précis que l'estimation statique du tableau de bord.
- **IoT** : communication directe avec le boîtier de charge domestique (si compatible OCPP) pour piloter la recharge et remonter la consommation en temps réel.
- **Smart home** : intégration avec les compteurs communicants et les box domotiques (ex. déclenchement automatique de la recharge si les panneaux solaires du foyer produisent un surplus).
- **Gamification** : badges « Éco-recharge » pour les sessions effectuées en heures creuses ou à partir d'énergie verte, classement mensuel du coût au km le plus bas.
- **Assistant vocal** : commande vocale « Démarre la recharge » ou « Combien de kilomètres il me reste ? » en conduite ou à domicile.

---

### 5.2 Entretien & Diagnostic

**Problème résolu :** Les propriétaires découvrent souvent une anomalie mécanique ou une échéance d'entretien trop tard, faute de suivi centralisé, et le réseau de service après-vente BYD est encore en construction en France et au Maroc — l'utilisateur a besoin d'anticiper plutôt que de subir.

**Fonctionnalités principales :**
1. Tableau de bord de diagnostic en temps réel (codes défaut, usure des plaquettes, niveau de liquide de refroidissement batterie, santé de la batterie haute tension).
2. Carnet d'entretien numérique avec rappels intelligents basés sur le kilométrage réel et les habitudes de conduite (pas seulement un calendrier générique).
3. Prise de rendez-vous en ligne avec le réseau d'ateliers agréés BYD ou partenaires certifiés les plus proches, avec devis estimatif.
4. Diagnostic préventif par IA : détection de dérives anormales (consommation, vibrations signalées, chute de tension) avant l'apparition d'un code défaut classique.
5. Suivi de la garantie batterie et du dossier technique du véhicule (utile pour la revente).

**Intégration technologique :**
- **IA** : détection d'anomalies par analyse de séries temporelles sur les données télémétriques du véhicule, avec score de confiance et recommandation d'action (« surveiller » vs « prendre rendez-vous sous 7 jours »).
- **IoT** : remontée continue des capteurs véhicule (pression pneus, température batterie, usure freinage régénératif) via la télématique embarquée.
- **Gamification** : « Score Entretien » valorisant la régularité (comme un score de crédit), débloquant des avantages chez les partenaires ateliers.
- **Assistant vocal** : rapport vocal quotidien type « Votre véhicule est en bon état, la prochaine révision est dans 1 200 km ».

---

### 5.3 Navigation intelligente

**Problème résolu :** La navigation généraliste (Google Maps, Waze) ignore les contraintes propres au véhicule électrique (autonomie, bornes, poids) et les données routières locales sont parfois peu fiables au Maroc (chantiers, contrôles, état des routes secondaires).

**Fonctionnalités principales :**
1. Navigation « EV-aware » intégrant automatiquement les arrêts de recharge nécessaires selon l'autonomie réelle et le trafic en temps réel.
2. Alertes communautaires locales (radars, contrôles, nids-de-poule, obstacles) alimentées par les utilisateurs ByDrive, façon Waze spécialisé BYD.
3. Synchronisation destination-véhicule : envoi d'un itinéraire depuis le smartphone vers l'écran DiLink avant même de monter dans la voiture.
4. Mode « Trajet longue distance France ↔ Maroc/traversée » avec préparation logistique (ferry, douane, bornes disponibles sur le trajet ibérique).

**Intégration technologique :**
- **IA** : recalcul dynamique d'itinéraire tenant compte de l'autonomie prédictive du module Recharge, du trafic et des habitudes de l'utilisateur (heures de départ favorites, préférence autoroute/route).
- **IoT** : synchronisation bidirectionnelle avec le système embarqué DiLink pour pousser l'itinéraire et récupérer la position en temps réel.
- **Gamification** : contribution communautaire récompensée par des points « Éclaireur » pour les signalements validés par d'autres membres.
- **Assistant vocal** : guidage vocal mains-libres, ajout de destination par la voix en français ou en darija.

---

### 5.4 Communauté de conducteurs BYD

**Problème résolu :** Les échanges d'expérience (astuces, retours d'achat, entraide en cas de panne, ventes d'accessoires) se font aujourd'hui de façon dispersée et non structurée sur des groupes Facebook ou WhatsApp génériques, sans lien avec les données réelles du véhicule.

**Fonctionnalités principales :**
1. Fil communautaire par ville/région avec groupes dédiés (ex. « BYD Casablanca », « BYD Île-de-France »), modéré et vérifié par preuve de possession du véhicule.
2. Système d'entraide géolocalisée en cas de panne, de recharge d'urgence ou de question technique (mise en relation avec des membres proches).
3. Classements et défis communautaires (éco-conduite, kilométrage électrique cumulé, parrainage) avec récompenses partenaires.
4. Marketplace communautaire (accessoires, covoiturage entre membres, avis vérifiés sur ateliers et bornes).
5. Événements physiques organisés depuis l'app (rassemblements, road trips, sessions découverte pour primo-acquéreurs).

**Intégration technologique :**
- **IA** : mise en relation intelligente (matching) entre membres selon la proximité géographique, le modèle de véhicule et l'historique d'entraide.
- **Gamification** : système de badges et niveaux (« Ambassadeur », « Mentor recharge », « Éclaireur route ») avec statut visible dans le profil, moteur central de rétention communautaire.
- **Assistant vocal** : partage vocal rapide d'un signalement ou d'une demande d'aide sans manipulation de l'écran en conduite.

---

### 5.5 Économies d'énergie

**Problème résolu :** Les conducteurs n'ont pas de visibilité claire sur l'impact réel de leur style de conduite ou de leurs habitudes de recharge sur leur facture d'électricité et l'usure du véhicule, ce qui limite l'adoption de comportements plus économes.

**Fonctionnalités principales :**
1. Score d'éco-conduite en temps réel (accélérations, freinage régénératif, vitesse) avec conseils personnalisés post-trajet.
2. Comparateur de coût réel électrique vs thermique équivalent, actualisé selon les tarifs locaux (EDF/fournisseurs FR, ONEE/Lydec au Maroc).
3. Recommandations d'optimisation tarifaire (heures creuses, abonnement électrique le plus adapté au profil de recharge).
4. Simulateur d'impact carbone cumulé et rapport mensuel « Économies réalisées » (argent + CO₂).

**Intégration technologique :**
- **IA** : modèle de scoring comportemental qui apprend le profil du conducteur et personnalise les conseils (pas de conseils génériques identiques pour tous).
- **Smart home** : couplage avec les compteurs intelligents et les installations photovoltaïques domestiques pour maximiser l'autoconsommation lors de la recharge.
- **Gamification** : défis mensuels « -10% de consommation ce mois-ci » avec récompenses (avoirs partenaires énergie, bornes gratuites).
- **Assistant vocal** : bilan vocal de fin de trajet façon coach personnel (« Ce trajet vous a coûté 1,20 € contre 4,50 € en thermique »).

---

### 5.6 Sécurité du véhicule

**Problème résolu :** Le vol de véhicule et l'usage non autorisé restent des préoccupations fortes, en particulier au Maroc où les dispositifs anti-vol connectés sont moins répandus, tandis qu'en France les familles cherchent des outils de contrôle parental sur les jeunes conducteurs.

**Fonctionnalités principales :**
1. Verrouillage/déverrouillage à distance et alertes en temps réel (effraction, déplacement non autorisé, choc détecté).
2. Geofencing (zones autorisées) avec notification immédiate en cas de sortie de périmètre — utile pour le contrôle parental ou la surveillance flotte.
3. Mode Valet/Prêt de véhicule limitant vitesse, accès à certaines fonctions et traçant le trajet lors d'un prêt à un tiers (voiturier, jeune conducteur, mécanicien).
4. Historique de localisation et rejeu de trajet, utile en cas de vol pour transmission aux autorités.
5. Alerte caméra de surveillance véhicule (si équipé) et notification en cas de tentative d'intrusion à l'arrêt.

**Intégration technologique :**
- **IA** : détection d'anomalies comportementales (déplacement à une heure inhabituelle, zone inhabituelle) déclenchant une alerte proactive avant même une effraction confirmée.
- **IoT** : capteurs de choc, d'inclinaison et de mouvement embarqués, remontée en temps réel via la connectivité véhicule.
- **Smart home** : intégration avec les systèmes d'alarme domestique et de vidéosurveillance du domicile pour une vision unifiée sécurité maison + véhicule (pertinent pour le stationnement en copropriété ou garage privé).
- **Gamification** : indicateur « Score de sécurité » valorisant l'activation des dispositifs (geofencing actif, mode valet utilisé) pouvant être partagé avec l'assureur pour des réductions de prime.
- **Assistant vocal** : commande vocale d'urgence (« Verrouille ma voiture », « Où est ma voiture ? ») et confirmation vocale des alertes reçues.

---

## 6. Architecture technique

### 6.1 Principes directeurs
- Application **tierce et indépendante**, sans branding officiel BYD, respectant les conditions d'usage des API constructeur.
- Architecture **API-first**, agnostique du marché, avec une couche d'adaptation locale (langue, réglementation, partenaires).
- Approche **hybride de collecte de données** : utilisation prioritaire de l'API officielle BYD (Open Platform / API constructeur régionale) là où elle est disponible et documentée ; complément par un **agent embarqué autonome** (sur le modèle de l'agent DiLink de ce dépôt, publiant la télémétrie en MQTT) sur les marchés ou modèles où l'API officielle n'expose pas encore toutes les données nécessaires — pertinent notamment au lancement sur le marché marocain.

### 6.2 Intégration API BYD
- **Authentification** : OAuth2 délégué via le compte BYD du propriétaire (consentement explicite RGPD/loi marocaine 09-08 sur les données personnelles).
- **Domaines de données consommés** : état de charge, localisation, verrouillage/déverrouillage, climatisation à distance, diagnostics véhicule, historique de trajets.
- **Fallback télémétrie** : lorsque l'API constructeur ne couvre pas un besoin (ex. données batterie fines), un module embarqué en lecture seule (basé sur l'architecture existante de ce dépôt : service Android de premier plan, MQTT 3.1.1 en TLS, chiffrement Android Keystore) publie les données brutes vers le backend ByDrive, strictement en complément et jamais en substitution des commandes officielles.

### 6.3 Stack recommandée

| Couche | Choix recommandé | Justification |
|---|---|---|
| **Mobile** | Flutter (iOS/Android unifié) | Un seul code base pour les deux marchés, rendu natif performant, support RTL natif pour l'arabe |
| **Backend applicatif** | Node.js/NestJS ou Go (microservices) | Scalabilité, écosystème riche pour intégrations tierces (paiement, cartographie) |
| **Bus de messages IoT** | Broker MQTT managé (EMQX / AWS IoT Core) | Continuité directe avec l'agent embarqué existant, TLS obligatoire |
| **Base télémétrie** | Base de séries temporelles (TimescaleDB/InfluxDB) | Adaptée aux flux haute fréquence (position, batterie, conduite) |
| **Base applicative** | PostgreSQL | Données utilisateur, communauté, transactions |
| **Couche IA/ML** | Modèles hébergés (prédiction autonomie, scoring éco-conduite, détection d'anomalies) + LLM pour l'assistant conversationnel | Nécessite ré-entraînement continu sur données locales (climat, relief marocain/français) |
| **Cartographie** | Fournisseur cartographique avec bonne couverture Maroc (ex. Mapbox/HERE) + couche bornes issue de partenaires locaux | Google Maps seul insuffisant sur le maillage secondaire marocain |
| **Assistant vocal** | Intégration Siri Shortcuts / Google Assistant Actions + moteur NLU propriétaire pour le darija | Les assistants génériques ne couvrent pas le darija de façon fiable |
| **Smart home** | Passerelle Matter / API Google Home & Apple Home | Standard ouvert facilitant l'intégration multi-écosystèmes |
| **Sécurité** | Chiffrement bout en bout des commandes véhicule, authentification forte (biométrie + 2FA), conformité RGPD (FR) et loi 09-08 (MA) | Exigence non négociable pour une app à commandes véhicule |

---

## 7. Expérience utilisateur (UX) différenciante

- **Un hub, pas un tableau de bord** : contrairement aux apps constructeur (BYD App officielle) centrées sur la commande brute, ByDrive organise l'expérience autour des *moments de vie* du conducteur (« je pars en trajet », « je rentre chez moi », « mon véhicule a besoin d'attention ») plutôt que par fonction technique.
- **IA contextuelle proactive** : l'application pousse l'information utile au bon moment (« Rechargez maintenant, tarif heures creuses dans 20 min ») plutôt que d'attendre que l'utilisateur aille la chercher — rupture avec la logique purement réactive des apps existantes.
- **Double localisation réelle, pas une traduction** : contenus, partenaires, unités et références culturelles adaptés au Maroc (darija, dirham, réseau ONEE) et à la France (euro, RTE/EDF), avec bascule automatique selon la localisation SIM/GPS.
- **Gamification qui a du sens** : les récompenses (badges, scores) sont adossées à de vrais bénéfices tangibles (réductions bornes, avantages assurance, primes partenaires) et non de la gamification cosmétique.
- **Continuité multi-écran** : démarrage d'une action sur mobile, poursuite sur l'écran DiLink du véhicule, et visibilité depuis une montre connectée ou l'assistant vocal domestique — une seule expérience, plusieurs points d'entrée.
- **Confiance et transparence des données** : tableau de bord de confidentialité clair indiquant précisément quelles données sont partagées avec BYD, avec les partenaires ou au sein de la communauté, essentiel pour rassurer sur un produit tiers.

---

## 8. Modèle économique

| Levier | Détail |
|---|---|
| **Freemium** | Fonctions de base gratuites (état véhicule, verrouillage, alertes basiques, communauté) pour maximiser l'adoption et l'effet réseau communautaire. |
| **Abonnement Premium (ByDrive+)** | Navigation EV-aware avancée, IA prédictive d'autonomie et de diagnostic, mode Valet, historique illimité, assistant vocal complet. Facturation mensuelle ou annuelle, tarif différencié FR/MA selon pouvoir d'achat local. |
| **Partenariats bornes de recharge** | Commission sur les sessions de recharge réservées/payées via l'app auprès des réseaux partenaires (Ionity, TotalEnergies, réseaux locaux marocains émergents). |
| **Marketplace entretien** | Commission sur les prises de rendez-vous et devis auprès des ateliers partenaires certifiés. |
| **Partenariats assurance** | Revenu d'apport (lead generation) et partage de données de conduite anonymisées avec consentement pour tarification comportementale (pay-how-you-drive). |
| **Marketplace communautaire** | Commission sur les transactions entre membres (accessoires, covoiturage). |
| **Données agrégées anonymisées (B2B)** | Insights de mobilité électrique vendus à des acteurs de l'énergie ou de l'aménagement urbain, dans le strict respect du RGPD/loi 09-08 et avec opt-in explicite. |
| **Flotte & Pro** | Offre B2B dédiée (indépendants, PME, VTC) avec reporting multi-véhicules, facturation groupée. |

---

## 9. Feuille de route MVP

### Priorité 1 — Recharge & Autonomie
**Pourquoi en premier :** c'est le point de douleur n°1 et quotidien de tout conducteur électrique, celui qui génère le plus d'usage récurrent (donc de rétention) et qui différencie le plus immédiatement l'app d'un simple gadget. C'est aussi le module le plus simple à valoriser commercialement dès le lancement (partenariats bornes).

### Priorité 2 — Sécurité du véhicule
**Pourquoi en second :** fonctionnalité à forte valeur perçue immédiate (verrouillage à distance, alertes), qui s'appuie directement sur les commandes déjà exposées par l'API BYD, donc rapide à implémenter techniquement, et qui rassure particulièrement le marché marocain, plus sensible à la question du vol de véhicule.

### Priorité 3 — Communauté de conducteurs BYD
**Pourquoi en troisième :** moteur d'acquisition et de rétention à coût marginal quasi nul une fois la masse critique atteinte ; permet de construire l'effet réseau et la fidélité dès les premiers mois, avant que les modules plus complexes (diagnostic IA avancé, navigation EV-aware complète) ne soient matures.

*Les modules Entretien & diagnostic, Navigation intelligente et Économies d'énergie sont développés en version simplifiée dès le MVP (visibilité basique) puis enrichis en versions 1.1 et 1.2, une fois la base d'utilisateurs et de données télémétriques suffisante pour entraîner les modèles IA prédictifs.*

---

## 10. Contraintes et considérations locales

### France
- **Infrastructure de recharge** : réseau mature mais fragmenté entre opérateurs (interopérabilité imparfaite) ; nécessité d'agréger plusieurs réseaux plutôt que d'en privilégier un seul.
- **Réglementation** : RGPD strict sur les données de géolocalisation et de conduite ; obligation d'un DPO et d'une base légale claire pour tout traitement IA. Cadre CNIL sur la vidéosurveillance embarquée si intégration caméra.
- **Habitudes d'usage** : forte attente sur l'intégration avec les compteurs Linky/heures creuses et les box domotiques déjà installées.
- **Langue** : français uniquement suffisant, avec vigilance sur l'accessibilité (RGAA) pour les organismes publics/flottes.

### Maroc
- **Infrastructure de recharge** : réseau public encore embryonnaire et concentré sur l'axe Casablanca-Rabat-Tanger-Marrakech ; la recharge à domicile (souvent en immeuble collectif, sans installation dédiée) est un point de friction majeur à documenter dans l'app (recensement des solutions de recharge en copropriété).
- **Réglementation** : loi 09-08 relative à la protection des données personnelles, moins contraignante que le RGPD mais nécessitant néanmoins une déclaration à la CNDP ; réglementation en évolution sur les véhicules électriques (exonérations douanières, incitations).
- **Langue** : nécessité d'une interface bilingue français/arabe avec support RTL complet pour l'arabe classique écrit, doublé d'un ton et d'exemples adaptés au darija à l'oral (assistant vocal, contenu communautaire).
- **Réseau et connectivité** : couverture data variable hors grandes villes ; l'app doit fonctionner en mode dégradé/offline pour les fonctions critiques (dernier état connu du véhicule, itinéraire déjà téléchargé).
- **Habitudes d'usage** : véhicule perçu comme un bien statutaire, sensibilité plus forte aux fonctions de sécurité et d'entretien préventif qu'aux subtilités tarifaires de recharge (marché de l'électricité moins dérégulé qu'en France) ; paiement mobile (ex. wallets locaux) à intégrer en complément des cartes bancaires pour la marketplace et les bornes.
- **Écosystème partenaires** : réseau d'ateliers agréés BYD encore en construction au Maroc ; l'app doit pouvoir référencer et qualifier des garages indépendants « BYD-friendly » en complément du réseau officiel, avec système d'avis communautaires pour construire la confiance.

---

*Document destiné à servir de base à un cahier des charges détaillé et à une présentation à des investisseurs ou partenaires (BYD, opérateurs de recharge, assureurs).*

# BYD Connected Monitor

Deux applications Android qui communiquent par MQTT sécurisé (TLS) :

- **Car Agent** (`app/`, `com.asmontec.bydavm.car`) — installée sur l'écran
  Android de la BYD Seal U DM-i. Agent de collecte et de publication,
  **strictement en lecture seule**.
- **Monitor Mobile** (`mobile/`, `ma.asmontec.bydmonitor.mobile`) —
  installée sur le téléphone du propriétaire. Tableau de bord sombre qui
  s'abonne aux données publiées par l'agent.

Aucune des deux applications n'envoie de commande au véhicule. Aucune
valeur non accessible n'est simulée : elle est transmise et affichée
comme `null` / `—`.

## Architecture

```
BYDConnectedMonitorCar/
├── app/       Car Agent   (publication uniquement)
└── mobile/    Monitor Mobile (abonnement uniquement)
```

Les deux modules embarquent chacun une petite implémentation MQTT 3.1.1
écrite sans bibliothèque externe (`mqtt/MiniMqttClient.java`), plutôt
qu'une dépendance tierce, afin de limiter la surface d'audit et de
supply-chain sur un projet embarqué dans un véhicule. Elle ne prend en
charge que ce qui est nécessaire :

- Car Agent : `CONNECT`, `PUBLISH` (QoS 0, retain optionnel), `PINGREQ`,
  `DISCONNECT`, testament de connexion (LWT) sur le topic `status`.
- Monitor Mobile : `CONNECT`, `SUBSCRIBE` (QoS 0), réception `PUBLISH`,
  `PINGREQ`, `DISCONNECT`.

## Paramètres MQTT (verrouillés)

| Paramètre | Valeur |
|---|---|
| Serveur | `f110c3c7fb39470e865d51c9530c8e9c.s1.eu.hivemq.cloud` |
| Port | `8883` (MQTT sur TLS 1.2/1.3) |
| Préfixe | `asmontec/byd/seal-u` |

Ces valeurs sont des constantes de code (`mqtt/MqttConfig.java` dans
chaque module) : elles ne sont pas modifiables depuis l'interface.
Seuls le nom d'utilisateur et le mot de passe MQTT sont saisis par
l'utilisateur puis chiffrés (AES-256-GCM, clé Android Keystore) avant
d'être stockés dans SharedPreferences.

### Topics

Publiés par l'agent voiture :

- `asmontec/byd/seal-u/status` — `{"online":bool,"timestamp":...}`, retenu, avec testament de déconnexion
- `asmontec/byd/seal-u/telemetry/live` — voir schéma ci-dessous
- `asmontec/byd/seal-u/location/live` — sous-ensemble position/vitesse
- `asmontec/byd/seal-u/android/health` — `{"androidBattery":..,"serviceRunning":true}`
- `asmontec/byd/seal-u/network/status` — `{"networkType":..,"signalLevel":..}`

Réservés par le schéma du projet mais **non encore publiés** par cette
version de l'agent (à définir avec une future intégration BYD/DiLink
officielle) :

- `asmontec/byd/seal-u/events`
- `asmontec/byd/seal-u/trips/current`
- `asmontec/byd/seal-u/trips/history`
- `asmontec/byd/seal-u/camera/status`

L'application mobile s'abonne à `asmontec/byd/seal-u/#`.

### Exemple JSON — `telemetry/live`

```json
{
  "vehicleId": "byd-seal-u-01",
  "timestamp": 1784929090922,
  "online": true,
  "speedKmh": 0.0,
  "latitude": 31.6245,
  "longitude": -8.0123,
  "altitude": 512.0,
  "accuracy": 6.5,
  "androidBattery": 78,
  "networkType": "4G",
  "signalLevel": 4,
  "soc": null,
  "rangeKm": null,
  "powerKw": null,
  "torqueNm": null,
  "batteryTemp": null
}
```

`soc`, `rangeKm`, `powerKw`, `torqueNm` et `batteryTemp` restent `null`
tant qu'aucune API BYD/DiLink officielle et autorisée n'a été validée
sur le véhicule — ce n'est pas encore le cas dans cette version.

## Sécurité

- MQTT exclusivement sur TLS 1.2/1.3, vérification du nom d'hôte via
  `SSLParameters.setEndpointIdentificationAlgorithm("HTTPS")`,
  épinglage optionnel de l'empreinte SHA-256 du certificat.
- Identifiants MQTT chiffrés AES-256-GCM, clé conservée exclusivement
  dans Android Keystore (jamais exportable).
- Car Agent : écran de configuration protégé par un code local (PBKDF2,
  120 000 itérations), verrouillage 5 minutes après 5 échecs.
- `android:allowBackup="false"` sur les deux applications.
- Aucun service ni receiver exporté (`android:exported="false"`), sauf
  l'activité de lancement.
- Aucune WebView dans le projet : supprime cette surface d'attaque
  plutôt que de la durcir.
- Permissions Android limitées au strict nécessaire (réseau, position,
  notifications, service de premier plan, démarrage automatique).
- Compte MQTT voiture limité à la publication ; compte mobile limité à
  l'abonnement (à configurer côté broker HiveMQ, voir plus bas).

## Configuration HiveMQ Cloud

1. Dans HiveMQ Cloud, créez deux identifiants distincts sur le cluster :
   un compte **publication uniquement** sur `asmontec/byd/seal-u/#` pour
   l'agent voiture, un compte **abonnement uniquement** sur le même
   préfixe pour l'application mobile.
2. Renseignez le compte voiture dans **Car Agent → Configuration
   développeur** (créez d'abord un code local, 6 à 10 chiffres).
3. Renseignez le compte mobile dans **Monitor Mobile → Plus →
   Paramètres de connexion**, en saisie manuelle ou via un code
   d'activation (JSON `{"u":"...","p":"..."}` encodé en base64). La
   lecture par QR code n'est pas implémentée dans cette version (aucune
   bibliothèque de scan n'a été ajoutée, pour limiter les dépendances).

## Compilation locale

Prérequis : JDK 17, Android SDK Platform 35, Build Tools 35.0.0, Gradle 8.10.2.

```bash
gradle clean test lintDebug :app:assembleDebug :mobile:assembleDebug
```

APK produites :

```
app/build/outputs/apk/debug/app-debug.apk
mobile/build/outputs/apk/debug/mobile-debug.apk
```

Le workflow GitHub Actions (`.github/workflows/build-apk.yml`) exécute
les tests unitaires, Android Lint, compile les deux APK, vérifie
l'archive et publie l'empreinte SHA-256 en artefact.

## Tests

- `app/src/test`, `mobile/src/test` : tests JVM purs (JUnit) sur la
  logique sans dépendance Android — codec de longueur MQTT (variable
  byte integer), sérialisation JSON de la télémétrie (valeurs `null`
  préservées), calcul de distance GPS (Haversine). Ces trois familles
  ont été compilées et exécutées manuellement hors Gradle pendant le
  développement pour vérifier leur exactitude ; elles s'exécutent aussi
  via `gradle test` en CI.
- Le chiffrement Keystore (`SecureCredentialStore`), le service de
  premier plan, le redémarrage après reboot et l'interface ne peuvent
  être vérifiés qu'avec un appareil ou un émulateur Android réel : **ils
  n'ont pas été exécutés dans cet environnement de développement**, qui
  ne dispose ni du SDK Android, ni d'un appareil, ni d'un véhicule.

## État réel des fonctions

**Actives et vérifiables par lecture de code / CI :**

- Publication télémétrie GPS + batterie Android + réseau (agent voiture)
- Connexion MQTT/TLS avec vérification de certificat, testament de
  déconnexion, file d'attente hors-ligne, reconnexion à temporisation
  exponentielle
- Chiffrement des identifiants (Keystore AES-GCM) et code local PBKDF2
- Démarrage automatique après redémarrage (`BOOT_COMPLETED`)
- Abonnement mobile, reconstruction d'état, historique de trajets
  (SQLite), journal d'événements, notifications configurables
- Interface : Accueil / État / Carte (hors ligne) / Caméras (en attente)
  / Plus, avec valeurs `—` pour tout champ non reçu

**Préparées mais non disponibles (honnêtement affichées comme telles) :**

- Données BYD/DiLink réelles (SOC, autonomie, puissance, couple,
  température batterie, portes, climatisation) — nécessitent une API
  constructeur officielle non intégrée ici
- Flux caméra (les 6 écrans affichent "Module caméra en attente d'une
  interface véhicule autorisée")
- Commandes distantes (boutons visibles, désactivés par défaut, aucun
  envoi possible sans API officielle + authentification forte)
- Carte en ligne (Google Maps/Mapbox/OSM) — aucune clé fournie, repli
  sur l'affichage des coordonnées
- Scan QR code pour l'activation mobile
- Compilation, signature et installation réelles sur un appareil/
  véhicule (nécessite le SDK Android, un keystore de signature et un
  appareil physique — aucun des trois n'est disponible dans cet
  environnement de développement automatisé)

## Prochaine étape

Définir, avec un accès officiel BYD/DiLink, un `BydVehicleDataSource`
strictement en lecture seule pour publier réellement `soc`, `rangeKm`,
`powerKw`, `torqueNm`, `batteryTemp` ainsi que les topics `events`,
`trips/current`, `trips/history` et `camera/status`.

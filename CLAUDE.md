# CLAUDE.md — Idle

Guide de référence pour toute personne (humaine ou agent) qui travaille sur ce dépôt.
À tenir à jour au fil du développement.

## Le projet

**Idle** est un gestionnaire de temps d'écran Android, première application d'une suite d'apps
minimalistes. L'utilisateur se pose des barrières — des **périodes** pendant lesquelles certaines
apps sont interdites, et des **minuteurs** qui plafonnent leur usage quotidien — dont la levée
exige un geste physique volontaire : scanner un QR code, approcher un tag NFC, ou se trouver dans
une zone précise.

### Philosophie

- **Minimaliste** — peu d'éléments par écran, chaque écran a un rôle unique et clair.
- **Gratuit** — aucune fonctionnalité payante, aucun achat intégré.
- **Sans publicité.**
- **Sans tracking** — aucune analytics, aucun crash reporter, aucun SDK tiers de mesure.
  La permission `INTERNET` n'est pas déclarée : l'app ne peut techniquement rien envoyer.
- **La friction est la fonctionnalité** — un déverrouillage doit coûter un effort réel.
  Toute facilité ajoutée qui permet de contourner une règle d'un simple tap est un bug.

## Décisions produit

| Sujet | Décision |
|---|---|
| Durée d'un déverrouillage | 15 min par défaut, réglable dans les Paramètres (valeur globale) |
| Portée d'un déverrouillage | L'app qui a déclenché le blocage, pas toute la règle |
| Mode urgence | 5 min par jour au total, toutes apps confondues, fractionnable, **non configurable**, remis à zéro à minuit |
| Anti-contournement | Les règles restent consultables mais non modifiables tant qu'elles sont actives. Modifier, supprimer ou désactiver une règle active exige son déverrouillage. La durée globale de déverrouillage est verrouillée dès qu'une règle est active. |
| Réglages système | Idle ne bloque pas l'accès aux Réglages Android. L'utilisateur reste maître de son appareil. |
| Déverrouiller une règle | Depuis l'app, via la méthode de la règle. La levée porte sur la règle (droit de la modifier), pas sur ses apps. |
| Langues | Anglais (`values/`) et français (`values-fr/`), rien d'autre |

## Stack

| Composant | Version | Rôle |
|---|---|---|
| Android Gradle Plugin | 9.4.1 | build |
| Gradle | 9.7.1 | build |
| Kotlin | 2.3.21 (fourni par AGP) | langage |
| KSP | 2.3.12 | génération de code (Room, Hilt) |
| Compose BOM | 2026.09.00 | UI (Material 3 1.4.0) |
| Room | 2.8.5 | règles, apps, méthodes, historique |
| DataStore Preferences | 1.2.1 | préférences simples uniquement |
| Hilt | 2.60.1 | injection de dépendances |
| WorkManager | 2.11.2 | tâches secondaires uniquement |
| CameraX + ZXing | 1.6.2 / 3.5.4 | scan des QR codes et codes-barres |
| Navigation Compose | 2.10.1 | navigation type-safe (kotlinx.serialization) |

`minSdk` 30 (Android 11) · `compileSdk` 37.2 · `targetSdk` 37.

`minSdk` 30 parce que `LocationManager.getCurrentLocation` y apparaît : c'est l'API qui lit une
position une seule fois, sans jamais s'abonner aux mises à jour ni poser de geofence.

Les versions des plugins Kotlin sont alignées sur le compilateur qu'embarque AGP 9.4. Un plugin
plus récent que le compilateur serait une incompatibilité silencieuse, donc l'avertissement
`NewerVersionAvailable` de Lint est refusé sciemment : c'est le seul qui reste.

## Architecture

MVVM, trois couches, un seul module Gradle `:app`. Les frontières sont tenues par les packages et
par les interfaces de repository déclarées dans `domain/`, pas par des modules Gradle : la
modularisation serait prématurée à cette taille.

```
com.juthing.idle
├── core/        transverse : horloge injectable, thème, composants UI partagés
├── data/        Room, DataStore, sources système, mappers, implémentations des repositories
├── domain/      modèles, interfaces de repository, use cases — aucune dépendance Android
├── blocking/    AccessibilityService, coordinateur de blocage, notifications, workers
└── ui/          un package par écran : Composable + ViewModel + UiState
```

**Règle de dépendance** : `ui` → `domain` ← `data`. La couche `ui` ne touche jamais `data`
directement, et `domain` n'importe rien d'Android (hors annotations) pour rester testable en JVM.

### Pourquoi ces choix

- **AccessibilityService plutôt que du polling** — c'est la seule API qui notifie du changement
  d'app au premier plan en temps réel. `UsageStatsManager` mesure les durées mais ne réagit pas
  assez vite pour bloquer une ouverture d'app.
- **`SYSTEM_ALERT_WINDOW` est indispensable, pas un confort** — Android interdit à une app de
  lancer une activité depuis l'arrière-plan, et faire tourner un service d'accessibilité ne figure
  pas dans la liste des exemptions ; « superposition à d'autres applications » y figure. Sans
  cette permission, le système jette le lancement de l'écran de blocage sans erreur ni journal, et
  l'app paraît marcher par intermittence — elle ne marche en réalité que pendant le court délai de
  grâce qui suit un passage au premier plan. Idle ne dessine jamais de fenêtre de superposition :
  la permission n'est détenue que pour cette exemption, et son absence est signalée à l'instant où
  elle fait échouer un blocage.
- **Le décompte vit dans le coordinateur, pas dans un service** — depuis Android 12, une app dont
  la seule présence de premier plan est un service d'accessibilité se voit souvent refuser le
  démarrage d'un foreground service ; le décompte ne partait donc jamais. Le service
  d'accessibilité étant lié par le système, le process est déjà maintenu en vie : une simple
  coroutine est plus simple et plus fiable qu'un service qui ne démarre pas. La notification
  persistante reste, parce que du temps ne doit pas être décompté sans que l'utilisateur puisse le
  voir — mais plus rien ne dépend d'elle.
- **Le décompte s'arrête à l'extinction de l'écran** — aucun événement d'accessibilité ne
  l'annonce, donc le service écoute `ACTION_SCREEN_OFF`. Sans cela, une app minutée laissée
  ouverte consommait son quota dans une poche.
- **ZXing plutôt que ML Kit** — ML Kit dépend des Google Play Services. ZXing garde l'app
  utilisable sur un appareil dégooglisé, ce qui est cohérent avec la philosophie du projet.
- **Kotlin intégré à AGP** (`android.builtInKotlin=true`) — ce n'est pas un choix : le nouveau DSL
  d'AGP 9, actif par défaut, est incompatible avec le plugin `org.jetbrains.kotlin.android`. AGP
  compile donc le Kotlin lui-même, avec sa propre version (2.3.21). Les plugins `plugin.compose`
  et `plugin.serialization` restent appliqués explicitement, et KSP 2.3.12 fonctionne avec cette
  chaîne.
- **Secrets de déverrouillage hachés** — on stocke un SHA-256 du contenu d'un QR code ou de
  l'identifiant d'un tag NFC, jamais la valeur brute : une lecture de la base ne permet pas de
  fabriquer le code manquant.
- **Aucune sauvegarde** (`allowBackup=false`) — restaurer l'état de blocage sur un autre appareil
  n'a pas de sens, et les secrets ne doivent pas voyager.
- **`<queries>` plutôt que `QUERY_ALL_PACKAGES`** — lister les apps lançables suffit à alimenter
  le sélecteur. `QUERY_ALL_PACKAGES` est une permission sensible côté Play, qu'Idle n'a pas besoin
  de demander.
- **Les sélecteurs sont des feuilles, pas des destinations** — choisir des apps, une méthode ou une
  heure se fait dans une `ModalBottomSheet` au-dessus de l'écran d'édition. Passer par la
  navigation obligerait à renvoyer un résultat et risquerait de perdre le brouillon en cours.
- **Idle ne peut pas se bloquer elle-même** — l'app est filtrée du sélecteur : la bloquer
  enfermerait l'utilisateur hors du seul écran d'où un blocage peut être levé.
- **`LocationManager` plutôt que le fused provider** — même raison que ZXing : pas de dépendance
  aux Google Play Services. La position n'est lue qu'à la demande, jamais en abonnement et jamais
  via une geofence, donc Idle ne peut pas suivre l'utilisateur en arrière-plan.
- **NFC en reader mode** — la lecture reste liée à l'écran qui l'a demandée et s'arrête dès qu'il
  disparaît, donc Idle n'intercepte jamais un tag destiné à une autre app. Seul l'identifiant
  matériel est lu : Idle n'écrit jamais sur un tag et n'en lit jamais le contenu.
- **La durée de déverrouillage est gelée dès qu'une règle est active** — sinon il suffirait de la
  passer de 15 minutes à une heure en pleine période pour contourner toutes les règles.
- **Permissions demandées en contexte** — la caméra au moment de scanner, la position au moment
  d'enregistrer un lieu, avec la raison affichée juste à côté du bouton. Une demande hors contexte
  est une demande refusée.
- **Un seul coordinateur** — tout ce qui observe le premier plan passe par `BlockingCoordinator`,
  donc la décision de bloquer, celle de démarrer le décompte et celle de l'arrêter sont toujours
  prises depuis la même vue de l'état.
- **La raison du blocage est recalculée par l'écran de blocage** — elle n'est pas transportée dans
  l'intent : une période peut se terminer entre la décision du service et l'affichage de l'écran,
  et Idle ne doit jamais insister sur un blocage qui n'a plus lieu d'être.
- **Le décompte écrit des incréments, pas des totaux** — un process tué entre deux ticks coûte au
  plus un tick, pas toute la session.
- **La réconciliation garde le plus grand des deux compteurs** — sous-compter rendrait du temps
  déjà consommé, ce que le quota est précisément censé empêcher ; sur-compter termine juste une
  session un peu tôt.
- **Pas de carte en ligne pour choisir un lieu** — Idle ne déclare pas `INTERNET`, donc il n'y a
  pas de tuiles à afficher et il n'y en aura pas : un sélecteur qui dépendrait d'un serveur de
  tuiles échangerait la promesse centrale de l'app contre un plus bel écran. Le sélecteur dessine
  ce qui compte réellement ici — distance et direction entre le téléphone et le point choisi, avec
  le rayon tracé à l'échelle — et le point se place au doigt.
- **Aucune permission n'est obligatoire pour terminer l'onboarding** — refuser le service
  d'accessibilité laisse une app fonctionnelle pour tout le reste, et l'écran Réglages continue de
  dire ce qui manque plutôt que de retenir l'utilisateur en otage sur l'onboarding.
- **Le bouton vers les réglages d'accessibilité est inaccessible tant que le consentement n'est pas
  donné** — c'est exactement ce qu'exige la politique Google Play, et c'est aussi la seule façon
  honnête de présenter la chose.
- **Clé de debug committée** (`app/debug.keystore`) — sans elle, chaque build CI signerait avec une
  clé différente et Android refuserait d'installer une mise à jour par-dessus la précédente, ce qui
  obligerait à désinstaller et perdrait toutes les règles de l'utilisateur à chaque build. C'est une
  clé de **debug** : la clé de signature de publication n'ira jamais dans le dépôt, elle passera par
  les secrets GitHub.
- **Pas de reset de minuit** — les lignes d'usage sont indexées par date et le compteur d'urgence
  porte le jour auquel il appartient, donc un nouveau jour démarre déjà à zéro. Un worker dont
  l'app dépendrait pour être correcte à minuit serait un worker sur lequel on ne peut pas compter.
  WorkManager ne fait donc que du ménage : purge des vieilles lignes et des autorisations
  expirées, sous contrainte de batterie.

## Conventions

- **Langue** : identifiants, commentaires et KDoc en **anglais**. Les chaînes visibles par
  l'utilisateur vivent dans `strings.xml`, jamais en dur dans le code.
- **Nommage** : `XxxScreen` (Composable d'écran), `XxxViewModel`, `XxxUiState`, `XxxRepository`
  (interface dans `domain`) / `XxxRepositoryImpl` (dans `data`), `XxxEntity` (Room),
  `XxxDao`, `XxxUseCase`.
- **KDoc obligatoire** sur toute classe et toute fonction publique. Le KDoc dit *pourquoi*, pas
  *quoi* : si le commentaire paraphrase la signature, il ne sert à rien.
- **État** : `StateFlow<XxxUiState>` exposé par le ViewModel, collecté avec
  `collectAsStateWithLifecycle()`. Pas de `mutableStateOf` public dans les ViewModels.
- **Composables** : paramètre `modifier: Modifier = Modifier` en premier paramètre optionnel,
  sans état interne quand l'état peut être hissé.
- **Pas de code mort** : rien n'est commité « pour plus tard ».

## Design

- **Material 3 strictement** — aucun composant Material 2, aucun composant maison qui duplique un
  composant M3 existant.
- **Material You** — couleurs dynamiques issues du fond d'écran dès Android 12 ; palette de repli
  sobre et désaturée en dessous. Thème clair et sombre suivis du système.
- **Typographie** — Google Sans Flex (SIL OFL), embarquée en trois graisses, appliquée à toute
  l'échelle typographique M3.
- **Couleur porteuse de sens** — la couleur d'accent signale ce qui est actif ou bloquant ; le
  reste de l'interface reste neutre.
- **Espacements** — multiples de 4 dp, marge latérale d'écran de 16 dp.
- **Animations** — transitions courtes et discrètes, jamais décoratives.
- **États vides** — ils expliquent à quoi sert la section avec un exemple concret, ils ne se
  contentent pas d'annoncer qu'il n'y a rien.

## Google Play — AccessibilityService

Idle n'est **pas** un outil d'accessibilité. Avant toute publication :

- ne jamais déclarer `android:isAccessibilityTool="true"` ;
- afficher une **prominent disclosure** in-app, sur un écran dédié atteint sans naviguer dans les
  menus, décrivant la donnée lue (le package de l'app au premier plan), son usage, et le fait
  qu'elle ne quitte jamais l'appareil, avec un consentement explicite **avant** d'envoyer
  l'utilisateur vers les réglages d'accessibilité ;
- remplir le *Permission Declaration Form* de la Play Console et fournir une vidéo montrant
  l'ouverture de l'app, l'écran de disclosure, l'acceptation et le refus, puis la fonctionnalité ;
- `targetSdk` 36 minimum (obligatoire depuis le 31 août 2026) ;
- garder `accessibility_service_config.xml` au strict nécessaire.

## Distribution

Chaque push sur `main` ou `claude/**` déclenche `.github/workflows/build.yml` : tests, lint, build,
puis publication de l'APK de debug sur la release `dev`. C'est de là que l'app s'installe sur un
téléphone, sans chaîne de build locale. `INSTALL.md` documente la procédure côté téléphone, dont
l'étape des **paramètres restreints** d'Android 13+, sans laquelle le service d'accessibilité ne
peut pas être activé pour une app installée hors Play Store.

## Commandes

```bash
./gradlew assembleDebug          # construire l'APK de debug
./gradlew installDebug           # installer sur l'appareil connecté
./gradlew testDebugUnitTest      # tests unitaires (logique métier)
./gradlew connectedDebugAndroidTest  # tests instrumentés (Room)
./gradlew lint                   # Android Lint
./gradlew clean                  # nettoyer les sorties de build
```

Le SDK Android est localisé par `local.properties` (`sdk.dir`), qui n'est pas versionné.

## État d'avancement

- [x] Étape 0 — squelette Gradle, thème Material 3, typographie, navigation à trois onglets, i18n
- [x] Étape 1 — couche data (Room, DataStore, repositories)
- [x] Étape 2 — couche domain et tests unitaires
- [x] Étape 3 — UI Périodes et Minuteurs, sélecteur d'apps
- [x] Étape 4 — méthodes de déverrouillage (QR, NFC, zone)
- [x] Étape 5 — moteur de blocage et mode urgence
- [x] Étape 6 — onboarding et permissions
- [x] Étape 7 — workers, verrouillage de l'édition, finitions
- [x] Étape 8 — correction du blocage (superposition, décompte hors service), déverrouillage
      depuis l'app, réglages en sous-pages, retour haptique, refonte de l'écran de blocage et des
      écrans de capture

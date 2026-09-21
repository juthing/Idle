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

`minSdk` 29 · `compileSdk` 37.2 · `targetSdk` 36.

`compileSdk` est en avance sur `targetSdk` : les artefacts AndroidX de fin 2026 exigent d'être
compilés contre l'API 37, alors que `targetSdk` 36 reste le plancher demandé par Google Play et
n'expose pas encore l'app aux changements de comportement d'Android 17.

## Architecture

MVVM, trois couches, un seul module Gradle `:app`. Les frontières sont tenues par les packages et
par les interfaces de repository déclarées dans `domain/`, pas par des modules Gradle : la
modularisation serait prématurée à cette taille.

```
com.juthing.idle
├── core/        transverse : horloge injectable, thème, composants UI partagés
├── data/        Room, DataStore, sources système, mappers, implémentations des repositories
├── domain/      modèles, interfaces de repository, use cases — aucune dépendance Android
├── blocking/    AccessibilityService, coordinateur de blocage, foreground service, workers
└── ui/          un package par écran : Composable + ViewModel + UiState
```

**Règle de dépendance** : `ui` → `domain` ← `data`. La couche `ui` ne touche jamais `data`
directement, et `domain` n'importe rien d'Android (hors annotations) pour rester testable en JVM.

### Pourquoi ces choix

- **AccessibilityService plutôt que du polling** — c'est la seule API qui notifie du changement
  d'app au premier plan en temps réel. `UsageStatsManager` mesure les durées mais ne réagit pas
  assez vite pour bloquer une ouverture d'app.
- **Foreground Service pour le décompte** — WorkManager n'offre aucune garantie de ponctualité ;
  il ne sert donc qu'au reset de minuit et à la purge de l'historique, jamais au blocage.
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
- [ ] Étape 1 — couche data (Room, DataStore, repositories)
- [ ] Étape 2 — couche domain et tests unitaires
- [ ] Étape 3 — UI Périodes et Minuteurs, sélecteur d'apps
- [ ] Étape 4 — méthodes de déverrouillage (QR, NFC, zone)
- [ ] Étape 5 — moteur de blocage et mode urgence
- [ ] Étape 6 — onboarding et permissions
- [ ] Étape 7 — workers, verrouillage de l'édition, finitions

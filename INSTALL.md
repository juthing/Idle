# Installer Idle sur son téléphone

Aucun outil à installer sur l'ordinateur. GitHub compile l'application à chaque modification du
code et publie un fichier `.apk` que le téléphone télécharge et installe directement.

Comptez dix minutes la première fois, dont la moitié à cause d'une protection Android décrite plus
bas — c'est l'étape où tout le monde se bloque.

---

## 1. Récupérer l'application

**Depuis le navigateur du téléphone**, pas depuis l'ordinateur.

1. Ouvrez `https://github.com/juthing/Idle/releases`
2. Ouvrez la release **« Dernier build »**
3. Dans la section *Assets*, touchez **`Idle-debug.apk`**

Le téléchargement démarre. Si le dépôt est privé, connectez-vous à GitHub dans ce navigateur
d'abord, sinon le lien renverra une page 404.

> Si la release n'existe pas encore, c'est que le build n'a jamais tourné. Allez dans l'onglet
> **Actions** du dépôt : chaque ligne est un build. Une coche verte signifie que la release est
> prête, une croix rouge signifie que le build a échoué — ouvrez-la pour lire le journal.

## 2. Autoriser l'installation

Android refuse par défaut d'installer une application venue d'ailleurs que du Play Store.

À l'ouverture du fichier téléchargé, un message propose d'aller dans les réglages : acceptez, et
activez **« Autoriser depuis cette source »** pour le navigateur. Revenez en arrière, et
l'installation se poursuit.

Le chemin manuel, si le message n'apparaît pas :
**Paramètres → Applications → Accès spécial → Installer des applications inconnues**, puis
choisissez votre navigateur.

## 3. ⚠️ Les paramètres restreints — l'étape qui bloque tout le monde

**Sans cette étape, Idle ne bloque rien du tout.**

Depuis Android 13, une application installée hors du Play Store n'a pas le droit d'activer un
service d'accessibilité. Or Idle repose entièrement dessus : c'est ce service qui remarque quelle
application vient d'être ouverte. Sans lui, l'application se lance, les règles se créent, et rien
ne se passe jamais.

L'option qui lève cette restriction **n'apparaît pas tant qu'on n'a pas essayé une première fois**.
Il faut donc provoquer le refus avant de pouvoir le contourner :

1. **Paramètres → Accessibilité → Applications installées → Idle**
2. Essayez d'activer la bascule.
   Android répond que « pour votre sécurité, ce réglage est actuellement indisponible ».
   C'est attendu : cette tentative vient de débloquer l'option suivante.
3. **Paramètres → Applications → Idle**
4. Menu **⋮** en haut à droite → **« Autoriser les paramètres restreints »**
5. Confirmez avec votre code ou votre empreinte
6. Retournez dans **Accessibilité → Applications installées → Idle** et activez la bascule

Le libellé change selon le constructeur — « Paramètres restreints », « Autoriser les réglages
restreints », « Allow restricted settings ». Le menu ⋮ de la fiche de l'application est toujours
le bon endroit.

## 4. L'accès aux statistiques d'usage

Nécessaire pour les minuteurs : sans lui, Idle ne sait pas combien de temps vous avez passé dans
une application aujourd'hui.

**Paramètres → Applications → Accès spécial → Accès aux données d'utilisation → Idle → activer.**

L'onboarding d'Idle propose un bouton qui ouvre directement cet écran.

## 5. Vérifier que tout fonctionne

L'écran **Réglages** d'Idle liste les trois permissions avec leur état. Si le service
d'accessibilité est désactivé, un avertissement rouge s'affiche en haut de l'écran : plus rien
n'est bloqué tant qu'il est là.

Premier test, avec une application dont vous vous moquez :

1. **Réglages → Méthodes de déverrouillage → Nouvelle méthode → QR code**, scannez n'importe quel
   code-barres — celui d'un paquet de céréales fait très bien l'affaire — et donnez-lui un nom.
2. **Périodes → Nouvelle période**, choisissez l'application de test, cochez le jour, réglez une
   plage horaire qui couvre l'instant présent, sélectionnez votre méthode, enregistrez.
3. Ouvrez l'application de test : l'écran de blocage doit apparaître.
4. Touchez **Déverrouiller**, rescannez le même code-barres : l'application s'ouvre.
5. Attendez quinze minutes et rouvrez-la : elle est bloquée à nouveau.

Si l'étape 3 ne se produit pas, le service d'accessibilité n'est pas actif — revenez à la
section 3.

## 6. Mettre à jour

À chaque modification du code poussée sur GitHub, un nouveau build est publié au même endroit.
Retéléchargez `Idle-debug.apk` et installez-le par-dessus : vos règles, vos méthodes et vos
compteurs sont conservés.

Les builds sont signés avec une clé fixe committée dans le dépôt, précisément pour que cela
fonctionne. Sans elle, chaque build aurait une signature différente et Android exigerait une
désinstallation complète — donc la perte de toutes vos règles — à chaque mise à jour.

## En cas de problème

**« Application non installée »**
La version déjà présente a été signée avec une autre clé. Désinstallez Idle, puis réinstallez.
Cela n'arrive normalement qu'une seule fois, en passant d'un APK bâti avant cette mise en place.

**La bascule d'accessibilité est grise**
C'est la restriction de la section 3. Il faut d'abord tenter de l'activer pour que l'option
« Autoriser les paramètres restreints » apparaisse dans le menu ⋮ de la fiche de l'application.

**Le service d'accessibilité se coupe tout seul**
Certains constructeurs — Xiaomi, Samsung, OnePlus, Oppo — tuent les services en arrière-plan de
façon agressive. Cherchez Idle dans les réglages de batterie et passez-le en
« Sans restriction » ou « Ne pas optimiser ».

**Rien n'est bloqué alors que tout semble activé**
Vérifiez que la règle couvre bien l'application ouverte, que le jour est coché, et que l'heure
actuelle tombe dans la plage. Une période de 22 h à 7 h appartient au jour où elle **commence** :
cochée le lundi, elle court du lundi soir au mardi matin, et ne bloque pas le lundi matin.

**Le build échoue sur GitHub**
Onglet **Actions**, ouvrez le build rouge, dépliez l'étape en erreur. Le message utile est
généralement dans `Test and lint` ou `Build the debug APK`.

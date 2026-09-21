# Installer Idle sur son téléphone

Aucun outil à installer sur l'ordinateur. GitHub compile l'application à chaque modification du
code et publie un fichier `.apk` que le téléphone télécharge et installe directement.

Comptez dix minutes la première fois. L'essentiel de ce temps part dans deux protections Android
qui vont toutes les deux se mettre en travers — sections 3 et 4. Elles sont attendues, elles ne
signalent aucun problème avec l'application, et aucune des deux ne se contourne en insistant.

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

## 3. Play Protect refuse l'installation — c'est normal

Au moment d'installer, Play Protect affiche **« Appli bloquée pour protéger votre appareil »**, avec
pour seul bouton **OK**. Ce n'est pas un défaut d'Idle et ce n'est pas contournable en insistant.

Play Protect bloque automatiquement toute application installée hors Play Store qui déclare un
**service d'accessibilité**, parce que c'est la permission la plus détournée par les logiciels
malveillants bancaires. Idle en déclare un — c'est le cœur de son fonctionnement — donc le blocage
se produira à chaque nouvelle installation.

La seule sortie est de suspendre l'analyse le temps d'installer :

1. Ouvrez le **Play Store**
2. Touchez votre **photo de profil**, en haut à droite
3. **Play Protect**
4. Icône **⚙️** en haut à droite
5. Désactivez **« Analyser les applis avec Play Protect »**
   (sur les versions récentes du Play Store, un bouton **Pause** fait la même chose et se réactive
   tout seul — préférez-le)
6. Retournez sur le fichier `Idle-debug.apk` et relancez l'installation
7. **Réactivez l'analyse** une fois l'installation terminée

Play Protect pourra proposer plus tard de supprimer Idle : refusez, l'application reste en place.

## 4. ⚠️ Les paramètres restreints — l'étape qui bloque tout le monde

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

## 5. ⚠️ La superposition à d'autres applications

**Sans cette étape non plus, Idle ne bloque rien — ou plutôt, il bloque par intermittence, ce qui
est pire.**

Android interdit à une application de s'afficher par-dessus une autre depuis l'arrière-plan.
Faire tourner un service d'accessibilité ne fait *pas* partie des exemptions ; la permission
« superposition à d'autres applications », si. Sans elle, Idle voit bien qu'une application
bloquée vient de s'ouvrir, puis le système jette l'écran de blocage sans le moindre message.

Le symptôme est déroutant : ça marche juste après avoir ouvert Idle, puis plus du tout. C'est le
court délai de grâce qu'Android accorde à une application qui vient d'être au premier plan.

**Paramètres → Applications → Accès spécial → Superposition à d'autres applications → Idle →
activer.**

L'onboarding propose un bouton qui ouvre directement cet écran, et la page **Réglages →
Permissions** indique si elle manque. Idle ne dessine jamais de fenêtre par-dessus quoi que ce
soit : la permission ne sert qu'à avoir le droit d'afficher son propre écran de blocage.

## 6. L'accès aux statistiques d'usage

Nécessaire pour les minuteurs : sans lui, Idle ne sait pas combien de temps vous avez passé dans
une application aujourd'hui.

**Paramètres → Applications → Accès spécial → Accès aux données d'utilisation → Idle → activer.**

L'onboarding d'Idle propose un bouton qui ouvre directement cet écran.

## 7. Vérifier que tout fonctionne

La page **Réglages → Permissions** liste tout ce dont Idle a besoin, avec son état. Tant qu'il
manque le service d'accessibilité ou la superposition, un bandeau rouge s'affiche en haut des
Réglages : plus rien n'est bloqué tant qu'il est là.

Premier test, avec une application dont vous vous moquez :

1. **Réglages → Méthodes de déverrouillage → Nouvelle méthode → QR code**, scannez n'importe quel
   code-barres — celui d'un paquet de céréales fait très bien l'affaire — et donnez-lui un nom.
2. **Périodes → Nouvelle période**, choisissez l'application de test, cochez le jour, réglez une
   plage horaire qui couvre l'instant présent, sélectionnez votre méthode, enregistrez.
3. Verrouillez le téléphone, déverrouillez-le, puis ouvrez l'application de test depuis l'écran
   d'accueil — sans repasser par Idle : l'écran de blocage doit apparaître.
4. Rescannez le même code-barres : l'application s'ouvre.
5. Attendez quinze minutes et rouvrez-la : elle est bloquée à nouveau.

Le détour par l'écran d'accueil de l'étape 3 n'est pas une coquetterie : lancer l'application de
test directement après avoir quitté Idle passerait par le délai de grâce d'Android et masquerait
une permission de superposition manquante.

Si l'écran de blocage n'apparaît pas et qu'une notification dit qu'Idle n'a pas pu l'afficher,
c'est la section 5. Si rien ne se passe du tout, c'est la section 4.

## 8. Mettre à jour

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

**Play Protect bloque de nouveau à la mise à jour**
Même cause qu'à la première installation, même solution : section 3.

**L'écran de blocage apparaît une fois sur deux**
C'est la permission de superposition, section 5. Idle n'a le droit de s'afficher par-dessus une
autre application que pendant quelques secondes après être passé au premier plan ; en dehors de
cette fenêtre, Android refuse en silence.

**La bascule d'accessibilité est grise**
C'est la restriction de la section 4. Il faut d'abord tenter de l'activer pour que l'option
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

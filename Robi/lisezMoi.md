# Projet Robi — Langage de script pour animations graphiques et application distribuée

## Auteurs

- **Le Gall Elouan**
- **Leon Plevert Glenn**
- **Bertin Louan**

---

## Partie 1 — Exercices rendus

### Exercice 1 — Animation le long des bords

- `exercice1/Exercice1_0.java` — Robi se déplace le long des bords de la fenêtre en boucle, avec changement de couleur aléatoire.

### Exercice 2 — Interpréteur de S-expressions basique

- `exercice2/Exercice2_1_0.java` — Exécution d'un script S-expression codé en dur avec `if/else` pour `setColor`, `translate`, `sleep`.

### Exercice 3 — Pattern Command

- `exercice3/Exercice3_0.java` — Refactoring avec une interface `Command` et des classes internes pour chaque commande.

### Exercice 4 — Reference / Environment / Interpreter

- **4.1** (`exercice4/Exercice4_1_0.java`) : architecture `Reference`/`Environment`/`Interpreter` + boucle REPL interactive.
- **4.2** (`exercice4/Exercice4_2_0.java`) : ajout de `add`, `del`, `setDim` et création dynamique d'éléments (`NewElement`, `NewImage`, `NewString`).

### Exercice 5 — Notation pointée

- `exercice5/Exercice5.java` — Nommage hiérarchique (`space.robi`, `space.robi.fils`) et suppression en cascade.
- Exemples : `exercice5/examples/Example1.java`, `exercice5/examples/Example2.java`

### Exercice 6 — Scripts utilisateur

- `exercice6/Exercice6.java` — Définition de scripts paramétrés sur les objets (`addScript`, `clear`). Le script substitue les paramètres puis re-parse et exécute le corps.

---

## Partie 2 — Application distribuée et IHM

Fichiers dans le package `p2Exercice1_2_3/` (`client/`, `server/`, `shared/`).

### Exercice 1 — Client-serveur

- **Serveur** (`server/SExpressionServer.java`) : serveur HTTP (port 4444) qui parse les S-expressions et les exécute. Endpoints : `/parse`, `/health`, `/version`, `/history`, `/save`, `/load`.
- **Rendu serveur** (`server/ServerSideRenderer.java`) : `GSpace` côté serveur pour comparer les rendus.
- **Client** (`client/SExpressionClient.java`) : envoie les S-expressions au serveur, maintient un rendu local, synchronisation automatique par polling.
- **Sérialisation** (`shared/SNodeSerializer.java`) : JSON fait main pour les `SNode`.

### Exercice 2 — Interface graphique

- **2.1** (`client/ClientGUI.java`) : interface Swing avec zone de texte, bouton "Exécuter", vue du `GSpace` local.
- **2.2** : boutons d'action dans la toolbar (Ajouter Rect, Ajouter Oval, Nettoyer, Sauvegarde/Chargement) pour envoyer des S-expressions sans les écrire.

### Exercice 3 — Capture d'écran

- Endpoint `/screenshot` côté serveur qui renvoie un PNG du rendu. Bouton côté client pour récupérer et afficher la capture.

### Exercice 4 — Pipeline CI/CD

- `.github/workflows/test.yml` : compilation, tests JUnit 5, couverture JaCoCo (70%), analyse statique (Checkstyle + SpotBugs), rapport qualité.
- Tests : `SExpressionClientTest.java`, `SNodeSerializerTest.java`
- Persistance : `shared/PersistenceManager.java` (sauvegarde/chargement JSON)

---

## Ce qui n'a pas été fait

- **Exercice 7 de la partie 1** : non réalisé.
- **Exercice 5 de la partie 2** : non réalisé.

---

## Éléments techniques

### Fonctionnement de l'interpréteur

L'utilisateur écrit une S-expression → `SParser` la parse en `SNode` → `Interpreter` résout le receveur dans l'`Environment` → la `Reference` dispatch vers la bonne `Command` → la `Command` exécute l'action graphique.

### Détails des composants de la Partie 2

#### SExpressionServer (Le Serveur)
Héberge un serveur HTTP (`com.sun.net.httpserver`) centralisant l'état de l'animation.
- **Gestion d'état** : Maintient un `ServerSideRenderer` et une version d'état (`stateVersion`) incrémentée à chaque modification.
- **Endpoints clés** : `/parse` pour l'exécution, `/version` pour la synchro, `/history` pour l'historique complet, et `/screenshot` pour l'export PNG.
- **Persistance** : Gère la sauvegarde et le chargement de l'historique des scripts via `/save` et `/load`.

#### SExpressionClient (Le Client Logique)
Gère la communication réseau et la cohérence du rendu local.
- **Synchronisation** : Un thread "démon" interroge `/version` chaque seconde. En cas de décalage, il récupère l'historique complet et reconstruit le `GSpace` local.
- **Environnement** : Initialise les commandes (`setColor`, `addScript`, etc.) et les classes graphiques (`Rect`, `Oval`, `Label`).
- **Mode Hybride** : Envoie les scripts au serveur tout en les exécutant localement pour assurer une interface fluide.

#### ClientGUI (L'Interface Graphique)
IDE Swing complet pour interagir avec le langage Robi.
- **Éditeur** : Zone de texte pour l'écriture de S-expressions.
- **Toolbar interactive** : Boutons de création rapide (Rectangle, Ovale), bouton de nettoyage (`clear`) et bouton de capture d'écran du rendu serveur.
- **Double Vue** : Intègre l'éditeur de code et le composant de dessin `GSpace` dans un panneau scindé (`JSplitPane`).

### Choix de conception notables

- Chaque exercice est un package indépendant pour ne pas casser les exercices précédents.
- La partie 2 réutilise les classes de l'exercice 6.
- `NewElement` instancie les éléments graphiques par réflexion.
- Les scripts (exo 6) fonctionnent par substitution textuelle des paramètres dans le corps.
- Sérialisation JSON faite main (pas de Gson/Jackson).
- Communication HTTP via `com.sun.net.httpserver` (serveur) et `HttpURLConnection` (client).

---

## Bilan critique

### Ce qui fonctionne bien

- Architecture extensible : ajouter une commande = créer une classe `Command` et l'enregistrer.
- Double rendu client/serveur fonctionnel avec synchronisation.
- Interface graphique utilisable avec boutons d'action.

### Limites et améliorations possibles

- Duplication de code entre exercices 4, 5 et 6 — il aurait fallu factoriser.
- Scripts par substitution textuelle : fragile si un nom de paramètre correspond à un nom existant.
- Pas de gestion des types, pas d'opérateur de retour `^`, pas de boucle `whileTrue:`.
- Synchronisation par polling (un thread par seconde) — des WebSockets seraient plus adaptés.

### Ce que nous aurions fait différemment

- Factoriser les classes communes dans un package partagé.
- Implémenter un vrai système de portée pour les scripts.
- Plus de tests unitaires.
- WebSockets au lieu du polling HTTP.
- Bibliothèque JSON (Gson) au lieu du sérialiseur fait main.

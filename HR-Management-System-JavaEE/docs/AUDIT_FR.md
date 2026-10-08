# Audit du projet d'origine et refonte

Date : 8 octobre 2026. Archive examinée : `Projet_GRH-20220322T214419Z-001.zip`.

## Étendue et méthode

L'archive contient 324 fichiers, dont 22 sources Java, 12 JSP, 22 classes compilées et 2 JAR. Tous les chemins et octets ont été inventoriés avec une empreinte SHA-256. Les sources Java et pages applicatives ont été examinées; les fichiers de templates, bibliothèques tierces, configurations et ressources ont été classés selon leur rôle. Les images, polices, classes et JAR ont été identifiés comme ressources binaires; cette analyse n'est pas une décompilation des bibliothèques ni un audit de sécurité exhaustif de leur code.

Le fichier `original-inventory.csv` donne la décision pour chacun des 324 fichiers. Les actifs tiers ne sont pas présentés comme du code métier original.

## Problèmes identifiés

| Priorité | Fichier / zone | Constat | Traitement |
| --- | --- | --- | --- |
| Bloquant | `web/Servlet.java` | `else se(...)` n'est pas du Java valide. | Contrôleur remplacé, compilation Maven. |
| Bloquant | Archive | Aucun script SQL ni export de la base. | Nouveau schéma versionné et initialisation automatique. |
| Bloquant | Configuration Eclipse | Runtime Tomcat/JRE dépendant de chemins locaux, absence de `pom.xml`. | Maven et WAR reproductible avec Java 17. |
| Critique | `dao/Singleton.java` | Identifiants MySQL en dur et connexion partagée statique. | Configuration externe, connexions fermées après usage. |
| Critique | `LoginAdmin`, `Login_Servlet` | Mots de passe en clair dans la requête et les logs; identité conservée dans un DAO partagé par les requêtes. | PBKDF2 salé, identité stockée uniquement dans la session. |
| Critique | Contrôleur et JSP | Pas de filtre d'authentification ni de contrôle cohérent des rôles. | Filtre et contrôles serveur à chaque action. |
| Critique | Suppressions | Opérations exécutables par GET sans CSRF ni propriété des messages. | POST avec jeton; suppression de messages limitée au destinataire. |
| Élevée | `ImpMessage.getMsgRecu` | Même objet `Message` ajouté à chaque itération : les lignes peuvent toutes montrer le dernier message. | Une map indépendante par ligne; test de deux messages distincts. |
| Élevée | `ImpEmploye.listeEmployes2` | `setString(2,...)` pour une requête à un seul paramètre. | Requêtes remplacées et testées. |
| Élevée | `ImpEmploye.modifierAdmin` | Mise à jour sans `WHERE`, donc toutes les lignes de la table. | Suppression de ce chemin inutilisé; gestion des comptes via employee. |
| Élevée | `ImpEmploye.RotationLeft/Right` | Recherche du rang renvoie toujours 0; boucle de gauche commence à 1 et dépasse les indices; connexions/statements mal gérés. | Rotation hebdomadaire atomique, règle explicite. |
| Élevée | `Servlet /consemp` | Forward vers la même route, donc récursion. | Route remplacée par consultation du planning filtrée selon l'utilisateur. |
| Élevée | `Servlet /emploiEmp` | Date ISO décomposée avec année et jour inversés, jour pris depuis le mois. | `LocalDate` et normalisation au lundi. |
| Élevée | `ICaseTabImplDAO` | Tableau fixe de 42 entrées, accès sans vérification des valeurs nulles, ordre SQL non garanti. | Planning normalisé dans une table de créneaux. |
| Élevée | `ImpAbsence.getAbsence` | Filtre sur l'ID d'absence au lieu de l'employé. | Clé étrangère employee_id; test d'isolation. |
| Élevée | JSP | Chemins `/Projet_GRH//`, `/ProjetGrhS4/...` et liens relatifs inconsistants. | URLs basées sur le contexte courant; vues sous WEB-INF. |
| Élevée | `respMsg.jsp` | Destinataire caché sans attribut `name`; formulaires copiés d'un autre module. | Formulaire de réponse relié au destinataire. |
| Moyenne | `Login_Servlet` | Échec de connexion sans réponse utilisable; GET délègue à POST. | Message d'erreur et code HTTP; séparation des méthodes. |
| Moyenne | Contrôleur | Routes déclarées sans traitement et branches non déclarées (`listAbsenceSess`, `updateAdmin`). | Ensemble de routes réduit et cohérent. |
| Moyenne | DAO | Exceptions parfois avalées avec `getStackTrace()`; ressources non fermées sur tous les chemins. | try-with-resources, erreurs HTTP et logs serveur. |
| Moyenne | Entités | `Employe.toString` inclut le mot de passe; listes non initialisées dans Message/Poste; Historique_des_absence ne modélise pas un historique exploitable. | Modèle JDBC simplifié, aucune sérialisation de mot de passe. |
| Moyenne | WebContent | Templates de démonstration, contacts PHP/Composer et dépendances sans rapport avec le serveur Java. | Retirés du livrable exécutable. |
| Moyenne | Tests | `dao/Test.java` est un programme manuel, aucune assertion automatisée. | Tests d'intégration HTTP JUnit. |
| Faible | Build et documentation | Classes compilées livrées, README du template au lieu du projet RH. | `.gitignore`, README dédié, guide GitHub et workflow CI. |

## Lecture par source Java

| Source d'origine | Analyse / décision |
| --- | --- |
| ICaseTabImplDAO | Risque de null, compteur fixe, états de cases exprimés par des noms; remplacé par schedule_slot. |
| IdateSemaineDAO | Interface de rotation trop liée aux noms historiques; remplacée par génération hebdomadaire. |
| IdateSemaineImplDAO | Date précédente ambiguë, erreurs avalées, dépendance à une ligne singleton; remplacée par LocalDate. |
| ImpAbsence | Filtre erroné, ID saisi par utilisateur, employé identifié par nom; remplacé par FK et IDs générés. |
| ImpEmploye | CRUD sans validation, mauvais index de paramètre, rotations incomplètes, update global; remplacé. |
| ImpMessage | Objet réutilisé, fermeture de ressources incomplète, absence d'autorisation; remplacé. |
| ImpNotif | Compteur global sans destinataire et état notification jamais clairement géré; remplacé par compteur de messages reçus. |
| LoginAdmin | État mutable partagé et authentification en clair; remplacé. |
| Singleton | Secrets et connexion globale; remplacé. |
| Test | Test manuel sans assertions; remplacé par JUnit. |
| Absence | Nom d'employé au lieu de relation stable; schéma normalisé. |
| Admin | Modèle redondant avec les employés ayant un rôle admin; unifié. |
| CaseTab | Cellule avec nom seulement, sans date ni FK; remplacée. |
| Employe | Mot de passe dans toString, salaire texte et relations sans gestion; schéma typé. |
| EmployePK | Clé composite non utilisée par les requêtes; retirée. |
| Historique_des_absence | Identifiant récursif sans données d'historique; retiré, aucun historique annoncé. |
| Message | Liste relationnelle non initialisée, accesseurs atypiques; remplacé par lignes JDBC. |
| Notif | Modèle simple, mais logique globale incorrecte; retiré. |
| Poste | Deux variantes d'ID et listes non initialisées; poste conservé comme libellé. |
| Login_Servlet | Logging de credentials, mauvaise gestion des erreurs et état partagé; remplacé. |
| Servlet | Syntaxe invalide, branches incohérentes, validations absentes, récursion; remplacé. |
| package-info | Déclaration de package seulement; retirée. |

## Lecture des pages JSP

| Page | Décision |
| --- | --- |
| login.jsp | Remplacée par login protégé par CSRF avec erreur visible. |
| home.jsp | Remplacée par dashboard avec compteurs issus de la base. |
| user.jsp | Remplacée par profile, mot de passe modifiable après vérification. |
| icons.jsp | Page employés issue d'un template d'icônes; remplacée par CRUD dédié. |
| modifierEmploye.jsp | Remplacée par formulaire d'édition commun aux employés. |
| tables.jsp | Remplacée par liste des absences avec relation stable à l'employé. |
| notifications.jsp | Remplacée par boîte de réception sans formulaires parasites. |
| sendMsg.jsp | Remplacée par formulaire de composition. |
| respMsg.jsp | Remplacée par réponse contrôlée par propriété du message. |
| typography.jsp | Planning incomplet; remplacé par planning de semaine. |
| test.jsp | Variante de planning avec liens vers un autre projet; retirée. |
| map.jsp | Démonstration cartographique sans rôle RH établi; retirée. |

## Choix de refonte

La livraison constitue une refonte des modules du ZIP, et non une retouche conservant tous les fichiers. Elle reste une application Java EE avec JSP, Servlets et JDBC. H2 permet une démonstration immédiate; une configuration MySQL est également fournie.

Sans schéma SQL ni spécification du planning original, il serait trompeur de promettre une restauration fidèle des anciennes données ou de l'algorithme métier. Le nouveau planning est volontairement explicite et ses limites sont documentées. La refonte ne comporte pas de gestion de paie, d'historique d'audit ni d'optimisation du personnel.

## Ce que le dépôt montrera

Une architecture compréhensible, une construction reproductible, des règles d'accès, des tests HTTP et des limites honnêtes apportent davantage au profil GitHub que 324 fichiers hétérogènes publiés tels quels. Le README doit rester fidèle au code et aux fonctionnalités testées.

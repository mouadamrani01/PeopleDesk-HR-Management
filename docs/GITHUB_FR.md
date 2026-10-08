# Publication avec VS Code

1. Extraire le ZIP et ouvrir le dossier `HR-Management-System-JavaEE` dans VS Code avec **File → Open Folder**.
2. Installer Java 17 et Maven. Dans **Terminal → New Terminal**, lancer `mvn clean verify`. Les tests doivent réussir.
3. Tester l'application avec la commande de lancement du README et vérifier les deux comptes de démonstration.
4. Ouvrir **Source Control** à gauche puis cliquer sur **Initialize Repository**.
5. Vérifier la liste des fichiers : `data/`, `target/` et `.env` ne doivent pas apparaître. Le `.gitignore` fourni les exclut.
6. Écrire le message de commit `Rebuild HR application with Maven and integration tests`, puis **Commit**.
7. Cliquer sur **Publish Branch / Publish to GitHub**, choisir `HR-Management-System-JavaEE`, puis le niveau de visibilité souhaité.
8. Sur GitHub, ouvrir **Actions** pour vérifier le build. Les premiers téléchargements Maven peuvent prendre plusieurs minutes.

Ne pas ajouter le ZIP, des données réelles de salariés, une base H2 ou le fichier `.env` au dépôt.

## Mettre le projet en valeur

- Ajouter une capture du tableau de bord et du planning avec des données fictives.
- Épingler le dépôt sur le profil GitHub.
- Ajouter les topics `java`, `java-ee`, `jsp`, `servlets`, `jdbc`, `hr-management`.
- Expliquer dans le README les corrections réalisées et les limites connues, comme cette version le fait.
- Savoir expliquer le trajet formulaire → servlet → repository → base → JSP.
- Présenter honnêtement l'origine du projet et la refonte assistée. Les compétences les plus convaincantes sont celles que tu peux démontrer et modifier toi-même.

La version livrée est une archive prête à importer. Aucun dépôt GitHub n'a été créé ou publié depuis cette session.

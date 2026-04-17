CREATE TABLE IF NOT EXISTS jeu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(400) NOT NULL,
    image VARCHAR(255) NOT NULL,
    interpretation_base VARCHAR(255) NOT NULL,
    max_participants INT NOT NULL DEFAULT 10,
    actif TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS participation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    jeu_id INT NOT NULL,
    nom_enfant VARCHAR(80) NOT NULL,
    age_enfant INT NOT NULL,
    choix_image VARCHAR(80) NOT NULL,
    interpretation VARCHAR(500) NOT NULL,
    date_participation DATETIME NOT NULL,
    CONSTRAINT fk_participation_jeu
        FOREIGN KEY (jeu_id) REFERENCES jeu(id)
        ON DELETE CASCADE
);

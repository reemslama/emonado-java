-- Schéma aligné avec ServiceJeu.java (ensureSchema).
-- Les tables jeu / image_carte / participation sont aussi créées au premier usage de ServiceJeu.
-- Ce fichier sert de référence ou d’import manuel si besoin.

USE emonado;

CREATE TABLE IF NOT EXISTS jeu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(400) NOT NULL,
    max_participants INT NOT NULL DEFAULT 3,
    actif TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS image_carte (
    id INT AUTO_INCREMENT PRIMARY KEY,
    jeu_id INT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    interpretation_psy VARCHAR(500) NOT NULL,
    CONSTRAINT fk_ic_jeu FOREIGN KEY (jeu_id) REFERENCES jeu(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS participation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL DEFAULT 0,
    jeu_id INT NOT NULL,
    image_choisie_id INT NOT NULL,
    resultat_psy VARCHAR(500) NOT NULL DEFAULT '',
    date_participation DATETIME NOT NULL,
    CONSTRAINT fk_participation_jeu FOREIGN KEY (jeu_id) REFERENCES jeu(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

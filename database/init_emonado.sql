-- EmoNado : base MySQL (port par defaut 3306 dans DataSource.java)
-- Exécuter une fois : mysql -u root -p < database/init_emonado.sql

CREATE DATABASE IF NOT EXISTS emonado
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE emonado;

-- Table utilisateurs (AuthService / inscription)
CREATE TABLE IF NOT EXISTS `user` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  email VARCHAR(191) NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(64) NOT NULL DEFAULT 'ROLE_PATIENT',
  telephone VARCHAR(40) NULL,
  sexe VARCHAR(20) NULL,
  dateNaissance DATE NULL,
  specialite VARCHAR(120) NULL,
  hasChild TINYINT(1) NOT NULL DEFAULT 0,
  avatar VARCHAR(500) NULL,
  face_id_image_path VARCHAR(500) NULL,
  UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comptes de test (mots de passe en clair, comme attendu par AuthService)
INSERT INTO `user` (nom, prenom, email, password, role, telephone, sexe, dateNaissance, specialite, hasChild)
VALUES
  ('Test', 'Nour', 'nour@esprit.tn', 'demo123', 'ROLE_PATIENT', '00000000', 'Femme', NULL, NULL, 0),
  ('Psy', 'Demo', 'psy@esprit.tn', 'demo123', 'ROLE_PSYCHOLOGUE', '00000000', 'Homme', NULL, 'Psychologie clinique', 0),
  ('Admin', 'Demo', 'admin@esprit.tn', 'demo123', 'ROLE_ADMIN', '00000000', 'Homme', NULL, NULL, 0)
ON DUPLICATE KEY UPDATE
  nom = VALUES(nom),
  prenom = VALUES(prenom),
  password = VALUES(password),
  role = VALUES(role);

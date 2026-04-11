CREATE TABLE IF NOT EXISTS journal (
    id INT PRIMARY KEY AUTO_INCREMENT,
    contenu TEXT NOT NULL,
    humeur VARCHAR(255) NOT NULL,
    date_creation DATETIME NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_journal_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS analyse_emotionnelle (
    id INT PRIMARY KEY AUTO_INCREMENT,
    journal_id INT NOT NULL UNIQUE,
    etat_emotionnel VARCHAR(80) NOT NULL,
    niveau VARCHAR(30) NOT NULL,
    declencheur TEXT NOT NULL,
    conseil TEXT NOT NULL,
    date_analyse DATETIME NOT NULL,
    CONSTRAINT fk_analyse_journal FOREIGN KEY (journal_id) REFERENCES journal(id) ON DELETE CASCADE
);

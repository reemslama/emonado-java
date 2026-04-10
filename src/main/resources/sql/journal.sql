CREATE TABLE IF NOT EXISTS journal (
    id INT PRIMARY KEY AUTO_INCREMENT,
    contenu TEXT NOT NULL,
    humeur VARCHAR(255) NOT NULL,
    date_creation DATETIME NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_journal_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

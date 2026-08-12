CREATE TABLE IF NOT EXISTS book (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    title           VARCHAR(200) NOT NULL,
    author          VARCHAR(100) NOT NULL,
    isbn            VARCHAR(20)  NOT NULL UNIQUE,
    category        VARCHAR(50),
    publisher       VARCHAR(100),
    page_count      INTEGER,
    description     TEXT,
    publication_date TEXT,
    created_at      TEXT DEFAULT (datetime('now', 'localtime')),
    updated_at      TEXT DEFAULT (datetime('now', 'localtime'))
);
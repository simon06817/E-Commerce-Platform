import sqlite3
import threading
import time
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

from app.config import config


class ConversationStore:
    """Persist recent conversations in a small embedded SQLite database."""

    def __init__(self, path: str, ttl_seconds: int, max_messages: int):
        self._path = Path(path)
        self._ttl_seconds = max(60, int(ttl_seconds))
        self._max_messages = max(2, int(max_messages))
        self._init_lock = threading.Lock()
        self._initialized = False

    @property
    def path(self) -> Path:
        return self._path

    def initialize(self) -> None:
        if self._initialized:
            return
        with self._init_lock:
            if self._initialized:
                return
            self._path.parent.mkdir(parents=True, exist_ok=True)
            with self._connect() as connection:
                connection.execute("PRAGMA journal_mode=WAL")
                connection.executescript(
                    """
                    CREATE TABLE IF NOT EXISTS conversation_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        thread_id TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        expires_at INTEGER NOT NULL
                    );

                    CREATE INDEX IF NOT EXISTS idx_conversation_lookup
                    ON conversation_messages(thread_id, id);

                    CREATE INDEX IF NOT EXISTS idx_conversation_expiry
                    ON conversation_messages(expires_at);
                    """
                )
            self._initialized = True

    def append_exchange(
        self,
        thread_id: str,
        question: str,
        answer: str,
        *,
        now: int | None = None,
    ) -> int:
        self.initialize()
        timestamp = int(time.time() if now is None else now)
        expires_at = timestamp + self._ttl_seconds
        with self._connect() as connection:
            connection.executemany(
                """
                INSERT INTO conversation_messages (
                    thread_id, role, content, created_at, expires_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
                (
                    (thread_id, "user", question, timestamp, expires_at),
                    (thread_id, "assistant", answer, timestamp, expires_at),
                ),
            )
            # Sliding TTL: continuing a conversation refreshes all its messages.
            connection.execute(
                """
                UPDATE conversation_messages
                SET expires_at = ?
                WHERE thread_id = ? AND expires_at > ?
                """,
                (expires_at, thread_id, timestamp),
            )
        return self.memory_size(thread_id, now=timestamp)

    def recent_messages(
        self,
        thread_id: str,
        *,
        limit: int | None = None,
        now: int | None = None,
    ) -> list[dict[str, str]]:
        self.initialize()
        timestamp = int(time.time() if now is None else now)
        message_limit = self._max_messages if limit is None else max(1, int(limit))
        with self._connect() as connection:
            rows = connection.execute(
                """
                SELECT role, content
                FROM (
                    SELECT id, role, content
                    FROM conversation_messages
                    WHERE thread_id = ? AND expires_at > ?
                    ORDER BY id DESC
                    LIMIT ?
                )
                ORDER BY id ASC
                """,
                (thread_id, timestamp, message_limit),
            ).fetchall()
        return [
            {"role": str(row["role"]), "content": str(row["content"])}
            for row in rows
        ]

    def memory_size(
        self,
        thread_id: str,
        *,
        now: int | None = None,
    ) -> int:
        self.initialize()
        timestamp = int(time.time() if now is None else now)
        with self._connect() as connection:
            row = connection.execute(
                """
                SELECT COUNT(*) AS message_count
                FROM (
                    SELECT id
                    FROM conversation_messages
                    WHERE thread_id = ? AND expires_at > ?
                    ORDER BY id DESC
                    LIMIT ?
                )
                """,
                (thread_id, timestamp, self._max_messages),
            ).fetchone()
        return int(row["message_count"] if row else 0)

    def active_conversation_count(self, *, now: int | None = None) -> int:
        self.initialize()
        timestamp = int(time.time() if now is None else now)
        with self._connect() as connection:
            row = connection.execute(
                """
                SELECT COUNT(DISTINCT thread_id) AS conversation_count
                FROM conversation_messages
                WHERE expires_at > ?
                """,
                (timestamp,),
            ).fetchone()
        return int(row["conversation_count"] if row else 0)

    def cleanup_expired(self, *, now: int | None = None) -> int:
        self.initialize()
        timestamp = int(time.time() if now is None else now)
        with self._connect() as connection:
            cursor = connection.execute(
                "DELETE FROM conversation_messages WHERE expires_at <= ?",
                (timestamp,),
            )
        return max(0, int(cursor.rowcount))

    def is_healthy(self) -> bool:
        try:
            self.initialize()
            with self._connect() as connection:
                connection.execute("SELECT 1").fetchone()
            return True
        except sqlite3.Error:
            return False

    @contextmanager
    def _connect(self) -> Iterator[sqlite3.Connection]:
        connection = sqlite3.connect(str(self._path), timeout=5)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA foreign_keys=ON")
        try:
            yield connection
            connection.commit()
        except Exception:
            connection.rollback()
            raise
        finally:
            connection.close()


conversation_store = ConversationStore(
    path=config.conversation_db_path,
    ttl_seconds=config.conversation_ttl_seconds,
    max_messages=config.conversation_history_limit,
)

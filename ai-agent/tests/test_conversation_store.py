import tempfile
import unittest
from pathlib import Path

from app.conversation_store import ConversationStore


class ConversationStoreTest(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = str(Path(self.temp_dir.name) / "conversation.db")
        self.store = ConversationStore(self.db_path, ttl_seconds=100, max_messages=4)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_exchange_is_available_after_store_recreation(self):
        self.store.append_exchange("BUYER:1", "你好", "您好", now=100)

        reopened = ConversationStore(self.db_path, ttl_seconds=100, max_messages=4)

        self.assertEqual(
            [
                {"role": "user", "content": "你好"},
                {"role": "assistant", "content": "您好"},
            ],
            reopened.recent_messages("BUYER:1", now=101),
        )

    def test_sliding_ttl_is_refreshed_by_the_last_message(self):
        self.store.append_exchange("BUYER:1", "第一轮", "回答一", now=100)
        self.store.append_exchange("BUYER:1", "第二轮", "回答二", now=150)

        self.assertEqual(
            0,
            self.store.cleanup_expired(now=200),
        )
        self.assertEqual(
            4,
            self.store.memory_size("BUYER:1", now=200),
        )
        self.assertEqual(
            4,
            self.store.cleanup_expired(now=251),
        )
        self.assertEqual([], self.store.recent_messages("BUYER:1", now=251))

    def test_history_limit_keeps_the_most_recent_messages(self):
        self.store.append_exchange("BUYER:1", "问题一", "回答一", now=100)
        self.store.append_exchange("BUYER:1", "问题二", "回答二", now=101)
        self.store.append_exchange("BUYER:1", "问题三", "回答三", now=102)

        messages = self.store.recent_messages("BUYER:1", now=103)

        self.assertEqual(4, len(messages))
        self.assertEqual("问题二", messages[0]["content"])
        self.assertEqual("回答三", messages[-1]["content"])


if __name__ == "__main__":
    unittest.main()

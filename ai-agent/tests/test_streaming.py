import json
import unittest
from unittest.mock import patch

from app.agent import stream_agent
from app.main import ChatRequest, _chat_event_stream


async def _fake_stream_agent(*_args, **_kwargs):
    yield "真实 "
    yield "流式"


class _Chunk:
    def __init__(self, content):
        self.content = content


class _FakeAgent:

    async def astream_events(self, *_args, **_kwargs):
        yield {
            "event": "on_chat_model_stream",
            "data": {"chunk": _Chunk("Hello ")},
        }
        yield {"event": "on_tool_start", "data": {}}
        yield {
            "event": "on_chat_model_stream",
            "data": {"chunk": _Chunk("world")},
        }


class StreamingTest(unittest.IsolatedAsyncioTestCase):

    async def test_structured_answer_is_sent_as_multiple_deltas(self):
        prepared = {
            "answer": "标题\n商品A",
            "_stream_chunks": ["标题", "商品A"],
            "user": {"id": 1, "role": "BUYER"},
            "product_ids": [8],
            "memory_size": 2,
            "pending_action": False,
        }
        with patch("app.main._handle_chat", return_value=prepared):
            events = [
                json.loads(line)
                async for line in _chat_event_stream(
                    ChatRequest(message="推荐电子产品"),
                    "Bearer test",
                )
            ]

        self.assertEqual(
            ["标题", "商品A"],
            [event["text"] for event in events if event["type"] == "delta"],
        )
        done = next(event for event in events if event["type"] == "done")
        self.assertNotIn("_stream_chunks", done["data"])

    async def test_stream_forwards_model_chunks_without_fake_slicing(self):
        context = {
            "_llm_context": {
                "user": {"id": 1, "role": "BUYER"},
                "thread_id": "BUYER:1",
                "question": "测试",
                "facts": [],
                "products": [],
                "authorization": "Bearer test",
            }
        }
        with (
            patch("app.main._handle_chat", return_value=context),
            patch("app.main.stream_agent", side_effect=_fake_stream_agent),
            patch("app.main.proposed_actions", return_value=[]),
            patch("app.main.remember_exchange", return_value=2),
        ):
            events = [
                json.loads(line)
                async for line in _chat_event_stream(
                    ChatRequest(message="测试"),
                    "Bearer test",
                )
            ]

        self.assertEqual(
            ["真实 ", "流式"],
            [event["text"] for event in events if event["type"] == "delta"],
        )
        done = next(event for event in events if event["type"] == "done")
        self.assertEqual("真实 流式", done["data"]["answer"])

    async def test_agent_stream_preserves_exact_model_chunks(self):
        with (
            patch("app.agent._prompt_messages", return_value=[]),
            patch("app.agent._agent", return_value=_FakeAgent()),
        ):
            chunks = [
                chunk
                async for chunk in stream_agent(
                    "BUYER:1",
                    "测试",
                    [],
                    "BUYER",
                )
            ]

        self.assertEqual(["Hello ", "world"], chunks)


if __name__ == "__main__":
    unittest.main()

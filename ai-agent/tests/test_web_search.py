import json
import unittest
from unittest.mock import patch

from app.config import config
from app.web_search import perform_web_search


class WebSearchTest(unittest.TestCase):

    def test_web_search_is_disabled_without_api_key(self):
        with patch.object(config, "tavily_api_key", ""):
            body = json.loads(perform_web_search("最新行情"))

        self.assertIn("error", body)
        self.assertIn("TAVILY_API_KEY", body["error"])

    def test_web_search_returns_compact_sources(self):
        class FakeResponse:
            def raise_for_status(self):
                return None

            def json(self):
                return {
                    "answer": "市场摘要",
                    "results": [
                        {
                            "title": "示例来源",
                            "url": "https://example.com/report",
                            "content": "示例内容",
                            "score": 0.9,
                        }
                    ],
                }

        with (
            patch.object(config, "tavily_api_key", "test-key"),
            patch("app.web_search.httpx.post", return_value=FakeResponse()) as post,
        ):
            body = json.loads(perform_web_search("最新行情", 3))

        self.assertEqual("市场摘要", body["answer"])
        self.assertEqual("tavily", body["source"])
        self.assertEqual("https://example.com/report", body["results"][0]["url"])
        self.assertEqual("test-key", post.call_args.kwargs["json"]["api_key"])


if __name__ == "__main__":
    unittest.main()

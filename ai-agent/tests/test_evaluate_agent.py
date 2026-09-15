import unittest

from scripts.evaluate_agent import evaluate_retrieval, ranked_product_ids, summarize


class EvaluateAgentTest(unittest.TestCase):

    def test_product_metrics_use_rank(self):
        case = {
            "id": "phone",
            "question": "phone",
            "expected_product_ids": [2],
            "expected_keywords": ["MacBook"],
        }
        result = {
            "knowledge": [],
            "products": [
                {"product_id": 1, "content": "iPhone"},
                {"product_id": 2, "content": "MacBook Pro"},
            ],
        }

        metrics = evaluate_retrieval(case, result, top_k=2)

        self.assertEqual([1, 2], ranked_product_ids(result, 2))
        self.assertEqual(1.0, metrics["hit_at_k"])
        self.assertEqual(1.0, metrics["recall_at_k"])
        self.assertEqual(0.5, metrics["reciprocal_rank"])
        self.assertEqual(1.0, metrics["keyword_recall"])

    def test_knowledge_keyword_recall(self):
        case = {
            "id": "stock",
            "question": "stock",
            "expected_product_ids": [],
            "expected_keywords": ["恢复", "库存"],
        }
        result = {
            "knowledge": [{"content": "取消订单后库存会恢复", "source": "faq.txt"}],
            "products": [],
        }

        metrics = evaluate_retrieval(case, result, top_k=1)

        self.assertIsNone(metrics["hit_at_k"])
        self.assertEqual(1.0, metrics["keyword_recall"])
        self.assertEqual(["faq.txt"], metrics["sources"])

    def test_summary_ignores_not_applicable_product_metrics(self):
        summary = summarize([{
            "hit_at_k": None,
            "recall_at_k": None,
            "reciprocal_rank": None,
            "keyword_recall": 1.0,
        }])

        self.assertEqual(1, summary["case_count"])
        self.assertEqual(0.0, summary["hit_at_k"])
        self.assertEqual(1.0, summary["keyword_recall"])


if __name__ == "__main__":
    unittest.main()

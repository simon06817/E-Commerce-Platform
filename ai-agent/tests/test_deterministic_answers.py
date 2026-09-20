import unittest
from unittest.mock import patch

from app.main import (
    _cart_count_answer,
    _is_cart_count_question,
    _is_recommendation_question,
    _recommendation_answer,
)


class DeterministicAnswerTest(unittest.TestCase):

    def test_cart_count_question_is_detected(self):
        self.assertTrue(_is_cart_count_question("我的购物车现在有几件商品"))
        self.assertFalse(_is_cart_count_question("购物车里有哪些商品"))

    def test_cart_count_is_summed_from_backend(self):
        with patch(
            "app.main._java_get",
            return_value=[
                {"productId": 8, "num": 2},
                {"productId": 70, "num": 1},
            ],
        ):
            answer, products = _cart_count_answer("Bearer test")

        self.assertIn("3 件商品", answer)
        self.assertIn("2 种", answer)
        self.assertEqual([8, 70], [item["product_id"] for item in products])

    def test_metric_recommendation_question_is_detected(self):
        self.assertTrue(
            _is_recommendation_question(
                "基于评论和销售数量帮我推荐一些电子产品"
            )
        )
        self.assertFalse(
            _is_recommendation_question("这款手机多少钱")
        )

    def test_recommendation_is_formatted_from_real_metrics(self):
        def fake_java_get(path, token=None):
            if path == "/api/categories":
                return [{"id": 1, "name": "电子产品"}]
            return [
                {
                    "id": 8,
                    "name": "机械键盘",
                    "price": 399,
                    "salesQuantity": 12,
                    "reviewCount": 3,
                    "averageRating": 4.7,
                    "sellerName": "Tech Store",
                }
            ]

        with patch("app.main._java_get", side_effect=fake_java_get):
            answer, products = _recommendation_answer(
                "基于评论和销售数量帮我推荐一些电子产品"
            )

        self.assertIn("机械键盘", answer)
        self.assertIn("销量 12 件", answer)
        self.assertIn("4.7/5", answer)
        self.assertEqual([8], [item["product_id"] for item in products])


if __name__ == "__main__":
    unittest.main()

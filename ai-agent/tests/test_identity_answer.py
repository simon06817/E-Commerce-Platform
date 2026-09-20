import unittest

from app.main import _identity_answer, _is_identity_question


class IdentityAnswerTest(unittest.TestCase):

    def test_identity_question_detection(self):
        self.assertTrue(_is_identity_question("我叫什么"))
        self.assertTrue(_is_identity_question("查看我的个人信息"))
        self.assertFalse(_is_identity_question("推荐几件电子产品"))

    def test_buyer_identity_is_not_assistant_identity(self):
        answer = _identity_answer(
            {"username": "buyer01", "nickname": "Simon"},
            "BUYER",
        )
        self.assertIn("不是您本人", answer)
        self.assertIn("buyer01", answer)
        self.assertIn("Simon", answer)
        self.assertNotIn("我是 Simon", answer)

    def test_seller_identity_includes_shop_name(self):
        answer = _identity_answer(
            {
                "username": "seller01",
                "nickname": "seller",
                "shopName": "Tech Store",
            },
            "SELLER",
        )
        self.assertIn("Tech Store", answer)


if __name__ == "__main__":
    unittest.main()

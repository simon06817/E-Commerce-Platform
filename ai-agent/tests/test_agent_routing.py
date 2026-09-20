import unittest

from app.main import _needs_api_catalog, _needs_product_retrieval, _needs_web_search


class AgentRoutingTest(unittest.TestCase):

    def test_identity_question_does_not_retrieve_products(self):
        self.assertFalse(_needs_product_retrieval("我叫什么", "BUYER"))
        self.assertFalse(_needs_product_retrieval("我的个人信息", "SELLER"))

    def test_catalog_question_retrieves_products(self):
        self.assertTrue(_needs_product_retrieval("推荐几件电子产品", "BUYER"))
        self.assertTrue(_needs_product_retrieval("手机价格是多少", "BUYER"))

    def test_mixed_order_and_product_question_retrieves_products(self):
        self.assertTrue(_needs_product_retrieval("我的订单里的商品评价怎么样", "BUYER"))

    def test_pure_account_question_skips_product_vectors(self):
        self.assertFalse(_needs_product_retrieval("查看本月收入统计", "SELLER"))
        self.assertFalse(_needs_product_retrieval("查询我的订单", "BUYER"))

    def test_add_to_cart_retrieves_product_context(self):
        self.assertTrue(
            _needs_product_retrieval(
                "帮我把shop7的遥控飞行无人机加入到我的购物车",
                "BUYER",
            )
        )

    def test_explicit_external_search_is_detected(self):
        self.assertTrue(_needs_web_search("上网搜索最新机械键盘行情"))
        self.assertFalse(_needs_web_search("查询我的购物车"))

    def test_api_catalog_is_loaded_only_for_capability_questions(self):
        self.assertTrue(_needs_api_catalog("这个项目有哪些后端接口"))
        self.assertFalse(_needs_api_catalog("查询我的购物车"))


if __name__ == "__main__":
    unittest.main()

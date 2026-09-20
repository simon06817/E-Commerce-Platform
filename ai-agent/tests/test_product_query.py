import unittest

from app.tools import split_product_query


class ProductQueryTest(unittest.TestCase):

    def test_combined_shop_and_product_is_split(self):
        product, shop = split_product_query("shop7的遥控飞行无人机")
        self.assertEqual("遥控飞行无人机", product)
        self.assertEqual("shop7", shop)

    def test_explicit_shop_parameter_is_kept(self):
        product, shop = split_product_query("遥控飞行无人机", "Shop 07")
        self.assertEqual("遥控飞行无人机", product)
        self.assertEqual("Shop 07", shop)

    def test_plain_product_keyword_is_unchanged(self):
        product, shop = split_product_query("T恤 白色")
        self.assertEqual("T恤 白色", product)
        self.assertIsNone(shop)


if __name__ == "__main__":
    unittest.main()

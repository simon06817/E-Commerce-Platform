import unittest

from app.api_catalog import api_catalog_facts, search_api_catalog


class ApiCatalogTest(unittest.TestCase):

    def test_buyer_catalog_excludes_seller_write_tools(self):
        entries = search_api_catalog("BUYER", "商品", limit=20)
        ids = {entry["id"] for entry in entries}

        self.assertIn("product.page", ids)
        self.assertIn("cart.add", ids)
        self.assertNotIn("seller.product.delete", ids)
        self.assertNotIn("admin.order.force-cancel", ids)

    def test_admin_catalog_can_find_order_management(self):
        entries = search_api_catalog("ADMIN", "强制关闭订单", limit=5)

        self.assertTrue(entries)
        self.assertEqual("admin.order.force-cancel", entries[0]["id"])

    def test_catalog_facts_do_not_contain_database_connection_info(self):
        facts = api_catalog_facts("查询订单接口", "BUYER")
        text = "\n".join(facts)

        self.assertIn("数据库访问由 Java 后端完成", text)
        self.assertNotIn("3306", text)
        self.assertNotIn("jdbc:", text)
        self.assertNotIn("password", text.lower())


if __name__ == "__main__":
    unittest.main()

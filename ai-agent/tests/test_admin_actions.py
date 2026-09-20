import unittest
from unittest.mock import patch

from app.main import _admin_direct_action, _admin_read_answer
from app.tools import build_tools


class AdminActionTest(unittest.TestCase):

    def test_explicit_force_cancel_is_deterministic(self):
        self.assertEqual(
            {"type": "force_cancel_order", "order_id": 31},
            _admin_direct_action("强制关闭订单 31"),
        )

    def test_explicit_category_status_change_is_deterministic(self):
        self.assertEqual(
            {
                "type": "set_category_status",
                "category_id": 5,
                "status": 0,
            },
            _admin_direct_action("停用分类 5"),
        )

    def test_admin_toolset_contains_management_tools(self):
        names = {tool.name for tool in build_tools("ADMIN")}
        self.assertIn("get_admin_buyers", names)
        self.assertIn("get_admin_sellers", names)
        self.assertIn("get_admin_categories", names)
        self.assertIn("delete_buyer", names)
        self.assertIn("create_category", names)
        self.assertIn("force_cancel_order", names)

    def test_admin_order_query_is_formatted_deterministically(self):
        with patch(
            "app.main._java_get",
            return_value={
                "records": [
                    {
                        "orderNo": "ORDER-1",
                        "buyerId": 1,
                        "totalAmount": 399,
                        "status": 1,
                    }
                ]
            },
        ):
            answer, products = _admin_read_answer("查看待发货订单", "Bearer test")

        self.assertIn("ORDER-1", answer)
        self.assertIn("待发货", answer)
        self.assertEqual([], products)


if __name__ == "__main__":
    unittest.main()

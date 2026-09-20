import unittest
from unittest.mock import patch

from app.main import (
    ChatRequest,
    _cart_product_query,
    _cart_quantity,
    _handle_chat,
    _pending_actions,
    _pending_actions_for,
    _normalize_actions,
    _set_pending_actions,
)


class PendingActionTest(unittest.TestCase):

    def tearDown(self):
        _pending_actions.clear()

    def test_pending_action_expires(self):
        with patch("app.main.time.monotonic", return_value=100.0):
            _set_pending_actions("BUYER:1", [{"type": "cancel_order"}])

        with patch("app.main.time.monotonic", return_value=401.0):
            self.assertEqual([], _pending_actions_for("BUYER:1"))
        self.assertNotIn("BUYER:1", _pending_actions)

    def test_unexpired_pending_action_is_returned(self):
        with patch("app.main.time.monotonic", return_value=100.0):
            _set_pending_actions("BUYER:1", [{"type": "add_to_cart"}])

        with patch("app.main.time.monotonic", return_value=200.0):
            self.assertEqual(
                [{"type": "add_to_cart"}],
                _pending_actions_for("BUYER:1"),
            )

    def test_add_to_cart_defaults_to_one_when_quantity_is_omitted(self):
        actions = [{"type": "add_to_cart", "product_id": 8, "num": 2}]

        _normalize_actions("把 Tech Store 的机械键盘加入购物车", actions)

        self.assertEqual(1, actions[0]["num"])

    def test_add_to_cart_keeps_explicit_quantity(self):
        actions = [{"type": "add_to_cart", "product_id": 8, "num": 2}]

        _normalize_actions("把 Tech Store 的机械键盘加 2 个到购物车", actions)

        self.assertEqual(2, actions[0]["num"])

    def test_cart_product_query_extracts_store_and_product(self):
        self.assertEqual(
            "Tech Store的机械键盘",
            _cart_product_query(
                "帮我把Tech Store的机械键盘添加到购物车"
            ),
        )

    def test_cart_quantity_defaults_to_one(self):
        self.assertEqual(
            1,
            _cart_quantity("把Tech Store的机械键盘加入购物车"),
        )

    def test_cart_quantity_reads_explicit_number(self):
        self.assertEqual(
            2,
            _cart_quantity("把Tech Store的机械键盘加2个到购物车"),
        )

    def test_new_request_replaces_unresolved_pending_action(self):
        with patch("app.main.time.monotonic", return_value=100.0):
            _set_pending_actions(
                "BUYER:1",
                [{"type": "cancel_order", "order_id": 31}],
            )
            with (
                patch(
                    "app.main._current_user",
                    return_value={"id": 1, "role": "BUYER"},
                ),
                patch("app.main._retrieve_facts", return_value=([], [])),
                patch(
                    "app.main._resolve_cart_product",
                    return_value={
                        "product_id": 8,
                        "name": "机械键盘",
                        "quantity": 1,
                    },
                ),
                patch("app.main.execute_proposed_action") as execute_action,
            ):
                result = _handle_chat(
                    ChatRequest(message="把 Tech Store 的机械键盘加入购物车"),
                    "Bearer test",
                )

        execute_action.assert_not_called()
        self.assertIn("机械键盘", result["answer"])
        self.assertTrue(result["pending_action"])


if __name__ == "__main__":
    unittest.main()

import json
import unittest

from app.tools import _enrich_product_status


class ProductStatusTest(unittest.TestCase):

    def test_search_result_uses_listing_status_text(self):
        raw = json.dumps(
            {
                "code": 200,
                "data": {
                    "records": [
                        {"id": 92, "status": 1},
                        {"id": 93, "status": 0},
                    ]
                },
            }
        )

        body = json.loads(_enrich_product_status(raw))

        self.assertEqual(
            "上架",
            body["data"]["records"][0]["statusText"],
        )
        self.assertEqual(
            "下架",
            body["data"]["records"][1]["statusText"],
        )

    def test_product_detail_uses_listing_status_text(self):
        raw = json.dumps({"code": 200, "data": {"id": 70, "status": 1}})

        body = json.loads(_enrich_product_status(raw))

        self.assertEqual("上架", body["data"]["statusText"])


if __name__ == "__main__":
    unittest.main()

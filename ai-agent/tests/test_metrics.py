import unittest

from app import metrics


class MetricsTest(unittest.TestCase):

    def setUp(self):
        metrics.reset()

    def tearDown(self):
        metrics.reset()

    def test_counters_and_timers_are_aggregated(self):
        metrics.increment("agent.requests")
        metrics.increment("agent.requests")
        metrics.observe_ms("agent.llm_ms", 100)
        metrics.observe_ms("agent.llm_ms", 300)

        snapshot = metrics.snapshot()

        self.assertEqual(2, snapshot["counters"]["agent.requests"])
        self.assertEqual(2, snapshot["timers"]["agent.llm_ms"]["count"])
        self.assertEqual(200.0, snapshot["timers"]["agent.llm_ms"]["average_ms"])
        self.assertEqual(300.0, snapshot["timers"]["agent.llm_ms"]["max_ms"])


if __name__ == "__main__":
    unittest.main()

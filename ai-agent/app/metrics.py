import threading
import time
from collections import Counter, defaultdict
from contextlib import contextmanager

_lock = threading.Lock()
_counters: Counter[str] = Counter()
_timers: dict[str, dict[str, float | int]] = defaultdict(
    lambda: {"count": 0, "total_ms": 0.0, "max_ms": 0.0}
)


def increment(name: str, amount: int | float = 1) -> None:
    with _lock:
        _counters[name] += amount


def observe_ms(name: str, duration_ms: float) -> None:
    with _lock:
        timer = _timers[name]
        timer["count"] += 1
        timer["total_ms"] += max(0.0, duration_ms)
        timer["max_ms"] = max(float(timer["max_ms"]), duration_ms)


@contextmanager
def timer(name: str):
    started = time.perf_counter()
    try:
        yield
    finally:
        observe_ms(name, (time.perf_counter() - started) * 1000)


def snapshot() -> dict:
    with _lock:
        timers = {}
        for name, value in _timers.items():
            count = int(value["count"])
            total_ms = float(value["total_ms"])
            timers[name] = {
                "count": count,
                "total_ms": round(total_ms, 3),
                "average_ms": round(total_ms / count, 3) if count else 0.0,
                "max_ms": round(float(value["max_ms"]), 3),
            }
        return {
            "counters": dict(sorted(_counters.items())),
            "timers": dict(sorted(timers.items())),
        }


def reset() -> None:
    with _lock:
        _counters.clear()
        _timers.clear()

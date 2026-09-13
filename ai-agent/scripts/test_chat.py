import sys

import httpx

JAVA_API = "http://localhost:8080"
AI_API = "http://localhost:8000"


def main():
    """先登录 Java 后端，再携带 JWT 调用 AI 服务。"""
    message = " ".join(sys.argv[1:]) or "list some products"
    login = httpx.post(
        f"{JAVA_API}/api/auth/login",
        json={"role": "BUYER", "username": "buyer01", "password": "123456"},
        timeout=20,
    )
    login.raise_for_status()
    token = login.json()["data"]["token"]

    response = httpx.post(
        f"{AI_API}/chat",
        headers={"Authorization": f"Bearer {token}"},
        json={"message": message},
        timeout=180,
    )
    response.raise_for_status()
    print(response.json()["answer"])


if __name__ == "__main__":
    main()

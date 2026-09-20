import os
from pathlib import Path

from dotenv import load_dotenv

# 读取 ai-agent/.env，便于本地覆盖 Ollama 与 Java 服务地址
load_dotenv()


class Config:
    """集中管理 AI 服务运行时配置。"""

    # 本地 Ollama 对话模型与 embedding 模型
    ollama_base_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    ollama_model = os.getenv("OLLAMA_MODEL", "qwen3.5:0.8b")
    embedding_model = os.getenv("EMBEDDING_MODEL", "quentinz/bge-small-zh-v1.5:q4_0")
    # Java 电商后端地址，Agent 工具通过它调用商品/分类等接口
    java_api_base_url = os.getenv("JAVA_API_BASE_URL", "http://localhost:8080")
    # Tavily 外部检索。没有配置 Key 时联网工具自动不可用。
    tavily_api_key = os.getenv("TAVILY_API_KEY", "").strip()
    tavily_max_results = int(os.getenv("TAVILY_MAX_RESULTS", "5"))
    tavily_search_depth = os.getenv("TAVILY_SEARCH_DEPTH", "basic").strip()
    tavily_timeout_seconds = int(os.getenv("TAVILY_TIMEOUT_SECONDS", "20"))
    # RAG 知识库原文与向量库目录
    vector_dir = os.getenv("VECTOR_DIR", "data/vector")
    kb_dir = os.getenv("KB_DIR", "data/kb")
    # 两个独立集合：静态知识（FAQ/政策）与商品描述
    kb_collection = os.getenv("KB_COLLECTION", "ecommerce_kb")
    product_collection = os.getenv("PRODUCT_COLLECTION", "ecommerce_products")
    # 结构化后端 API 能力目录，不包含数据库连接信息。
    api_catalog_path = os.getenv(
        "API_CATALOG_PATH",
        str(Path(__file__).resolve().parents[1] / "data" / "api_catalog.json"),
    )
    # 商品同步接口的分页大小
    product_sync_page_size = int(os.getenv("PRODUCT_SYNC_PAGE_SIZE", "50"))
    # 允许直接访问 AI 服务的前端来源，多个用逗号分隔
    cors_allowed_origins = os.getenv(
        "CORS_ALLOWED_ORIGINS",
        "http://localhost:5173,http://localhost:3000",
    )

    @property
    def cors_origins(self) -> list[str]:
        return [origin.strip() for origin in self.cors_allowed_origins.split(",") if origin.strip()]

    @property
    def tavily_enabled(self) -> bool:
        return bool(self.tavily_api_key)


config = Config()

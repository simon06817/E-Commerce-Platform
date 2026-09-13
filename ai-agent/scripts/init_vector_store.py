import sys
from pathlib import Path

# 允许直接执行 scripts 下的脚本时导入项目根目录下的 app 包
sys.path.append(str(Path(__file__).resolve().parents[1]))

from app.rag import build_vector_store


def main():
    """初始化静态知识向量库（FAQ、政策、指南）。"""
    store = build_vector_store()
    print(f"vector store initialized at data/vector, count={store._collection.count()}")


if __name__ == "__main__":
    main()

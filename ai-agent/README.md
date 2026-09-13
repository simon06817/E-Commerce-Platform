# ai-agent

Python AI agent service for the E-Commerce project.

## Features

- FastAPI HTTP entry
- Local Ollama model via LangChain
- RAG over product/FAQ knowledge base (Chroma)
- Agent tools that call the Java E-Commerce APIs
- JWT identity check through the Java `/api/auth/me` endpoint
- In-process session memory (one session per user, last 20 messages, reset on restart)
- Deterministic pre-retrieval: knowledge + product description + live price/stock + reviews
- Authenticated buyer tools: cart/order read tools and `add_to_cart` / `cancel_order`
- Write actions use two-phase confirmation: the model proposes, the user confirms, then Java executes
- CORS allows the configured frontend origins (default `5173` and `3000`)

## Setup

```bash
cd ai-agent
python -m venv .venv
.venv/Scripts/activate
pip install -r requirements.txt
```

## Configure

Copy `.env.example` to `.env` and fill values:

```text
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3:0.8b
EMBEDDING_MODEL=bge-m3
JAVA_API_BASE_URL=http://localhost:8080
```

Make sure the Ollama embedding model is available:

```bash
ollama pull bge-m3
```

## Run

```bash
uvicorn app.main:app --reload --port 8000
```

## Build knowledge base

Put FAQ/policy/guide text files under `data/kb/`, then run:

```bash
python scripts/init_vector_store.py
```

Sync product descriptions from the running Java API into a separate vector collection:

```bash
python scripts/sync_products_to_vector.py
```

Product price, stock and availability are always read through Java tools at
request time; the vector store only keeps semantic descriptions.

## Test

```bash
python scripts/test_chat.py "find me a phone under 6000"
```

The test script logs in as `buyer01`, then sends the JWT to the AI service.

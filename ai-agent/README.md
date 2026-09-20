# ai-agent

Python AI agent service for the E-Commerce project.

## Features

- FastAPI HTTP entry
- Local Ollama model via LangChain
- RAG over product/FAQ knowledge base (Chroma)
- Optional Tavily web search for external, real-time information
- Role-filtered backend API capability catalog loaded from `data/api_catalog.json`
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
OLLAMA_MODEL=qwen3.5:0.8b
EMBEDDING_MODEL=quentinz/bge-small-zh-v1.5:q4_0
JAVA_API_BASE_URL=http://localhost:8080
TAVILY_API_KEY=
TAVILY_MAX_RESULTS=5
TAVILY_SEARCH_DEPTH=basic
TAVILY_TIMEOUT_SECONDS=20
API_CATALOG_PATH=data/api_catalog.json
```

Make sure the Ollama embedding model is available:

```bash
ollama pull qwen3.5:0.8b
ollama pull quentinz/bge-small-zh-v1.5:q4_0
```

## Run

```bash
uvicorn app.main:app --reload --port 8000
```

## Docker

The root `docker-compose.yml` builds this directory as the `agent` service:

```bash
docker compose up -d --build agent
```

Inside Docker the service uses:

```text
JAVA_API_BASE_URL=http://java:8080
OLLAMA_BASE_URL=http://ollama:11434
```

`ai-agent/.env` is loaded at runtime through `env_file` and is not copied into
the image. Keep `TAVILY_API_KEY` in that local file. The Compose project also
starts Ollama and runs an initialization container that downloads the configured
chat and embedding models into the `ollama-data` volume.

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

## Routing and security

The Agent keeps knowledge retrieval and business execution separate:

- Buyer/seller/admin account data is queried through authenticated Java tools.
- Product descriptions, FAQ and policies are retrieved from Chroma.
- `get_api_catalog` returns only the backend capabilities allowed for the current role.
- `web_search` uses Tavily only for external or real-time information.
- `GET /metrics` returns lightweight in-process routing, tool and latency counters.
- Cart, order, refund, product and admin write actions still use two-phase confirmation.
- Database host, port, username, password and JDBC URL are never added to the
  model prompt or Chroma.

If `TAVILY_API_KEY` is empty, the web search tool stays disabled and reports that
external search is unavailable. External search results are treated as
untrusted reference material and cannot trigger write actions.

## Test

```bash
python scripts/test_chat.py "find me a phone under 6000"
```

The test script logs in as `buyer01`, then sends the JWT to the AI service.

## Evaluation

Run deterministic retrieval metrics without calling the Java backend:

```bash
python scripts/evaluate_agent.py --mode retrieval --top-k 3
```

Run retrieval plus local Ollama answer evaluation:

```bash
python scripts/evaluate_agent.py --mode both --top-k 3
```

The report includes product `Hit@K`, `Recall@K`, `MRR`, keyword recall,
retrieved sources and optional answer keyword coverage. The dataset lives in
`data/eval/questions.jsonl`.

Run the metric unit tests without Ollama or Chroma:

```bash
python -m unittest discover -s tests -v
```

Current agent unit tests: `42` tests, all passing.

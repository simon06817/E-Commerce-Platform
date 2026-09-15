# Agent Evaluation Set

`questions.jsonl` contains deterministic retrieval evaluation cases. Each line
has:

- `id`: stable case id
- `question`: user question sent to retrieval
- `type`: `product` or `knowledge`
- `expected_product_ids`: relevant product ids, or an empty list
- `expected_keywords`: facts that should appear in retrieval or an answer

Run retrieval metrics:

```bash
python scripts/evaluate_agent.py --mode retrieval --top-k 3
```

Run retrieval plus local Ollama answer generation:

```bash
python scripts/evaluate_agent.py --mode both --top-k 3
```

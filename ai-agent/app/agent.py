from typing import TypedDict

from langchain_core.messages import AIMessage, HumanMessage
from langchain_ollama import ChatOllama
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.prebuilt import create_react_agent

from app.config import config
from app.tools import build_tools

# 传给模型的最近消息数，避免上下文无限增长。
MAX_MEMORY_MESSAGES = 20

_ACCOUNT_KEYWORDS = (
    "order", "cart", "return", "notification", "ship", "statistics",
    "订单", "购物车", "退货", "通知", "发货", "统计", "取消", "支付",
)


class AgentState(TypedDict, total=False):
    """State passed between LangGraph nodes and persisted by the checkpointer."""

    messages: list
    question: str
    facts: list[str]
    role: str
    intent: str
    answer: str


def _model() -> ChatOllama:
    """Create the local Ollama chat model."""
    return ChatOllama(
        model=config.ollama_model,
        base_url=config.ollama_base_url,
        temperature=0.2,
    )


def _extract_ai_answer(result: dict) -> str:
    """Pick the last non-empty AI message, skipping tool payloads."""
    for message in reversed(result.get("messages", [])):
        message_type = (getattr(message, "type", "") or message.__class__.__name__).lower()
        if message_type not in ("ai", "aimessage"):
            continue
        content = getattr(message, "content", "")
        if isinstance(content, list):
            content = " ".join(
                block.get("text", "")
                for block in content
                if isinstance(block, dict) and block.get("text")
            )
        if isinstance(content, str) and content.strip():
            return content.strip()
    return "I could not generate an answer from the available facts."


def _build_prompt(question: str, facts: list[str], intent: str) -> str:
    """Give the model facts plus intent-specific tool instructions."""
    fact_text = "\n".join(facts) if facts else "No retrieved facts."
    if intent == "account":
        extra = (
            "This is an account-related question. Use the authenticated tools for "
            "orders, cart, returns and notifications. Never invent account data."
        )
    else:
        extra = (
            "This is a catalog question. Use product, category and review facts. "
            "Never invent price, stock or review content."
        )
    return (
        "You are an e-commerce assistant. Answer only from the Facts below and from "
        f"successful tool results. {extra}\n\n"
        f"Facts:\n{fact_text}\n\n"
        f"User question: {question}"
    )


def _trim_node(state: AgentState) -> dict:
    """Keep only the most recent messages before calling the model."""
    return {"messages": state.get("messages", [])[-MAX_MEMORY_MESSAGES:]}


def _classify_node(state: AgentState) -> dict:
    """Deterministic intent routing keeps the small model reliable."""
    question = state.get("question", "").lower()
    intent = "account" if any(keyword in question for keyword in _ACCOUNT_KEYWORDS) else "catalog"
    return {"intent": intent}


def _agent_node(state: AgentState) -> dict:
    """Run the ReAct agent with role-scoped tools and persist the exchange."""
    intent = state.get("intent", "catalog")
    prompt = _build_prompt(state.get("question", ""), state.get("facts", []), intent)
    agent = create_react_agent(_model(), build_tools(state.get("role")))
    result = agent.invoke({
        "messages": state.get("messages", []) + [("user", prompt)]
    })
    answer = _extract_ai_answer(result)
    messages = state.get("messages", []) + [
        HumanMessage(content=state.get("question", "")),
        AIMessage(content=answer),
    ]
    return {"messages": messages[-MAX_MEMORY_MESSAGES:], "answer": answer}


def _build_graph():
    graph = StateGraph(AgentState)
    graph.add_node("trim", _trim_node)
    graph.add_node("classify", _classify_node)
    graph.add_node("agent", _agent_node)
    graph.add_edge(START, "trim")
    graph.add_edge("trim", "classify")
    graph.add_edge("classify", "agent")
    graph.add_edge("agent", END)
    return graph.compile(checkpointer=MemorySaver())


_compiled_graph = _build_graph()


def run_agent(thread_id: str, question: str, facts: list[str], role: str | None) -> dict:
    """Invoke the graph with one thread per role:userId."""
    state = _compiled_graph.invoke(
        {"question": question, "facts": facts, "role": role},
        config={"configurable": {"thread_id": thread_id}},
    )
    return {
        "answer": state.get("answer", "I could not generate an answer from the available facts."),
        "memory_size": len(state.get("messages", [])),
    }

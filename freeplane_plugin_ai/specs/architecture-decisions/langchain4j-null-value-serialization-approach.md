# LangChain4j null value serialization approach

Date: 2025-12-29
Status: Accepted

## Context
LangChain4j tools serialize responses and deserialize tool arguments using its internal JSON codec. We considered adding a custom JSON codec to omit null values from tool responses and to control serialization behavior.

## Decision
Use the default LangChain4j JSON codec and accept null fields in tool responses. Do not introduce a custom JsonCodecFactory at this time.

## Consequences
- Tool responses include explicit null fields when a value is absent.
- Missing fields in tool inputs deserialize to null for reference types and default values for primitive types.
- Strict unknown property handling remains in place, which protects against hallucinated fields.
- If response size or null noise becomes a problem, revisit the decision and design a custom codec that preserves current safety settings.

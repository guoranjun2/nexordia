# Freeplane AI Plugin

Overview
- Provides tool method stubs and configuration helpers for large language model integration using LangChain4j.
- Focuses on structured tool calls and a chat workflow that can be wired to Freeplane actions later.

OpenRouter key setup
- Set `ai_openrouter_key=<openrouter-api-key>`
- Optional service address override:
  `ai_openrouter_service_address=<openrouter-service-address>`

Local model setup with Ollama
- Start Ollama and make sure the model is pulled.
- Set `ai_ollama_service_address=<ollama-service-address>`
- Optional remote token:
  `ai_ollama_api_key=<ollama-api-key>`
  (sent as `Authorization: Bearer <token>` only when non-empty)

MCP server authentication
- Enable MCP server with `ai_mcp_server_enabled=true`.
- Configure token in `ai_mcp_token`.
- MCP clients can authenticate with either:
  - `Authorization: Bearer <token>` (recommended)
  - `X-Freeplane-MCP-Token: <token>` (legacy compatibility)
- If both headers are sent, token values must match.

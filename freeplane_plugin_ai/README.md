# Freeplane AI Plugin

Overview
- Provides tool method stubs and configuration helpers for large language model integration using LangChain4j.
- Focuses on structured tool calls and a chat workflow that can be wired to Freeplane actions later.

OpenRouter key setup
- Set the OpenRouter key with one of the following options:
  - Environment variable: `FREEPLANE_OPENROUTER_KEY`
  - System property: `freeplane.openrouter.key`
- Set the provider property: `freeplane.ai.provider=openrouter`
- Set the model name property: `freeplane.ai.model.name=<model-name>`
- Optional service address override: `freeplane.ai.service.address=<openrouter-service-address>`

Local model setup with Ollama
- Start Ollama and make sure the model is pulled.
- Set the provider property: `freeplane.ai.provider=ollama`
- Set the service address property: `freeplane.ai.service.address=<ollama-service-address>`
- Set the model name property: `freeplane.ai.model.name=<model-name>`

# Task: Add llm provider model preferences

## Scope
- Allow provider-specific language model lists to be configured in preferences.
- Use a text area per provider, accepting entries separated by commas or line breaks.
- Apply the lists as white list filters with `*` and `?` wildcards.
- Keep separate lists per provider and use current hard-coded lists as default property values.

## Motivation
- Current model lists are hard-coded or fetched, which makes it difficult to adjust or limit models per provider without a code change.

## Research
- Current model list configuration:
- AIModelCatalog uses OPENROUTER_MODEL_ALLOWLIST for OpenRouter models.
- AIModelCatalog uses GEMINI_MODELS for Gemini models; there is no Gemini model fetch.
- Ollama models are fetched from the configured service address when ai_use_ollama is true.
- preferences.xml exposes provider addresses, provider keys, ai_use_ollama, and other toggles, but has no provider model list entries.
- defaults.properties defines default provider service addresses and ai_use_ollama; it does not define model lists.
- AIProviderConfiguration reads ai_selected_model plus legacy ai_provider_name and ai_model_name properties.

## Design
- Preferences add a model list entry per provider, exposed as a text area field.
- Parsing rules:
- Split on commas or line breaks.
- Trim whitespace around entries.
- Ignore empty entries.
- Treat entries as white list patterns supporting `*` and `?` wildcards.
- Behavior:
- Use the per-provider list as a white list filter on the provider model list.
- For OpenRouter and Ollama, fetch the provider model list and filter it with the preference white list.
- For Gemini, fetch the provider model list from the Gemini models endpoint using the configured Gemini key and filter it with the preference white list.
- Limit Gemini to models that support text generation (supported generation methods include `generateContent`).
- Cache Gemini fetches using the same refresh interval as the other providers.
- When the provider preference is empty, fall back to the full provider model list.
- Keep existing provider enablement checks and selection storage unchanged.
- Defaults:
- Populate the new preference defaults with the current hard-coded OpenRouter and Gemini lists.
- Default Ollama list stays empty so fetched models are unrestricted unless configured.

## Test specification
- Verify parsing accepts comma and line break separation.
- Verify wildcard patterns `*` and `?` filter the provider model list.
- Verify empty or whitespace-only input falls back to current provider lists.
- Verify provider list preferences do not change selection storage behavior.

## Modified files
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIProviderConfiguration.java
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIModelCatalog.java
- freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties
- freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml
- freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/AIModelCatalogTest.java

## Subtasks
- ### Subtask: Rebuild model selector when allowlist preferences change
  - **Status:** Planning
  - **Scope**
    - Rebuild the model selector when any provider model allowlist preference changes.
  - **Motivation**
    - Model selection should reflect the latest allowlist without restarting.
  - **Research**
    - Entry point: `.addPropertyChangeListenerAndPropagate(modelContextProtocolServer);`
  - **Design**
    - Determine where model selection refresh is triggered today, and add a property change listener that rebuilds the model list when allowlist preferences are updated.
  - **Test specification**
    - Verify updating a provider allowlist refreshes the model selector.
  - **Modified files**
    - None yet.

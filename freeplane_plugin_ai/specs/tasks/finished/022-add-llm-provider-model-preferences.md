# Task: Add llm provider model preferences

## Scope
- Allow provider-specific language model lists to be configured in preferences.
- Use a text area per provider, accepting entries separated by commas or line breaks.
- Apply the lists as white list filters with `*` and `?` wildcards for OpenRouter and Ollama.
- Use the Gemini list as the source of truth for model names.
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
- Treat OpenRouter and Ollama entries as white list patterns supporting `*` and `?` wildcards.
- Treat the Gemini list entries as literal model names and show them in the selector even if they are placeholders.
- Behavior:
- Use the per-provider list as a white list filter on the provider model list.
- For OpenRouter and Ollama, fetch the provider model list and filter it with the preference white list.
- For Gemini, use the configured list as the sole model list without fetching.
- When the provider preference is empty, fall back to the full provider model list.
- Keep existing provider enablement checks and selection storage unchanged.
- Defaults:
- Populate the new preference defaults with the current hard-coded OpenRouter and Gemini lists.
- Default Ollama list stays empty so fetched models are unrestricted unless configured.

## Test specification
- Verify parsing accepts comma and line break separation.
- Verify wildcard patterns `*` and `?` filter the OpenRouter and Ollama model lists.
- Verify Gemini model list entries are shown without fetching.
- Verify empty or whitespace-only input falls back to current provider lists.
- Verify provider list preferences do not change selection storage behavior.

## Modified files
- See subtasks.

## Subtasks
- ### Subtask: Provider allowlist preferences and Gemini model list
  - **Status:** Finished
  - **Scope**
    - Add provider allowlist preferences with wildcard filtering.
    - Use the Gemini model list preference as the sole source of Gemini models.
  - **Motivation**
    - Provider model lists should be configurable and stay aligned with available models.
  - **Research**
    - AIModelCatalog previously used static OpenRouter and Gemini lists, and fetched Ollama models.
  - **Design**
    - Fetch provider model lists and apply allowlist filtering per provider.
    - Build Gemini model descriptors directly from the configured list without fetching.
  - **Test specification**
    - Verify allowlist filtering, Gemini list parsing, and updated OpenRouter parsing behavior.
  - **Modified files**
    - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIProviderConfiguration.java
    - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIModelCatalog.java
    - freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties
    - freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/preferences.xml
    - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/AIModelCatalogTest.java
- ### Subtask: Rebuild model selector when allowlist preferences change
  - **Status:** Implementation Review
  - **Scope**
    - Rebuild the model selector when any provider model list preference changes.
  - **Motivation**
    - Model selection should reflect the latest model list without restarting.
  - **Research**
    - Entry point: `.addPropertyChangeListenerAndPropagate(modelContextProtocolServer);`
    - Comparable pattern: `AntiAliasingConfigurator` registers a property change listener via `ResourceController.getResourceController().addPropertyChangeListenerAndPropagate(...)` and updates runtime behavior when `antialias` changes.
    - Model selector lifecycle: `AIChatPanel` constructs `AIModelSelectionController` and calls `loadInitialModelSelectionList()` once during panel initialization; there is no listener that rebuilds the list when preferences change.
  - **Design**
    - Add a property change listener in `AIChatPanel` (or a small helper owned by it) using `ResourceController.addPropertyChangeListener(...)` to avoid an immediate refresh on registration.
    - When any of the model list preference keys changes (`ai_openrouter_model_allowlist`, `ai_gemini_model_list`, `ai_ollama_model_allowlist`), call `modelSelectionController.loadInitialModelSelectionList()` to rebuild the model selector with refreshed provider lists.
    - Keep the refresh scoped to allowlist changes only to avoid unintended reloads from unrelated preference updates.
  - **Test specification**
    - Verify updating a provider model list refreshes the model selector.
  - **Modified files**
    - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatPanel.java

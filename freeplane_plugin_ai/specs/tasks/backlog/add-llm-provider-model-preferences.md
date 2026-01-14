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
- AIModelCatalog uses GEMINI_MODELS for Gemini models.
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
- Fall back to current static or fetched lists when the provider preference is empty.
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
- None yet.

## Subtasks
- None.

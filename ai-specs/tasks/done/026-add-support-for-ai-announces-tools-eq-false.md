Analyze the Freeplane Plugin AI codebase to propose implementation options for adding
a new property "ai_announces_tools" that controls the scope of information sent to the LLM.

## Current Behavior
The plugin constructs API requests that include:
1. System message with guidance on tool usage and response formatting
2. User prompt and conversation history
3. Complete tool definitions (function schemas, descriptions, parameters)

Example current flow:
- MessageBuilder constructs system guidance including tool call instructions
- Tool definitions are added to the request payload
- The full message set is sent to the LLM (currently Ollama/Claude)

## Desired Behavior
When "ai_announces_tools" property is set to false:
- Send ONLY: user prompt and essential conversation history
- EXCLUDE:
    - Tool definitions/schemas
    - Tool usage guidance and instructions
    - Tool call request wrapper guidance
    - Map selection guidance
- The LLM should operate without awareness of available tools
- Other system guidance (profile control, markdown response)
  should remain if not tool-related

## Implementation Analysis Requirements

### 1. Request Construction Flow
- Identify where tool definitions are added to the request payload
- Identify where system message components are built
- Determine which components are conditionally includable
- Map the flow from configuration property to final API request

### 2. System Message Decoupling
- Analyze which parts of the system message should be excluded when "ai_announces_tools" is false
- Propose options for conditionally building system guidance
- Consider impact on response formatting and profile management

### 3. Tool Definition Handling
- Where are tools serialized into the request?
- How should tool definitions be conditionally excluded?
- Should tool availability be checked before serialization or removed after?

### 4. Implementation Approaches
Propose at least 2-3 approaches considering:
- **Option 1**: Conditional building at message composition stage
- **Option 2**: Conditional filtering at request serialization stage
- **Option 3**: Alternative approaches you identify
- For each: evaluate code clarity, performance, testability, maintainability

### 5. Configuration Integration
The standard pattern used in this codebase is:
```java
resourceController.getProperty("ai_announces_tools")
```

This approach:
- Follows existing configuration patterns (see AIProviderConfiguration for examples)
- Allows the property to be changed at any time by users via Groovy scripting in Freeplane
- Requires no additional configuration files or UI changes
- Can be toggled dynamically without restarting

Implementation considerations:
- Define the property constant following the naming convention (e.g., `AI_ANNOUNCES_TOOLS_PROPERTY`)
- The property can be queried per-request without caching concerns
- Default behavior: announces-tools mode should be "opt-out" (default: true)

### 6. Backward Compatibility & Edge Cases
- Ensure existing behavior is unchanged when property is absent or true
- What happens if "ai_announces_tools" is changed mid-conversation?
- How should tool call results be handled when announcesTools is false?
- Should there be validation or warnings if tools are referenced but unavailable?

### 7. Recommended Approach
Based on analysis:
- Recommend the implementation strategy with strongest rationale
- Provide pseudocode or architectural overview
- Highlight any breaking changes or considerations
- Suggest implementation order/phases if applicable
- Identify which components need modification and in what order

## Context
- Java SDK version 8
- System guidance is currently hard-coded in MessageBuilder
- Configuration uses ResourceController.getProperty() pattern
- Tools are passed to the LLM API as function definitions
- Properties can be changed dynamically at runtime via user scripting

---

# Plan: Implement `ai_announces_tools` property

## Context
Add a `ai_announces_tools` boolean property (read via `ResourceController.getProperty("ai_announces_tools")`) that strips tool-related content from requests. When false:
- **System message**: exclude `MAP_SELECTION_GUIDANCE` and `TOOL_CALL_REQUEST_WRAPPER_GUIDANCE`
- **Request payload**: exclude all tool definitions (no `tools` array sent to LLM)

The LangChain4j `systemMessageProvider` is called per-request (already dynamic). Tool definitions are baked into `AiServices` at construction — changing them requires rebuilding the `AIAssistant` proxy.

## Files to modify

### 1. `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/MessageBuilder.java`

Add overload `buildForChat(boolean announcesTools)` with the real logic. Old `buildForChat()` becomes a delegate that reads the property:

```java
private static final String AI_ANNOUNCES_TOOLS_PROPERTY = "ai_announces_tools";

public String buildForChat() {
    ResourceController rc = ResourceController.getResourceController();
    boolean announcesTools = "true".equalsIgnoreCase(rc.getProperty(AI_ANNOUNCES_TOOLS_PROPERTY, "true"));
    return buildForChat(announcesTools);
}

public String buildForChat(boolean announcesTools) {
    String message = messageTextProvider.getMessageText();
    String guidance;
    if (announcesTools) {
        guidance = PROFILE_CONTROL_GUIDANCE + "\n\n" + MARKDOWN_RESPONSE_GUIDANCE;
    } else {
        guidance = MAP_SELECTION_GUIDANCE + "\n\n" + PROFILE_CONTROL_GUIDANCE
            + "\n\n" + MARKDOWN_RESPONSE_GUIDANCE + "\n\n" + TOOL_CALL_REQUEST_WRAPPER_GUIDANCE;
    }
    if (message == null) return guidance;
    String trimmed = message.trim();
    if (trimmed.isEmpty()) return guidance;
    return trimmed + "\n\n" + guidance;
}
```

Note: `AI_ANNOUNCES_TOOLS_PROPERTY` constant defined here (canonical location), or in `AIChatService` — define in `AIChatService` and reference from there, to keep MessageBuilder's only concern as message text. Actually keep it in `AIChatService` since that's where the tool-exclusion logic lives; `MessageBuilder.buildForChat()` reads it locally.

Simplest: define `AI_ANNOUNCES_TOOLS_PROPERTY = "ai_announces_tools"` as a `package-private` constant in `MessageBuilder` (used by both `MessageBuilder` and `AIChatService`). Or just duplicate the literal — it's a single string, not a shared API.

**Decision**: Define in `AIChatService` as a constant, pass the boolean to `MessageBuilder.buildForChat(boolean)`. The no-arg `buildForChat()` is kept for tests/backward compat.

### 2. `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIChatService.java`

Refactor constructor to store fields needed for rebuilding. Extract `buildAssistant(boolean)`. Check for mode change in `chat()`.

Fields to add/store:
- `static final String AI_ANNOUNCES_TOOLS_PROPERTY = "ai_announces_tools"`
- `ChatModel chatLanguageModel` (store from constructor param)
- `AIToolSet toolSet` (already used via method ref — store it)
- `ChatMemory chatMemory` (store from constructor param)
- `ChatTokenUsageTracker chatTokenUsageTracker` (store)
- `ToolCallSummaryHandler toolCallSummaryHandler` (store)
- `Supplier<Boolean> cancellationSupplier` (store)
- `Consumer<TokenUsage> tokenUsageConsumer` (store)
- `ToolExecutorRegistry toolExecutorRegistry` (move from local to field)
- `boolean lastAnnouncesTools` (new)
- `AIAssistant assistant` (move from field-like local to instance field)

`chat()`:
```java
public String chat(String message) {
    boolean announcesTools = this.announcesTools();
    if (announcesTools != lastAnnouncesTools) {
        assistant = buildAssistant(announcesTools);
        lastAnnouncesTools = announcesTools;
    }
    return assistant.chat(message);
}

private boolean announcesTools() {
    String value = ResourceController.getResourceController().getProperty(AI_ANNOUNCES_TOOLS_PROPERTY, "true");
    return "true".equalsIgnoreCase(value);
}
```

`buildAssistant(boolean announcesTools)`:
```java
private AIAssistant buildAssistant(boolean announcesTools) {
    AiServices.Builder<AIAssistant> builder = AiServices.builder(AIAssistant.class)
        .toolArgumentsErrorHandler(toolArgumentsErrorHandler)
        .chatModel(chatLanguageModel)
        .systemMessageProvider(input -> toolSet.systemMessageForChat(input, announcesTools))
        // ... re-attach all 3 listeners ...
        ;
    if (!announcesTools) {
        builder.tools(toolExecutorRegistry.getExecutorsBySpecification());
    }
    if (chatMemory != null) {
        builder.chatMemory(chatMemory);
    }
    return builder.build();
}
```

Constructor calls `buildAssistant(announcesTools())` at the end to set initial state.

**Note on systemMessageProvider**: Instead of passing `announcesTools` through `AIToolSet.systemMessageForChat`, the lambda can call `toolSet.systemMessageForChat()` directly with the boolean. This avoids changing `AIToolSet`'s public method signature. The provider becomes:
```java
.systemMessageProvider(input -> toolSet.systemMessageForChat(input, announcesTools))
```
But `AIToolSet.systemMessageForChat()` currently takes `Object` and calls `messageBuilder.buildForChat()`. We'd need to add `systemMessageForChat(Object, boolean)` — OR simply have `buildAssistant` pass a lambda that calls `messageBuilder.buildForChat(announcesTools)` directly:
```java
.systemMessageProvider(input -> toolSet.buildSystemMessageForChat(announcesTools))
```

**Simplest path**: Add `buildForChat(boolean)` to `MessageBuilder`, then in `buildAssistant(boolean announcesTools)`:
```java
.systemMessageProvider(input -> toolSet.messageBuilder.buildForChat(announcesTools))
```
But `messageBuilder` is private.

**Clean solution**: Keep `AIToolSet.systemMessageForChat(Object)` as-is (it reads the property itself via `messageBuilder.buildForChat()` which reads `ai_announces_tools`). No change needed to `AIToolSet`. The `systemMessageProvider` stays `toolSet::systemMessageForChat`. The `buildForChat()` no-arg in `MessageBuilder` reads the property each call — already dynamic.

This means `MessageBuilder` only needs:
- `buildForChat(boolean announcesTools)` — real logic
- `buildForChat()` — reads `ai_announces_tools`, delegates to `buildForChat(boolean)`

And `AIToolSet.systemMessageForChat()` stays unchanged.

## Summary of changes

| File | Change                                                                                                                                                            |
|------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MessageBuilder.java` | Add `buildForChat(boolean)` with MAP_SELECTION/TOOL_CALL_WRAPPER excluded when announcesTools is false; `buildForChat()` reads `ai_announces_tools` and delegates |
| `AIChatService.java` | Store all constructor params as fields; add `AI_ANNOUNCES_TOOLS_PROPERTY`, `lastAnnouncesTools`; extract `buildAssistant(boolean)`; check+rebuild in `chat()`     |

`AIToolSet.java` — **no change needed**: `systemMessageForChat()` already calls `messageBuilder.buildForChat()` which will now be property-aware.

## Verification
- With `ai_announces_tools` unset or `true`: behavior identical to current (all 4 guidance blocks, tools in request)
- With `ai_announces_tools=false`: system message has only PROFILE_CONTROL + MARKDOWN guidance; no `tools` array in API request
- Toggle mid-conversation: rebuild fires once on first `chat()` after the change, same `ChatMemory` kept

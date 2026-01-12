# Task: Remove Gson dependency from ai plugin
- **Scope:** Remove the Gson dependency from the ai plugin build file and use Jackson provided by LangChain4j instead.
- **Modified production files:**
  - freeplane_plugin_ai/build.gradle
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/chat/AIModelCatalog.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/chat/AIModelCatalogTest.java
- **Research:**
```plantuml
@startuml
class AIModelCatalog
class Gson

AIModelCatalog --> Gson

note right of AIModelCatalog
Parses OpenRouter and Ollama model lists.
Uses Gson with SerializedName annotations.
No other Gson usage in the ai plugin.
end note

note bottom
freeplane_plugin_ai/build.gradle declares
lib 'com.google.code.gson:gson:2.13.1'.
end note
@enduml
```
- **Design:**
```plantuml
@startuml
class AIModelCatalog
class ObjectMapper
class OpenrouterModelsResponse
class OllamaModelsResponse

AIModelCatalog --> ObjectMapper
ObjectMapper --> OpenrouterModelsResponse
ObjectMapper --> OllamaModelsResponse

note right of AIModelCatalog
Replace Gson with a reusable ObjectMapper.
Configure the mapper to ignore unknown properties.
Use JsonProperty annotations on response fields.
end note

note bottom
Remove Gson dependency from freeplane_plugin_ai/build.gradle.
Keep parsing limited to model list responses.
end note
@enduml
```
- **Test specification:**
  - Add a unit test that parses a sample OpenRouter response and returns the expected model identifiers.
  - Add a unit test that parses a sample Ollama response and returns the expected model names.

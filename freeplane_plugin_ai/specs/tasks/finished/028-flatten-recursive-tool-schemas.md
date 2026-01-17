# Task: Flatten recursive tool schemas for model-agnostic tool calls

## Scope
Replace recursive tool request structures with flat lists so Gemini and Ollama can use the same tool set without schema recursion.

## Motivation
Gemini and Ollama do not accept recursive JSON schema in tool parameters, which breaks `createNodes` and `createSummary`. A flat schema removes the recursion and keeps tool availability consistent across providers.

## Research
- Gemini tool parameters reject `$ref` and `JsonReferenceSchema`, so recursive schemas fail at request construction.
- Flat lists with `index` and `parentIndex` are accepted by Gemini in tool call tests.

## Design
- Replace recursive `NodeCreationItem` inputs with a flat `nodes` list.
- Each item includes `index` (unique) and `parentIndex` (`-1` for root).
- Build the tree in two passes so item order does not matter.
- Validate uniqueness of indices and that every `parentIndex` is `-1` or refers to a valid index.
- Preserve sibling order based on the order of items in the flat list.
- Apply the same structure to `createNodes` and `createSummary` inputs.
- Keep outputs unchanged.

## Test specification
- Validate flat input builds the same tree as prior recursive inputs.
- Validate invalid indices or parent references return errors.

## Modified files
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/AIToolSet.java
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/CreateNodesTool.java
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/CreateSummaryTool.java
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeContentApplier.java
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeCreationHierarchy.java
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeCreationHierarchyBuilder.java
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeCreationItem.java
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/NodeModelCreator.java
- freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/CreateNodesToolTest.java
- freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/CreateSummaryToolTest.java
- freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/NodeContentApplierTest.java
- freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/NodeCreationHierarchyBuilderTest.java
- freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/tools/NodeModelCreatorTest.java

## Subtasks
None.

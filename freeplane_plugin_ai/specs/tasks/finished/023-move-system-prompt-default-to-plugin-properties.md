# Task: Move system prompt default to plugin properties

## Scope
- Store the default system prompt in the plugin default properties instead of translations.
- Ensure the default system prompt is not translatable.

## Motivation
- The system prompt should be stable and not vary by translation.

## Research
- Current system prompt default configuration:
- Activator reads ai_system_message_default through TextUtils.getRawText and sets the default property for ai_system_message.
- The ai_system_message_default key is defined in freeplane/src/viewer/resources/translations/Resources_en.properties.
- preferences.xml exposes ai_system_message as a text box in the plugin preferences.
- defaults.properties for the plugin does not define a default system message string.

## Design
- Move the default system prompt to the plugin defaults:
- Define ai_system_message in freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties.
- Remove use of ai_system_message_default from translation resources.
- Adjust Activator to set the default system message from plugin defaults without TextUtils lookup.
- Behavior:
- The preferences text box continues to edit ai_system_message.
- The default value is no longer tied to translations and is not translatable.

## Test specification
- Verify the default system prompt is loaded from plugin defaults.
- Verify translations no longer affect the default system prompt.

## Modified files
- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/Activator.java
- freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/defaults.properties
- freeplane/src/viewer/resources/translations/Resources_en.properties

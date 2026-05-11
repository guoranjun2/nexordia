package org.freeplane.plugin.ai.prompt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AiPromptNameValidator {
    private static final String DEFAULT_NEW_PROMPT_NAME = "New Prompt";
    private static final Pattern NUMBERED_NAME_PATTERN = Pattern.compile("^(.*)\\s+(\\d+)$");

    private AiPromptNameValidator() {
    }

    static List<AiPrompt> normalizeAndDeduplicate(List<AiPrompt> prompts, String defaultName) {
        List<AiPrompt> normalizedPrompts = new ArrayList<AiPrompt>();
        if (prompts == null) {
            return normalizedPrompts;
        }
        String fallbackName = fallbackName(defaultName);
        Set<String> usedExactNames = new LinkedHashSet<String>();
        Map<String, Set<Integer>> usedNumbersByBase = new LinkedHashMap<String, Set<Integer>>();
        for (AiPrompt prompt : prompts) {
            AiPrompt normalizedPrompt = normalize(prompt, fallbackName);
            PromptNameInfo promptNameInfo = PromptNameInfo.from(normalizedPrompt.getName());
            String resolvedName = resolveName(promptNameInfo, usedExactNames, usedNumbersByBase);
            normalizedPrompt.setName(resolvedName);
            normalizedPrompts.add(normalizedPrompt);
        }
        return normalizedPrompts;
    }

    static AiPrompt normalizeForSave(AiPrompt prompt, List<AiPrompt> existingPrompts, String defaultName) {
        List<AiPrompt> prompts = new ArrayList<AiPrompt>();
        if (existingPrompts != null) {
            for (AiPrompt existingPrompt : existingPrompts) {
                prompts.add(existingPrompt == null ? new AiPrompt() : existingPrompt.copy());
            }
        }
        prompts.add(prompt == null ? new AiPrompt() : prompt.copy());
        List<AiPrompt> normalizedPrompts = normalizeAndDeduplicate(prompts, defaultName);
        return normalizedPrompts.get(normalizedPrompts.size() - 1);
    }

    static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private static String resolveName(PromptNameInfo promptNameInfo, Set<String> usedExactNames,
                                      Map<String, Set<Integer>> usedNumbersByBase) {
        if (!usedExactNames.contains(promptNameInfo.exactKey)) {
            usedExactNames.add(promptNameInfo.exactKey);
            reserveNumber(usedNumbersByBase, promptNameInfo.baseKey, promptNameInfo.requestedNumber);
            return promptNameInfo.displayName();
        }
        int resolvedNumber = nextAvailableNumber(usedNumbersByBase.get(promptNameInfo.baseKey));
        String resolvedName = promptNameInfo.baseName + " " + resolvedNumber;
        usedExactNames.add(normalizedKey(resolvedName));
        reserveNumber(usedNumbersByBase, promptNameInfo.baseKey, resolvedNumber);
        return resolvedName;
    }

    private static int nextAvailableNumber(Set<Integer> usedNumbers) {
        int counter = 1;
        while (usedNumbers != null && usedNumbers.contains(counter)) {
            counter++;
        }
        return counter;
    }

    private static void reserveNumber(Map<String, Set<Integer>> usedNumbersByBase, String baseKey, int number) {
        Set<Integer> usedNumbers = usedNumbersByBase.get(baseKey);
        if (usedNumbers == null) {
            usedNumbers = new LinkedHashSet<Integer>();
            usedNumbersByBase.put(baseKey, usedNumbers);
        }
        usedNumbers.add(number);
    }

    private static AiPrompt normalize(AiPrompt prompt, String fallbackName) {
        AiPrompt safePrompt = prompt == null ? new AiPrompt() : prompt.copy();
        String normalizedName = normalizeName(safePrompt.getName());
        safePrompt.setName(normalizedName.isEmpty() ? fallbackName : normalizedName);
        return safePrompt;
    }

    private static String fallbackName(String defaultName) {
        String normalizedDefaultName = normalizeName(defaultName);
        return normalizedDefaultName.isEmpty() ? DEFAULT_NEW_PROMPT_NAME : normalizedDefaultName;
    }

    private static String normalizedKey(String value) {
        return normalizeName(value).toLowerCase(Locale.ROOT);
    }

    private static class PromptNameInfo {
        private final String baseName;
        private final String baseKey;
        private final int requestedNumber;
        private final String exactKey;

        private PromptNameInfo(String baseName, int requestedNumber) {
            this.baseName = baseName;
            this.baseKey = normalizedKey(baseName);
            this.requestedNumber = requestedNumber;
            this.exactKey = normalizedKey(displayName());
        }

        static PromptNameInfo from(String desiredName) {
            String safeDesiredName = normalizeName(desiredName);
            Matcher matcher = NUMBERED_NAME_PATTERN.matcher(safeDesiredName);
            if (matcher.matches()) {
                String candidateBaseName = normalizeName(matcher.group(1));
                if (!candidateBaseName.isEmpty()) {
                    return new PromptNameInfo(candidateBaseName, Integer.parseInt(matcher.group(2)));
                }
            }
            return new PromptNameInfo(safeDesiredName, 0);
        }

        String displayName() {
            return requestedNumber == 0 ? baseName : baseName + " " + requestedNumber;
        }
    }
}

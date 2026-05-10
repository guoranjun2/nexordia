package org.freeplane.plugin.ai.tools.utilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;

public class ToolExecutorRegistryTest {

    @Test
    public void filteredKeepsSubsetInOriginalRegistryOrderAndAlignsSpecifications() {
        ToolExecutorRegistry registry = new ToolExecutorFactory(true, true).createRegistry(new TestToolSet());

        ToolExecutorRegistry filteredRegistry = registry.filtered(Arrays.asList("gamma", "beta"));

        List<String> expectedToolNames = filterToolNames(registry.getExecutorsByName(), "beta", "gamma");
        assertThat(new ArrayList<String>(filteredRegistry.getExecutorsByName().keySet())).isEqualTo(expectedToolNames);
        assertThat(toolSpecificationNames(filteredRegistry)).isEqualTo(expectedToolNames);
    }

    private List<String> filterToolNames(Map<String, ?> valuesByName, String... allowedNames) {
        List<String> expected = new ArrayList<String>();
        List<String> allowed = Arrays.asList(allowedNames);
        for (String toolName : valuesByName.keySet()) {
            if (allowed.contains(toolName)) {
                expected.add(toolName);
            }
        }
        return expected;
    }

    private List<String> toolSpecificationNames(ToolExecutorRegistry registry) {
        List<String> toolNames = new ArrayList<String>();
        for (ToolSpecification specification : registry.getExecutorsBySpecification().keySet()) {
            toolNames.add(specification.name());
        }
        return toolNames;
    }

    private static class TestToolSet {
        @Tool("alpha")
        public String alpha() {
            return "alpha";
        }

        @Tool("beta")
        public String beta() {
            return "beta";
        }

        @Tool("gamma")
        public String gamma() {
            return "gamma";
        }
    }
}

package org.freeplane.plugin.ai.edits;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class AiEditsStateIconDecisionTest {
    @Test
    public void shouldShowIcon_requiresVisibilityAndMarker() {
        AiEditsStateIconDecision uut = new AiEditsStateIconDecision();

        assertThat(uut.shouldShowIcon(true, true)).isTrue();
        assertThat(uut.shouldShowIcon(true, false)).isFalse();
        assertThat(uut.shouldShowIcon(false, true)).isFalse();
        assertThat(uut.shouldShowIcon(false, false)).isFalse();
    }
}

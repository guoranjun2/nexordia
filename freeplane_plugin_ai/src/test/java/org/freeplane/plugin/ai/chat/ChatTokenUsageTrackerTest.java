package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import dev.langchain4j.model.output.TokenUsage;
import org.junit.Test;

public class ChatTokenUsageTrackerTest {
    @Test
    public void recordTokenUsage_accumulatesTotals() {
        AtomicReference<ChatUsageTotals> totalsReference = new AtomicReference<>();
        ChatTokenUsageTracker uut = new ChatTokenUsageTracker(totalsReference::set);

        uut.recordTokenUsage(new TokenUsage(3, 7));
        uut.recordTokenUsage(new TokenUsage(2, 4));

        ChatUsageTotals totals = totalsReference.get();
        assertThat(totals.getInputTokenCount()).isEqualTo(5L);
        assertThat(totals.getOutputTokenCount()).isEqualTo(11L);
    }

    @Test
    public void resetTotals_clearsCounts() {
        AtomicReference<ChatUsageTotals> totalsReference = new AtomicReference<>();
        ChatTokenUsageTracker uut = new ChatTokenUsageTracker(totalsReference::set);

        uut.recordTokenUsage(new TokenUsage(3, 7));
        uut.resetTotals();

        ChatUsageTotals totals = totalsReference.get();
        assertThat(totals.getInputTokenCount()).isZero();
        assertThat(totals.getOutputTokenCount()).isZero();
    }
}

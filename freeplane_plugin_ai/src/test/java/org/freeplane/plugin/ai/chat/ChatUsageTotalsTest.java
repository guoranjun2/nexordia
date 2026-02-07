package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ChatUsageTotalsTest {

    @Test
    public void formatStatusLineUsesModeLabelWhenProvided() {
        ChatUsageTotals totals = ChatUsageTotals.estimated(5L, 7L).withLabel("Context window estimates");

        assertThat(totals.formatStatusLine()).startsWith("Context window estimates:");
    }

    @Test
    public void formatStatusLineOmitsPrefixWhenNoLabelIsSet() {
        ChatUsageTotals totals = new ChatUsageTotals(2L, 3L);

        assertThat(totals.formatStatusLine()).startsWith("input 2, output 3");
    }
}

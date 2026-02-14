package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.freeplane.core.ui.components.html.ScaledStyleSheet;
import org.junit.Test;

public class ChatMessageStyleApplierTest {

    @Test
    public void applyUsesScaledStyleSheetAndConfiguredFontSizes() {
        JEditorPane messageHistoryPane = new JEditorPane();
        messageHistoryPane.setContentType("text/html");
        HTMLEditorKit messageHistoryEditorKit = new HTMLEditorKit();

        new ChatMessageStyleApplier().apply(messageHistoryPane, messageHistoryEditorKit, 18);

        HTMLDocument document = (HTMLDocument) messageHistoryPane.getDocument();
        StyleSheet styleSheet = document.getStyleSheet();
        assertThat(styleSheet).isInstanceOf(ScaledStyleSheet.class);

        assertThat(readFontSize(styleSheet, "body")).isEqualTo("18pt");
        assertThat(readFontSize(styleSheet, ".message-context-boundary")).isEqualTo("15pt");
    }

    private String readFontSize(StyleSheet styleSheet, String selector) {
        AttributeSet rule = styleSheet.getRule(selector);
        Object fontSize = rule == null ? null : rule.getAttribute(CSS.Attribute.FONT_SIZE);
        return fontSize == null ? null : fontSize.toString();
    }
}

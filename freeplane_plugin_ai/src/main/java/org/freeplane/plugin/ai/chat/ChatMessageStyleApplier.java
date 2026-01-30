package org.freeplane.plugin.ai.chat;

import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.Color;

public class ChatMessageStyleApplier {

    public void apply(JEditorPane messageHistoryPane, HTMLEditorKit messageHistoryEditorKit) {
        Color baseBackground = UIManager.getColor("TextArea.background");
        Color baseForeground = UIManager.getColor("TextArea.foreground");
        if (baseBackground == null) {
            baseBackground = Color.WHITE;
        }
        if (baseForeground == null) {
            baseForeground = Color.BLACK;
        }
        boolean darkTheme = isDark(baseBackground);
        Color userBackground = darkTheme ? new Color(0x45, 0x45, 0x45) : new Color(0xeb, 0xeb, 0xeb);
        Color assistantBackground = darkTheme ? new Color(0x2b, 0x2b, 0x2b) : new Color(0xf5, 0xf5, 0xf5);
        Color toolBackground = darkTheme ? new Color(0x2a, 0x36, 0x46) : new Color(0xea, 0xf3, 0xff);
        Color mcpBackground = darkTheme ? new Color(0x25, 0x33, 0x54) : new Color(0xea, 0xf3, 0xff);
        Color userBorderColor = darkTheme ? new Color(0x6a, 0x6a, 0x6a) : new Color(0x3e, 0x3e, 0x3e);
        Color borderColor = darkTheme ? new Color(0x52, 0x52, 0x52) : new Color(0xd7, 0xd7, 0xd7);
        Color toolBorderColor = darkTheme ? new Color(0x5a, 0x6f, 0x8a) : new Color(0xbc, 0xd9, 0xff);
        Color mcpBorderColor = darkTheme ? new Color(0x6a, 0x7f, 0xb0) : new Color(0x5c, 0x79, 0xbd);
        messageHistoryPane.setBackground(baseBackground);
        StyleSheet styleSheet = messageHistoryEditorKit.getStyleSheet();
        styleSheet.addRule("body { font-family: Sans-Serif; font-size: 12pt; margin: 6px; color: "
            + toCssColor(baseForeground) + "; background-color: " + toCssColor(baseBackground) + "; }");
        styleSheet.addRule(".message-user { margin: 6px 0; padding: 6px 8px; background-color: "
            + toCssColor(userBackground) + "; border-left: 4px solid " + toCssColor(userBorderColor) + "; }");
        styleSheet.addRule(".message-assistant { margin: 6px 0; padding: 6px 8px; background-color: "
            + toCssColor(assistantBackground) + "; border-left: 4px solid " + toCssColor(borderColor) + "; }");
        styleSheet.addRule(".message-tool { margin: 6px 0; padding: 6px 8px; background-color: "
            + toCssColor(toolBackground) + "; border-left: 4px solid " + toCssColor(toolBorderColor) + "; }");
        styleSheet.addRule(".message-mcp-call { margin: 6px 0; padding: 6px 8px; background-color: "
            + toCssColor(mcpBackground) + "; border-left: 8px solid " + toCssColor(mcpBorderColor) + "; }");
    }

    private boolean isDark(Color color) {
        double luminance = 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
        return luminance < 128;
    }

    private String toCssColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}

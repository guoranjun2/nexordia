package org.freeplane.plugin.ai.chat;

import org.freeplane.core.util.LogUtils;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.IOException;

class ChatMessageHistory {
    private final JEditorPane messageHistoryPane;
    private final HTMLEditorKit messageHistoryEditorKit;

    ChatMessageHistory(JEditorPane messageHistoryPane, HTMLEditorKit messageHistoryEditorKit) {
        this.messageHistoryPane = messageHistoryPane;
        this.messageHistoryEditorKit = messageHistoryEditorKit;
    }

    void appendMessage(String messageText, String styleClassName) {
        HTMLDocument document = (HTMLDocument) messageHistoryPane.getDocument();
        String messageMarkup = "<div class=\"" + styleClassName + "\">" + messageText + "</div>";
        try {
            messageHistoryEditorKit.insertHTML(document, document.getLength(), messageMarkup, 0, 0, null);
        } catch (BadLocationException | IOException error) {
            LogUtils.severe(error);
        }
        scrollToBottom();
    }

    private void scrollToBottom() {
        messageHistoryPane.setCaretPosition(messageHistoryPane.getDocument().getLength());
    }
}

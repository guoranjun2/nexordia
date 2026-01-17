package org.freeplane.plugin.ai.chat;

import org.freeplane.core.util.LogUtils;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ChatMessageHistory {
    private final JEditorPane messageHistoryPane;
    private final HTMLEditorKit messageHistoryEditorKit;
    private final List<MessageEntry> messageEntries;

    ChatMessageHistory(JEditorPane messageHistoryPane, HTMLEditorKit messageHistoryEditorKit) {
        this.messageHistoryPane = messageHistoryPane;
        this.messageHistoryEditorKit = messageHistoryEditorKit;
        messageEntries = new ArrayList<>();
    }

    void appendMessage(String sourceText, String messageText, String styleClassName) {
        HTMLDocument document = (HTMLDocument) messageHistoryPane.getDocument();
        String messageMarkup = "<div class=\"" + styleClassName + "\">" + messageText + "</div>";
        int startOffset = document.getLength();
        try {
            messageHistoryEditorKit.insertHTML(document, document.getLength(), messageMarkup, 0, 0, null);
        } catch (BadLocationException | IOException error) {
            LogUtils.severe(error);
        }
        int endOffset = document.getLength();
        messageEntries.add(new MessageEntry(startOffset, endOffset, sourceText, messageMarkup));
        scrollToBottom();
    }

    Transferable createTransferable(int selectionStart, int selectionEnd) {
        if (selectionStart == selectionEnd) {
            return null;
        }
        List<MessageEntry> selectedEntries = new ArrayList<>();
        for (MessageEntry entry : messageEntries) {
            if (selectionStart < entry.endOffset && selectionEnd > entry.startOffset) {
                selectedEntries.add(entry);
            }
        }
        if (selectedEntries.isEmpty()) {
            return null;
        }
        String plainText = joinSourceText(selectedEntries);
        String markupText = wrapMarkup(joinMarkup(selectedEntries));
        return new ChatMessageTransferable(plainText, markupText);
    }

    private String joinSourceText(List<MessageEntry> selectedEntries) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < selectedEntries.size(); index++) {
            if (index > 0) {
                builder.append("\n\n");
            }
            builder.append(selectedEntries.get(index).sourceText);
        }
        return builder.toString();
    }

    private String joinMarkup(List<MessageEntry> selectedEntries) {
        StringBuilder builder = new StringBuilder();
        for (MessageEntry entry : selectedEntries) {
            builder.append(entry.messageMarkup);
        }
        return builder.toString();
    }

    private String wrapMarkup(String markup) {
        return "<html><body>" + markup + "</body></html>";
    }

    private void scrollToBottom() {
        messageHistoryPane.setCaretPosition(messageHistoryPane.getDocument().getLength());
    }

    private static class MessageEntry {
        private final int startOffset;
        private final int endOffset;
        private final String sourceText;
        private final String messageMarkup;

        MessageEntry(int startOffset, int endOffset, String sourceText, String messageMarkup) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.sourceText = sourceText;
            this.messageMarkup = messageMarkup;
        }
    }

    private static class ChatMessageTransferable implements Transferable {
        private static final DataFlavor MARKUP_DATA_FLAVOR = createMarkupDataFlavor();

        private final String plainText;
        private final String markupText;

        ChatMessageTransferable(String plainText, String markupText) {
            this.plainText = plainText;
            this.markupText = markupText;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.stringFlavor, MARKUP_DATA_FLAVOR };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.stringFlavor.equals(flavor) || MARKUP_DATA_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (DataFlavor.stringFlavor.equals(flavor)) {
                return plainText;
            }
            if (MARKUP_DATA_FLAVOR.equals(flavor)) {
                return markupText;
            }
            throw new UnsupportedFlavorException(flavor);
        }

        private static DataFlavor createMarkupDataFlavor() {
            try {
                return new DataFlavor("text/html;class=java.lang.String");
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Unable to create text/html data flavor.", exception);
            }
        }
    }
}

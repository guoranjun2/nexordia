package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;

import org.junit.Test;

public class ChatMessageTransferHandlerTest {
    @Test
    public void createTransferable_copiesPlainTextWithLineBreaksAndMarkup() throws Exception {
        JEditorPane messageHistoryPane = new JEditorPane();
        messageHistoryPane.setContentType("text/html");
        HTMLEditorKit messageHistoryEditorKit = (HTMLEditorKit) messageHistoryPane.getEditorKit();
        ChatMessageHistory messageHistory = new ChatMessageHistory(messageHistoryPane, messageHistoryEditorKit);
        String sourceText = "Line one\nLine two";
        String renderedMarkup = "Line one<br>Line two";
        messageHistory.appendMessage(sourceText, renderedMarkup, "message-assistant");
        ChatMessageTransferHandler transferHandler = new ChatMessageTransferHandler(messageHistoryPane, messageHistory);

        messageHistoryPane.setSelectionStart(0);
        messageHistoryPane.setSelectionEnd(messageHistoryPane.getDocument().getLength());

        Transferable transferable = transferHandler.createTransferable(messageHistoryPane);

        assertThat(transferable).isNotNull();
        assertThat(transferable.isDataFlavorSupported(DataFlavor.stringFlavor)).isTrue();
        Object plainText = transferable.getTransferData(DataFlavor.stringFlavor);
        assertThat(plainText).isEqualTo(sourceText);

        DataFlavor markupFlavor = findMarkupFlavor(transferable.getTransferDataFlavors());
        assertThat(markupFlavor).isNotNull();
        Object markupText = transferable.getTransferData(markupFlavor);
        assertThat(markupText).isInstanceOf(String.class);
        assertThat((String) markupText).contains("<html><body>");
        assertThat((String) markupText).contains("<div class=\"message-assistant\">Line one<br>Line two</div>");
    }

    @Test
    public void createTransferable_joinsMultipleMessagesWithBlankLine() throws Exception {
        JEditorPane messageHistoryPane = new JEditorPane();
        messageHistoryPane.setContentType("text/html");
        HTMLEditorKit messageHistoryEditorKit = (HTMLEditorKit) messageHistoryPane.getEditorKit();
        ChatMessageHistory messageHistory = new ChatMessageHistory(messageHistoryPane, messageHistoryEditorKit);
        messageHistory.appendMessage("First line\nSecond line", "First line<br>Second line", "message-user");
        messageHistory.appendMessage("Third line", "Third line", "message-user");
        ChatMessageTransferHandler transferHandler = new ChatMessageTransferHandler(messageHistoryPane, messageHistory);

        messageHistoryPane.setSelectionStart(0);
        messageHistoryPane.setSelectionEnd(messageHistoryPane.getDocument().getLength());

        Transferable transferable = transferHandler.createTransferable(messageHistoryPane);

        Object plainText = transferable.getTransferData(DataFlavor.stringFlavor);
        assertThat(plainText).isEqualTo("First line\nSecond line\n\nThird line");
    }

    private DataFlavor findMarkupFlavor(DataFlavor[] dataFlavors) {
        for (DataFlavor dataFlavor : dataFlavors) {
            if (dataFlavor.getMimeType().startsWith("text/html")) {
                return dataFlavor;
            }
        }
        return null;
    }
}

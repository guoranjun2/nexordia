package org.freeplane.plugin.ai.chat;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.ai.tools.AIToolSet;
import org.freeplane.plugin.ai.tools.ToolCallSummary;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;

public class AIChatPanel extends JPanel {

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
    private final JEditorPane messageHistoryPane;
    private final HTMLEditorKit messageHistoryEditorKit;
    private final JScrollPane scrollPane;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private AIChatService chatService;
    private final JPopupMenu menuPopup;
    private final AIProviderConfiguration configuration;
    private final ChatDisplaySettings chatDisplaySettings;
    private final AIModelSelectionController modelSelectionController;
    private final ChatSessionMemoryController chatSessionMemoryController;
    private final ChatTokenUsageTracker chatTokenUsageTracker;
    private final JLabel tokenUsageLabel;

    public AIChatPanel() {
        setLayout(new BorderLayout());
        messageHistoryPane = new JEditorPane();
        messageHistoryPane.setContentType("text/html");
        messageHistoryPane.setEditable(false);
        messageHistoryPane.setOpaque(true);
        messageHistoryPane.setBackground(Color.WHITE);
        messageHistoryEditorKit = (HTMLEditorKit) messageHistoryPane.getEditorKit();
        configureMessageHistoryStyles();
        resetMessageHistory();
        scrollPane = new JScrollPane(messageHistoryPane);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputArea = new JTextArea(3, 20);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        sendButton = new JButton("Send");
        menuPopup = buildMenuPopup();
        configuration = new AIProviderConfiguration();
        chatDisplaySettings = new ChatDisplaySettings();
        modelSelectionController = new AIModelSelectionController(configuration, new AIModelCatalog(configuration));
        modelSelectionController.setModelSelectionChangeListener(modelDescriptor -> chatService = null);
        chatSessionMemoryController = new ChatSessionMemoryController();
        tokenUsageLabel = new JLabel();
        tokenUsageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        chatTokenUsageTracker = new ChatTokenUsageTracker(this::updateTokenUsageLabel);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.add(inputPanel, BorderLayout.CENTER);
        JPanel tokenUsagePanel = new JPanel(new BorderLayout());
        tokenUsagePanel.add(tokenUsageLabel, BorderLayout.EAST);
        inputContainer.add(tokenUsagePanel, BorderLayout.SOUTH);

        FreeplaneToolBar toolbar = new FreeplaneToolBar(SwingConstants.HORIZONTAL);
        configureToolbar(toolbar);
        JPanel topBarContainer = new JPanel(new BorderLayout());
        topBarContainer.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
        topBarContainer.add(toolbar, BorderLayout.WEST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputContainer, BorderLayout.SOUTH);
        add(topBarContainer, BorderLayout.NORTH);

        sendButton.addActionListener(event -> sendMessage());
        inputArea.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ctrl ENTER"), "sendMessage");
        inputArea.getActionMap().put("sendMessage", new AbstractAction() {
            /**
			 * Comment for <code>serialVersionUID</code>
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                sendMessage();
            }
        });
        modelSelectionController.loadInitialModelSelectionList();
    }

    private void configureToolbar(FreeplaneToolBar toolbar) {
        JButton menuButton = new JButton("\u2261");
        TranslatedElementFactory.createTooltip(menuButton, "preferences");
        menuButton.addActionListener(event -> menuPopup.show(menuButton, 0, menuButton.getHeight()));
        toolbar.add(menuButton);
        JButton newChatButton = new JButton("New chat");
        newChatButton.addActionListener(event -> startNewChat());
        toolbar.add(newChatButton);
        toolbar.add(modelSelectionController.getModelSelectionComboBox());
    }

    private JPopupMenu buildMenuPopup() {
        JPopupMenu menuPopup = new JPopupMenu();
        Action openPreferencesAction = new AbstractAction("Preferences") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                openPreferences();
            }
        };
        menuPopup.add(TranslatedElementFactory.createMenuItem(openPreferencesAction, "preferences"));
        return menuPopup;
    }

    private void openPreferences() {
        Controller controller = Controller.getCurrentController();
        MModeController modeController = (MModeController) controller.getModeController(MModeController.MODENAME);
        modeController.showPreferences("plugins", "ai");
    }

    private void sendMessage() {
        String userMessage = inputArea.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }
        appendChatMessage(userMessage, ChatMessageCategory.USER);
        inputArea.setText("");
        ensureChatService();
        if (chatService == null) {
            return;
        }
        sendButton.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return chatService.chat(userMessage);
            }

            @Override
            protected void done() {
                try {
                    appendChatMessage(get(), ChatMessageCategory.ASSISTANT);
                } catch (Exception error) {
                    appendChatMessage(String.valueOf(error.getMessage()), ChatMessageCategory.ASSISTANT);
                } finally {
                    sendButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void ensureChatService() {
        if (chatService != null) {
            return;
        }
        AIModelSelection selection = AIModelSelection.fromSelectionValue(configuration.getSelectedModelValue());
        if (selection == null) {
            appendChatMessage("Missing AI model selection.", ChatMessageCategory.ASSISTANT);
            return;
        }
        String providerName = selection.getProviderName();
        if (AIChatModelFactory.PROVIDER_NAME_OPENROUTER.equalsIgnoreCase(providerName)) {
            if (configuration.getOpenRouterKey() == null || configuration.getOpenRouterKey().isEmpty()) {
                appendChatMessage("Missing OpenRouter key setting.", ChatMessageCategory.ASSISTANT);
                return;
            }
        } else if (AIChatModelFactory.PROVIDER_NAME_GEMINI.equalsIgnoreCase(providerName)) {
            if (configuration.getGeminiKey() == null || configuration.getGeminiKey().isEmpty()) {
                appendChatMessage("Missing Gemini key setting.", ChatMessageCategory.ASSISTANT);
                return;
            }
        } else if (AIChatModelFactory.PROVIDER_NAME_OLLAMA.equalsIgnoreCase(providerName)) {
            if (!configuration.isOllamaEnabled()) {
                appendChatMessage("Ollama usage is disabled.", ChatMessageCategory.ASSISTANT);
                return;
            }
        } else {
            appendChatMessage("Unknown AI provider selection.", ChatMessageCategory.ASSISTANT);
            return;
        }
        chatService = AIChatServiceFactory.createService(new AIToolSet(this::handleToolCallSummary),
            chatSessionMemoryController,
            chatTokenUsageTracker);
    }

    private void appendChatMessage(String text, ChatMessageCategory category) {
        if (SwingUtilities.isEventDispatchThread()) {
            appendChatMessageInternal(text, category);
        } else {
            SwingUtilities.invokeLater(() -> appendChatMessageInternal(text, category));
        }
    }

    private void appendChatMessageInternal(String text, ChatMessageCategory category) {
        if (text == null || category == null) {
            return;
        }
        HTMLDocument document = (HTMLDocument) messageHistoryPane.getDocument();
        String messageText = formatMessageText(text);
        String messageMarkup = "<div class=\"" + category.getStyleClassName() + "\">" + messageText + "</div>";
        try {
            messageHistoryEditorKit.insertHTML(document, document.getLength(), messageMarkup, 0, 0, null);
        } catch (BadLocationException | IOException error) {
            LogUtils.severe(error);
        }
        scrollToBottom();
    }

    private void handleToolCallSummary(ToolCallSummary summary) {
        if (summary == null || !chatDisplaySettings.isToolCallHistoryVisible()) {
            return;
        }
        appendChatMessage(summary.getSummaryText(), ChatMessageCategory.TOOL_CALL);
    }

    private void configureMessageHistoryStyles() {
        StyleSheet styleSheet = messageHistoryEditorKit.getStyleSheet();
        styleSheet.addRule("body { font-family: Sans-Serif; font-size: 12pt; margin: 6px; }");
        styleSheet.addRule(".message-user { margin: 6px 0; padding: 6px 8px; background-color: #ebebeb;"
            + " border-left: 4px solid #3e3e3eff; }");
        styleSheet.addRule(".message-assistant { margin: 6px 0; padding: 6px 8px; background-color: #f5f5f5;"
            + " border-left: 4px solid #d7d7d7; }");
        styleSheet.addRule(".message-tool { margin: 6px 0; padding: 6px 8px; background-color: #eaf3ff;"
            + " border-left: 4px solid #bcd9ff; }");
    }

    private String formatMessageText(String text) {
        String escaped = HtmlUtils.toXMLEscapedText(text);
        String normalized = escaped.replace("\r\n", "\n").replace("\r", "\n");
        return normalized.replace("\n", "<br>");
    }

    private void resetMessageHistory() {
        messageHistoryPane.setText("<html><body></body></html>");
        messageHistoryPane.setCaretPosition(0);
    }

    private void startNewChat() {
        resetMessageHistory();
        chatSessionMemoryController.clearChatMemory();
        chatTokenUsageTracker.resetTotals();
    }

    private void updateTokenUsageLabel(ChatUsageTotals totals) {
        SwingUtilities.invokeLater(() -> tokenUsageLabel.setText(totals.formatStatusLine()));
    }

    private void scrollToBottom() {
        messageHistoryPane.setCaretPosition(messageHistoryPane.getDocument().getLength());
    }

    private enum ChatMessageCategory {
        USER("message-user"),
        ASSISTANT("message-assistant"),
        TOOL_CALL("message-tool");

        private final String styleClassName;

        ChatMessageCategory(String styleClassName) {
            this.styleClassName = styleClassName;
        }

        String getStyleClassName() {
            return styleClassName;
        }
    }

}

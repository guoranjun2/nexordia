package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.SetBooleanPropertyAction;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.LabelAndMnemonicSetter;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.components.JAutoCheckBoxMenuItem;
import org.freeplane.core.ui.textchanger.TranslatedElement;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.ai.edits.AiEditsSettings;
import org.freeplane.plugin.ai.edits.ClearAiMarkersInMapAction;
import org.freeplane.plugin.ai.edits.ClearAiMarkersInSelectionAction;
import org.freeplane.plugin.ai.tools.AIToolSetBuilder;
import org.freeplane.plugin.ai.tools.ToolCallSummary;
import org.freeplane.plugin.ai.tools.ToolCallSummaryHandler;
import org.freeplane.plugin.ai.tools.ToolCaller;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

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
    private final ChatMessageRenderer messageRenderer;
    private final ChatMessageHistory messageHistory;

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
        messageHistory = new ChatMessageHistory(messageHistoryPane, messageHistoryEditorKit);
        messageHistoryPane.setTransferHandler(new ChatMessageTransferHandler(messageHistoryPane, messageHistory));
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
        messageRenderer = new ChatMessageRenderer();

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
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        inputArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, shortcutMask), "sendMessage");
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
        registerModelAllowlistRefreshListener();
    }

    private void configureToolbar(FreeplaneToolBar toolbar) {
        JButton menuButton = new JButton("\u2261");
        TranslatedElementFactory.createTooltip(menuButton, "preferences");
        menuButton.addActionListener(event -> menuPopup.show(menuButton, 0, menuButton.getHeight()));
        toolbar.add(menuButton);
        toolbar.add(modelSelectionController.getModelSelectionComboBox());
        String clearIconPath = "/images/generic_trash.svg?useAccentColor=true";
        JButton newChatButton = TranslatedElementFactory.createButtonWithIcon(clearIconPath, "ai_chat_clear");
        newChatButton.addActionListener(event -> startNewChat());
        toolbar.add(newChatButton);
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
        addAiEditsMenuItems(menuPopup);
        return menuPopup;
    }

    private void addAiEditsMenuItems(JPopupMenu menuPopup) {
        if (Controller.getCurrentModeController() == null) {
            return;
        }
        AFreeplaneAction clearMapAction = Controller.getCurrentModeController()
            .getAction(ClearAiMarkersInMapAction.ACTION_KEY);
        AFreeplaneAction clearSelectionAction = Controller.getCurrentModeController()
            .getAction(ClearAiMarkersInSelectionAction.ACTION_KEY);
        AFreeplaneAction showIconAction = Controller.getCurrentModeController()
            .getAction(SetBooleanPropertyAction.actionKey(AiEditsSettings.AI_EDITS_STATE_ICON_VISIBLE_PROPERTY));
        if (clearMapAction == null && clearSelectionAction == null && showIconAction == null) {
            return;
        }
        menuPopup.addSeparator();
        addMenuItem(menuPopup, clearMapAction);
        addMenuItem(menuPopup, clearSelectionAction);
        addToggleMenuItem(menuPopup, showIconAction);
    }

    private void addMenuItem(JPopupMenu menuPopup, AFreeplaneAction action) {
        if (action == null) {
            return;
        }
        menuPopup.add(TranslatedElementFactory.createMenuItem(action, action.getTextKey()));
    }

    private void addToggleMenuItem(JPopupMenu menuPopup, AFreeplaneAction action) {
        if (action == null) {
            return;
        }
        String labelKey = action.getTextKey();
        JCheckBoxMenuItem menuItem = new JAutoCheckBoxMenuItem(action);
        LabelAndMnemonicSetter.setLabelAndMnemonic(menuItem, TextUtils.getRawText(labelKey));
        TranslatedElement.TEXT.setKey(menuItem, labelKey);
        TranslatedElementFactory.createTooltip(menuItem, action.getTooltipKey());
        menuPopup.add(menuItem);
    }

    private void openPreferences() {
        Controller controller = Controller.getCurrentController();
        MModeController modeController = (MModeController) controller.getModeController(MModeController.MODENAME);
        modeController.showPreferences("plugins", "ai");
    }

    private void registerModelAllowlistRefreshListener() {
        ResourceController.getResourceController().addPropertyChangeListener(
            new IFreeplanePropertyListener() {
                @Override
                public void propertyChanged(String propertyName, String newValue, String oldValue) {
                    if (!isModelAllowlistProperty(propertyName)) {
                        return;
                    }
                    SwingUtilities.invokeLater(() -> modelSelectionController.loadInitialModelSelectionList());
                }
            });
    }

    private boolean isModelAllowlistProperty(String propertyName) {
        return "ai_openrouter_model_allowlist".equals(propertyName)
            || "ai_gemini_model_list".equals(propertyName)
            || "ai_ollama_model_allowlist".equals(propertyName);
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
        chatService = AIChatServiceFactory.createService(new AIToolSetBuilder()
                .toolCallSummaryHandler(this::handleToolCallSummary)
                .build(),
            chatSessionMemoryController,
            chatTokenUsageTracker,
            this::handleToolCallSummary);
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
        String messageText = messageRenderer.renderMessage(text, category == ChatMessageCategory.ASSISTANT);
        messageHistory.appendMessage(text, messageText, category.getStyleClassName());
    }

    public ToolCallSummaryHandler toolCallSummaryHandler() {
        return this::handleToolCallSummary;
    }

    private void handleToolCallSummary(ToolCallSummary summary) {
        if (summary == null || !chatDisplaySettings.isToolCallHistoryVisible()) {
            return;
        }
        boolean isMcpCall = summary.getToolCaller() == ToolCaller.MCP;
        ChatMessageCategory category = isMcpCall
                ? ChatMessageCategory.MCP_CALL
                : ChatMessageCategory.TOOL_CALL;
        String messageText = isMcpCall
            ? "MCP: " + summary.getSummaryText()
            : summary.getSummaryText();
        appendChatMessage(messageText, category);
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
        styleSheet.addRule(".message-mcp-call { margin: 6px 0; padding: 6px 8px; background-color: #eaf3ff;"
            + " border-left: 8px solid #5c79bd; }");
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

    private enum ChatMessageCategory {
        USER("message-user"),
        ASSISTANT("message-assistant"),
        TOOL_CALL("message-tool"),
        MCP_CALL("message-mcp-call");

        private final String styleClassName;

        ChatMessageCategory(String styleClassName) {
            this.styleClassName = styleClassName;
        }

        String getStyleClassName() {
            return styleClassName;
        }
    }

}

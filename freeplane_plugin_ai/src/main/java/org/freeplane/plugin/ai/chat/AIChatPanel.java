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
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.edits.AiEditsSettings;
import org.freeplane.plugin.ai.edits.ClearAiMarkersInMapAction;
import org.freeplane.plugin.ai.edits.ClearAiMarkersInSelectionAction;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.maps.ControllerMapModelProvider;
import org.freeplane.plugin.ai.tools.AIToolSetBuilder;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummary;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummaryHandler;
import org.freeplane.plugin.ai.tools.utilities.ToolCaller;

import dev.langchain4j.data.message.ChatMessage;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.Icon;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private final Icon sendIcon;
    private final Icon stopIcon;
    private AIChatService chatService;
    private final JPopupMenu menuPopup;
    private final AIProviderConfiguration configuration;
    private final ChatDisplaySettings chatDisplaySettings;
    private final AIModelSelectionController modelSelectionController;
    private ChatSessionMemoryController chatSessionMemoryController;
    private final ChatTokenUsageTracker chatTokenUsageTracker;
    private final JLabel tokenUsageLabel;
    private final ChatMessageRenderer messageRenderer;
    private final ChatMessageHistory messageHistory;
    private final AvailableMaps availableMaps;
    private final DateTimeFormatter chatNameFormatter;
    private final LiveChatController liveChatController;
    private final ChatRequestCancellation requestCancellation;
    private SwingWorker<String, Void> activeWorker;
    private boolean requestInProgress;
    private int activeRequestId;
    private List<ChatMessageHistory.ChatMessageSnapshot> pendingHistorySnapshot;
    private List<ChatMessage> pendingMemorySnapshot;
    private List<ChatTranscriptEntry> pendingTranscriptEntries;
    private String pendingUserMessage;

    public AIChatPanel() {
        setLayout(new BorderLayout());
        messageHistoryPane = new JEditorPane();
        messageHistoryPane.setContentType("text/html");
        messageHistoryPane.setEditable(false);
        messageHistoryPane.setOpaque(true);
        messageHistoryPane.setBackground(Color.WHITE);
        messageHistoryEditorKit = (HTMLEditorKit) messageHistoryPane.getEditorKit();
        new ChatMessageStyleApplier().apply(messageHistoryPane, messageHistoryEditorKit);
        resetMessageHistory();
        messageHistory = new ChatMessageHistory(messageHistoryPane, messageHistoryEditorKit);
        messageHistoryPane.setTransferHandler(new ChatMessageTransferHandler(messageHistoryPane, messageHistory));
        messageHistoryPane.setDragEnabled(true);
        scrollPane = new JScrollPane(messageHistoryPane);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputArea = new JTextArea(3, 20);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        sendButton = new JButton();
        sendButton.setIcon(ResourceController.getResourceController()
            .getImageIcon("/images/ai_send_arrow_up.svg?useAccentColor=true"));
        sendIcon = sendButton.getIcon();
        stopIcon = ResourceController.getResourceController()
            .getImageIcon("/images/ai_stop.svg?useAccentColor=true");
        Dimension sendButtonSize = sendButton.getPreferredSize();
        sendButton.setPreferredSize(sendButtonSize);
        sendButton.setMinimumSize(sendButtonSize);
        sendButton.setMaximumSize(sendButtonSize);
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
        availableMaps = new AvailableMaps(new ControllerMapModelProvider());
        chatNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        liveChatController = new LiveChatController(
            this,
            messageHistory,
            availableMaps,
            requireTextController(),
            chatNameFormatter,
            this::activateSession
        );
        requestCancellation = new ChatRequestCancellation();
        liveChatController.initialize(chatSessionMemoryController);

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

        sendButton.addActionListener(event -> {
            if (isRequestActive()) {
                cancelActiveRequest();
            } else {
                sendMessage();
            }
        });
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        messageHistoryPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, shortcutMask), "selectAllMessages");
        messageHistoryPane.getActionMap().put("selectAllMessages", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                messageHistoryPane.selectAll();
            }
        });
        inputArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, shortcutMask), "sendMessage");
        inputArea.getActionMap().put("sendMessage", new AbstractAction() {
            /**
			 * Comment for <code>serialVersionUID</code>
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent event) {
                if (isRequestActive()) {
                    cancelActiveRequest();
                } else {
                    sendMessage();
                }
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
        String historyIconPath = "/images/ai_history.svg?useAccentColor=true";
        JButton chatsButton = TranslatedElementFactory.createButtonWithIcon(historyIconPath, "ai_chat_chats");
        chatsButton.addActionListener(event -> {
            cancelActiveRequest();
            liveChatController.openLiveChats();
        });
        toolbar.add(chatsButton);
        String clearIconPath = "/images/ai_new_chat.svg?useAccentColor=true";
        JButton newChatButton = TranslatedElementFactory.createButtonWithIcon(clearIconPath, "ai_chat_new_chat");
        newChatButton.addActionListener(event -> {
            cancelActiveRequest();
            liveChatController.startNewChat();
        });
        toolbar.add(newChatButton);
    }

    private JPopupMenu buildMenuPopup() {
        JPopupMenu menuPopup = new JPopupMenu();
        Action openPreferencesAction = new AbstractAction("Preferences") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
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
        beginRequest(userMessage);
        appendChatMessage(userMessage, ChatMessageCategory.USER);
        liveChatController.updateSessionNameFromFirstUserMessage(userMessage);
        inputArea.setText("");
        ensureChatService();
        if (chatService == null) {
            restoreCancelledRequest();
            return;
        }
        final int requestId = activeRequestId;
        activeWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return chatService.chat(userMessage);
            }

            @Override
            protected void done() {
                try {
                    if (requestId != activeRequestId || requestCancellation.isCancelled()) {
                        return;
                    }
                    appendChatMessage(get(), ChatMessageCategory.ASSISTANT);
                } catch (Exception error) {
                    if (requestId != activeRequestId || requestCancellation.isCancelled()) {
                        return;
                    }
                    appendChatMessage(String.valueOf(error.getMessage()), ChatMessageCategory.ASSISTANT);
                } finally {
                    if (requestId == activeRequestId && !requestCancellation.isCancelled()) {
                        finishRequest();
                    }
                }
            }
        };
        activeWorker.execute();
    }

    private boolean isRequestActive() {
        return requestInProgress;
    }

    private void beginRequest(String userMessage) {
        requestCancellation.reset();
        activeRequestId++;
        pendingUserMessage = userMessage;
        pendingHistorySnapshot = messageHistory.snapshot();
        pendingMemorySnapshot = chatSessionMemoryController.snapshotMessages();
        pendingTranscriptEntries = liveChatController.snapshotTranscriptEntries();
        requestInProgress = true;
        inputArea.setEditable(false);
        setSendButtonStopState();
    }

    private void finishRequest() {
        activeWorker = null;
        requestInProgress = false;
        inputArea.setEditable(true);
        setSendButtonSendState();
        clearPendingRequestState();
    }

    private void cancelActiveRequest() {
        if (!isRequestActive()) {
            return;
        }
        requestCancellation.cancel();
        activeRequestId++;
        if (activeWorker != null) {
            activeWorker.cancel(true);
        }
        requestInProgress = false;
        restoreCancelledRequest();
    }

    private void restoreCancelledRequest() {
        if (pendingHistorySnapshot != null) {
            messageHistory.restoreMessages(pendingHistorySnapshot);
        }
        if (pendingMemorySnapshot != null) {
            chatSessionMemoryController.restoreMessages(pendingMemorySnapshot);
        }
        if (pendingTranscriptEntries != null) {
            liveChatController.restoreTranscriptEntries(pendingTranscriptEntries);
        }
        activeWorker = null;
        requestInProgress = false;
        inputArea.setEditable(true);
        inputArea.setText(pendingUserMessage == null ? "" : pendingUserMessage);
        inputArea.setCaretPosition(inputArea.getText().length());
        setSendButtonSendState();
        clearPendingRequestState();
    }

    private void clearPendingRequestState() {
        pendingHistorySnapshot = null;
        pendingMemorySnapshot = null;
        pendingTranscriptEntries = null;
        pendingUserMessage = null;
    }

    private void setSendButtonStopState() {
        sendButton.setText(null);
        sendButton.setIcon(stopIcon);
    }

    private void setSendButtonSendState() {
        sendButton.setText(null);
        sendButton.setIcon(sendIcon);
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
                .availableMaps(availableMaps)
                .mapAccessListener(liveChatController.mapAccessListener())
                .build(),
            chatSessionMemoryController,
            chatTokenUsageTracker,
            this::handleToolCallSummary,
            requestCancellation::isCancelled);
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
        if (category == ChatMessageCategory.USER) {
            liveChatController.recordUserMessage(text);
        } else if (category == ChatMessageCategory.ASSISTANT) {
            liveChatController.recordAssistantMessage(text);
        }
    }

    public ToolCallSummaryHandler toolCallSummaryHandler() {
        return this::handleToolCallSummary;
    }

    public void persistCurrentChatIfNeeded() {
        liveChatController.persistCurrentSessionIfNeeded();
    }

    private void handleToolCallSummary(ToolCallSummary summary) {
        if (summary == null || !chatDisplaySettings.isToolCallHistoryVisible() || !isRequestActive()) {
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

    private void resetMessageHistory() {
        messageHistoryPane.setText("<html><body></body></html>");
        messageHistoryPane.setCaretPosition(0);
    }

    private void updateTokenUsageLabel(ChatUsageTotals totals) {
        SwingUtilities.invokeLater(() -> tokenUsageLabel.setText(totals.formatStatusLine()));
    }

    private void activateSession(ChatSessionMemoryController sessionMemoryController) {
        if (sessionMemoryController == null) {
            return;
        }
        chatSessionMemoryController = sessionMemoryController;
        chatService = null;
        chatTokenUsageTracker.resetTotals();
    }

    private TextController requireTextController() {
        ModeController modeController = Controller.getCurrentModeController();
        if (modeController == null) {
            throw new IllegalStateException("Current mode controller is not available.");
        }
        TextController textController = modeController.getExtension(TextController.class);
        if (textController == null) {
            throw new IllegalStateException("Text controller is not available.");
        }
        return textController;
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

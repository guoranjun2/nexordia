package org.freeplane.plugin.ai.chat;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.ai.tools.AIToolSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public class AIChatPanel extends JPanel {

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
    private final JPanel messagesPanel;
    private final JScrollPane scrollPane;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private AIChatService chatService;
    private final JPopupMenu menuPopup;
    private final AIProviderConfiguration configuration;
    private final AIModelSelectionController modelSelectionController;
    private final ChatSessionMemoryController chatSessionMemoryController;
    private final ChatTokenUsageTracker chatTokenUsageTracker;
    private final JLabel tokenUsageLabel;

    public AIChatPanel() {
        setLayout(new BorderLayout());
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputArea = new JTextArea(3, 20);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        sendButton = new JButton("Send");
        menuPopup = buildMenuPopup();
        configuration = new AIProviderConfiguration();
        modelSelectionController = new AIModelSelectionController(configuration, new AIModelCatalog(configuration));
        modelSelectionController.setModelSelectionChangeListener(modelDescriptor -> chatService = null);
        chatSessionMemoryController = new ChatSessionMemoryController();
        tokenUsageLabel = new JLabel();
        chatTokenUsageTracker = new ChatTokenUsageTracker(this::updateTokenUsageLabel);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        FreeplaneToolBar toolbar = new FreeplaneToolBar(SwingConstants.HORIZONTAL);
        configureToolbar(toolbar);
        JPanel topBarContainer = new JPanel(new BorderLayout());
        topBarContainer.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
        topBarContainer.add(toolbar, BorderLayout.WEST);
        topBarContainer.add(tokenUsageLabel, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
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
        appendMessage(userMessage, true);
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
                    appendMessage(get(), false);
                } catch (Exception error) {
                    appendMessage(String.valueOf(error.getMessage()), false);
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
            appendMessage("Missing AI model selection.", false);
            return;
        }
        String providerName = selection.getProviderName();
        if (AIChatModelFactory.PROVIDER_NAME_OPENROUTER.equalsIgnoreCase(providerName)) {
            if (configuration.getOpenRouterKey() == null || configuration.getOpenRouterKey().isEmpty()) {
                appendMessage("Missing OpenRouter key setting.", false);
                return;
            }
        } else if (AIChatModelFactory.PROVIDER_NAME_GEMINI.equalsIgnoreCase(providerName)) {
            if (configuration.getGeminiKey() == null || configuration.getGeminiKey().isEmpty()) {
                appendMessage("Missing Gemini key setting.", false);
                return;
            }
        } else if (AIChatModelFactory.PROVIDER_NAME_OLLAMA.equalsIgnoreCase(providerName)) {
            if (!configuration.isOllamaEnabled()) {
                appendMessage("Ollama usage is disabled.", false);
                return;
            }
        } else {
            appendMessage("Unknown AI provider selection.", false);
            return;
        }
        chatService = AIChatServiceFactory.createService(new AIToolSet(), chatSessionMemoryController,
            chatTokenUsageTracker);
    }

    private void appendMessage(String text, boolean isFromUser) {
        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setText(text);
        messageArea.setBackground(isFromUser ? new Color(235, 235, 235) : new Color(245, 245, 245));
        messageArea.setAlignmentX(LEFT_ALIGNMENT);
        Dimension preferredSize = messageArea.getPreferredSize();
        messageArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));
        messageArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel messageWrapper = new JPanel(new BorderLayout());
        messageWrapper.setAlignmentX(LEFT_ALIGNMENT);
        messageWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height + 12));
        messageWrapper.add(messageArea, BorderLayout.CENTER);
        messageWrapper.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));

        messagesPanel.add(messageWrapper);
        messagesPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        messagesPanel.revalidate();
        messagesPanel.repaint();
        SwingUtilities.invokeLater(this::scrollToBottom);
    }

    private void startNewChat() {
        messagesPanel.removeAll();
        messagesPanel.revalidate();
        messagesPanel.repaint();
        chatSessionMemoryController.clearChatMemory();
        chatTokenUsageTracker.resetTotals();
    }

    private void updateTokenUsageLabel(ChatUsageTotals totals) {
        SwingUtilities.invokeLater(() -> tokenUsageLabel.setText(totals.formatStatusLine()));
    }

    private void scrollToBottom() {
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setValue(verticalBar.getMaximum());
    }

}

package org.freeplane.plugin.ai.chat;

import org.freeplane.plugin.ai.AIConfiguration;
import org.freeplane.plugin.ai.tools.AIToolSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public class AIChatPanel extends JPanel {

    private final JPanel messagesPanel;
    private final JScrollPane scrollPane;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private AIChatService chatService;

    public AIChatPanel() {
        setLayout(new BorderLayout());
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputArea = new JTextArea(3, 20);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        sendButton = new JButton("Send");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(event -> sendMessage());
        inputArea.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ctrl ENTER"), "sendMessage");
        inputArea.getActionMap().put("sendMessage", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                sendMessage();
            }
        });
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
        String providerName = AIConfiguration.getProviderName();
        String modelName = AIConfiguration.getModelName();
        if (providerName == null || providerName.isEmpty()) {
            appendMessage("Missing AI provider setting.", false);
            return;
        }
        if (modelName == null || modelName.isEmpty()) {
            appendMessage("Missing AI model name setting.", false);
            return;
        }
        chatService = AIChatServiceFactory.createService(new AIToolSet());
    }

    private void appendMessage(String text, boolean fromUser) {
        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setText(text);
        messageArea.setBackground(fromUser ? new Color(235, 235, 235) : new Color(245, 245, 245));
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

    private void scrollToBottom() {
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setValue(verticalBar.getMaximum());
    }
}

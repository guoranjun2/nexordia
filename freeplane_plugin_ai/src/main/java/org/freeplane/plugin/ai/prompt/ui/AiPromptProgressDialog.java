package org.freeplane.plugin.ai.prompt.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.freeplane.core.util.TextUtils;

public class AiPromptProgressDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JLabel promptNameLabel;
    private final JButton cancelButton;
    private final Runnable cancelAction;

    public AiPromptProgressDialog(Component owner, Icon promptIcon, Icon cancelIcon,
                                  String cancelTooltipText, Runnable cancelAction) {
        super(findOwnerWindow(owner));
        this.cancelAction = cancelAction;
        setTitle(TextUtils.getText("ai_prompt_running_title"));
        setModal(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancel();
            }
        });

        promptNameLabel = new JLabel();
        promptNameLabel.setIcon(promptIcon);
        cancelButton = new JButton(cancelIcon);
        cancelButton.setToolTipText(cancelTooltipText);
        cancelButton.addActionListener(event -> cancel());

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(promptNameLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.EAST);

        add(contentPanel, BorderLayout.CENTER);
        setMinimumSize(new Dimension(280, 90));
        installEscapeCancellation();
        pack();
        setLocationRelativeTo(owner);
    }

    public void showPrompt(String promptName) {
        promptNameLabel.setText(displayPromptName(promptName));
        pack();
        setVisible(true);
    }

    public void closeDialog() {
        setVisible(false);
        dispose();
    }

    private void cancel() {
        if (cancelAction != null) {
            cancelAction.run();
        }
    }

    private void installEscapeCancellation() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelPrompt");
        ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put("cancelPrompt", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cancel();
            }
        });
    }

    private String displayPromptName(String promptName) {
        String safePromptName = promptName == null ? "" : promptName.trim();
        return safePromptName.isEmpty()
            ? TextUtils.getText("ai_prompt_untitled")
            : safePromptName;
    }

    private static Window findOwnerWindow(Component owner) {
        if (owner instanceof Window) {
            return (Window) owner;
        }
        return owner == null ? null : SwingUtilities.getWindowAncestor(owner);
    }
}

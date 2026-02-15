package org.freeplane.core.resources.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.JPasswordField;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.LabelAndMnemonicSetter;
import org.freeplane.core.util.TextUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;

public class SecretProperty extends PropertyBean {
	private static final String SHOW_TEXT_KEY = "OptionPanel.secret.show";
	private static final String HIDE_TEXT_KEY = "OptionPanel.secret.hide";
	private static final String SHOW_TEXT_FALLBACK = "Show";
	private static final String HIDE_TEXT_FALLBACK = "Hide";
	private static final String SHOW_ICON_PATH = "/images/eye.svg?useAccentColor=true";
	private static final String HIDE_ICON_PATH = "/images/hide.svg?useAccentColor=true";

	private final JPasswordField passwordField;
	private final JButton toggleVisibilityButton;
	private final char maskEchoChar;
	private final Icon showIcon;
	private final Icon hideIcon;
	private boolean valueVisible;

	public SecretProperty(final String name) {
		super(name);
		passwordField = new JPasswordField();
		char configuredEchoChar = passwordField.getEchoChar();
		maskEchoChar = configuredEchoChar == 0 ? '\u2022' : configuredEchoChar;
		passwordField.setEchoChar(maskEchoChar);
		passwordField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				firePropertyChangeEvent();
			}
		});
		toggleVisibilityButton = new JButton();
		toggleVisibilityButton.setMaximumSize(new Dimension(1000, 1000));
		showIcon = loadIcon(SHOW_ICON_PATH);
		hideIcon = loadIcon(HIDE_ICON_PATH);
		toggleVisibilityButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				setValueVisible(!valueVisible);
			}
		});
		setValueVisible(false);
	}

	@Override
	public String getValue() {
		return new String(passwordField.getPassword());
	}

	@Override
	public JComponent getValueComponent() {
		return passwordField;
	}

	@Override
	public void appendToForm(DefaultFormBuilder builder) {
		Box row = Box.createHorizontalBox();
		row.add(passwordField);
		row.add(toggleVisibilityButton);
		appendToForm(builder, row);
	}

	@Override
	public void setEnabled(boolean enabled) {
		passwordField.setEnabled(enabled);
		toggleVisibilityButton.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	@Override
	public void setValue(String value) {
		passwordField.setText(value);
		passwordField.selectAll();
	}

	JButton getToggleVisibilityButton() {
		return toggleVisibilityButton;
	}

	boolean isValueVisible() {
		return valueVisible;
	}

	private void setValueVisible(boolean valueVisible) {
		this.valueVisible = valueVisible;
		passwordField.setEchoChar(valueVisible ? (char) 0 : maskEchoChar);
		passwordField.putClientProperty("JPasswordField.cutCopyAllowed", valueVisible ? Boolean.TRUE : Boolean.FALSE);
		String textKey = valueVisible ? HIDE_TEXT_KEY : SHOW_TEXT_KEY;
		String fallback = valueVisible ? HIDE_TEXT_FALLBACK : SHOW_TEXT_FALLBACK;
		Icon icon = valueVisible ? hideIcon : showIcon;
		if (icon != null) {
			toggleVisibilityButton.setIcon(icon);
			toggleVisibilityButton.setText(null);
		}
		else {
			toggleVisibilityButton.setIcon(null);
			LabelAndMnemonicSetter.setLabelAndMnemonic(toggleVisibilityButton, translatedText(textKey, fallback));
		}
		toggleVisibilityButton.setToolTipText(translatedText(textKey, fallback));
	}

	private String translatedText(String key, String fallback) {
		try {
			return TextUtils.getText(key);
		}
		catch (RuntimeException runtimeException) {
			return fallback;
		}
	}

	private Icon loadIcon(String resourcePath) {
		try {
			return ResourceController.getResourceController().getImageIcon(resourcePath);
		}
		catch (RuntimeException runtimeException) {
			return null;
		}
	}
}

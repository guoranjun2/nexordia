package org.freeplane.core.resources.components;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.JPasswordField;
import org.junit.Test;

public class SecretPropertyTest {
	@Test
	public void isMaskedByDefaultAndTogglesVisibility() {
		SecretProperty uut = new SecretProperty("api_key");
		JPasswordField passwordField = (JPasswordField) uut.getValueComponent();

		assertThat(uut.isValueVisible()).isFalse();
		assertThat(passwordField.getEchoChar()).isNotEqualTo((char) 0);
		assertThat(passwordField.getClientProperty("JPasswordField.cutCopyAllowed")).isEqualTo(Boolean.FALSE);

		uut.getToggleVisibilityButton().doClick();

		assertThat(uut.isValueVisible()).isTrue();
		assertThat(passwordField.getEchoChar()).isEqualTo((char) 0);
		assertThat(passwordField.getClientProperty("JPasswordField.cutCopyAllowed")).isEqualTo(Boolean.TRUE);

		uut.getToggleVisibilityButton().doClick();

		assertThat(uut.isValueVisible()).isFalse();
		assertThat(passwordField.getEchoChar()).isNotEqualTo((char) 0);
		assertThat(passwordField.getClientProperty("JPasswordField.cutCopyAllowed")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void preservesValueRoundtrip() {
		SecretProperty uut = new SecretProperty("api_key");

		uut.setValue("secret-token");

		assertThat(uut.getValue()).isEqualTo("secret-token");
	}
}

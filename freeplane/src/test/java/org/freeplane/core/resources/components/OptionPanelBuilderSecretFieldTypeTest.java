package org.freeplane.core.resources.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import org.junit.Test;

public class OptionPanelBuilderSecretFieldTypeTest {

	@Test
	public void createsSecretPropertyForSecretXmlTag() {
		OptionPanelBuilder uut = new OptionPanelBuilder();

		uut.load(new StringReader(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<preferences_structure xmlns=\"http://freeplane.sf.net/ui/preferences/1.0\">"
				+ "<tabbed_pane><tab name=\"plugins\"><separator name=\"ai\">"
				+ "<secret name=\"api_key\"/>"
				+ "<string name=\"service_address\"/>"
				+ "</separator></tab></tabbed_pane>"
				+ "</preferences_structure>"));

		IPropertyControlCreator secretCreator = findCreatorByPropertyName(uut.getRoot(), "api_key");
		IPropertyControlCreator stringCreator = findCreatorByPropertyName(uut.getRoot(), "service_address");

		assertThat(secretCreator).isNotNull();
		assertThat(secretCreator.createControl()).isInstanceOf(SecretProperty.class);
		assertThat(stringCreator).isNotNull();
		assertThat(stringCreator.createControl()).isInstanceOf(StringProperty.class);
	}

	private IPropertyControlCreator findCreatorByPropertyName(DefaultMutableTreeNode root, String propertyName) {
		Enumeration<?> nodes = root.preorderEnumeration();
		while (nodes.hasMoreElements()) {
			Object node = nodes.nextElement();
			if (!(node instanceof DefaultMutableTreeNode)) {
				continue;
			}
			Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
			if (!(userObject instanceof IPropertyControlCreator)) {
				continue;
			}
			IPropertyControlCreator creator = (IPropertyControlCreator) userObject;
			if (propertyName.equals(creator.getPropertyName())) {
				return creator;
			}
		}
		return null;
	}
}

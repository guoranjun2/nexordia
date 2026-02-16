package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.junit.Test;

public class AIModelSelectionControllerTest {
    @Test
    public void renderer_showsTextAfterFirstSlash_forSelectedValue() {
        AIModelSelectionController uut = newController();
        JComboBox<AIModelDescriptor> selector = uut.getModelSelectionComboBox();
        AIModelDescriptor descriptor = new AIModelDescriptor(
            "openrouter",
            "openai/gpt-4.1-mini",
            "OpenRouter: openai/gpt-4.1-mini",
            false
        );

        JLabel label = renderLabel(selector, descriptor, -1);

        assertThat(label.getText()).isEqualTo("gpt-4.1-mini");
    }

    @Test
    public void renderer_showsDisplayName_forDropdownItems() {
        AIModelSelectionController uut = newController();
        JComboBox<AIModelDescriptor> selector = uut.getModelSelectionComboBox();
        AIModelDescriptor descriptor = new AIModelDescriptor(
            "openrouter",
            "openai/gpt-4.1-mini",
            "OpenRouter: openai/gpt-4.1-mini",
            false
        );

        JLabel label = renderLabel(selector, descriptor, 0);

        assertThat(label.getText()).isEqualTo("OpenRouter: openai/gpt-4.1-mini");
    }

    @Test
    public void renderer_usesDisplayNameForPreferredSize_whenSelectedValueIsShortened() {
        AIModelSelectionController uut = newController();
        JComboBox<AIModelDescriptor> selector = uut.getModelSelectionComboBox();
        AIModelDescriptor descriptor = new AIModelDescriptor(
            "openrouter",
            "openai/gpt-4.1-mini",
            "OpenRouter: openai/gpt-4.1-mini",
            false
        );

        JLabel selectedLabel = renderLabel(selector, descriptor, -1);
        String selectedText = selectedLabel.getText();
        Dimension selectedPreferredSize = selectedLabel.getPreferredSize();
        JLabel dropdownLabel = renderLabel(selector, descriptor, 0);
        Dimension dropdownPreferredSize = dropdownLabel.getPreferredSize();

        assertThat(selectedText).isEqualTo("gpt-4.1-mini");
        assertThat(selectedPreferredSize.width).isGreaterThanOrEqualTo(dropdownPreferredSize.width);
    }

    @Test
    public void constructor_usesRegularComboboxSizing() {
        AIModelSelectionController uut = newController();
        JComboBox<AIModelDescriptor> selector = uut.getModelSelectionComboBox();

        assertThat(selector.getPrototypeDisplayValue()).isNull();
        assertThat(selector.getItemCount()).isZero();
    }

    @Test
    public void selectionChange_persistsProviderAndModelValue() {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        AIModelCatalog modelCatalog = mock(AIModelCatalog.class);
        AIModelSelectionController uut = new AIModelSelectionController(configuration, modelCatalog);
        JComboBox<AIModelDescriptor> selector = uut.getModelSelectionComboBox();
        AIModelDescriptor descriptor = new AIModelDescriptor(
            "openrouter",
            "openai/gpt-4.1-mini",
            "OpenRouter: openai/gpt-4.1-mini",
            false
        );

        selector.addItem(descriptor);
        selector.setSelectedItem(descriptor);

        verify(configuration, atLeastOnce()).setSelectedModelValue("openrouter|openai/gpt-4.1-mini");
    }

    private AIModelSelectionController newController() {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        AIModelCatalog modelCatalog = mock(AIModelCatalog.class);
        return new AIModelSelectionController(configuration, modelCatalog);
    }

    private JLabel renderLabel(JComboBox<AIModelDescriptor> selector, AIModelDescriptor descriptor, int index) {
        ListCellRenderer<? super AIModelDescriptor> renderer = selector.getRenderer();
        JList<AIModelDescriptor> list = new JList<>();
        Component component = renderer.getListCellRendererComponent(list, descriptor, index, false, false);
        return (JLabel) component;
    }
}

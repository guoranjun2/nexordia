package org.freeplane.plugin.ai.chat;

import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import org.freeplane.core.ui.LabelAndMnemonicSetter;
import org.freeplane.core.ui.textchanger.TranslatedElement;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;

class ChatToolAvailabilityMenu {
    private final Supplier<ChatToolAvailability> effectiveToolAvailabilitySupplier;
    private final Consumer<ChatToolAvailability> explicitUserSelectionHandler;
    private final EnumMap<ChatToolAvailability, JRadioButtonMenuItem> toolAvailabilityMenuItems =
        new EnumMap<ChatToolAvailability, JRadioButtonMenuItem>(ChatToolAvailability.class);

    ChatToolAvailabilityMenu(Supplier<ChatToolAvailability> effectiveToolAvailabilitySupplier,
                             Consumer<ChatToolAvailability> explicitUserSelectionHandler) {
        this.effectiveToolAvailabilitySupplier = effectiveToolAvailabilitySupplier;
        this.explicitUserSelectionHandler = explicitUserSelectionHandler;
    }

    void addTo(JPopupMenu menuPopup) {
        JMenu toolAvailabilityMenu = TranslatedElementFactory.createMenu(
            "OptionPanel." + ChatToolAvailabilitySettings.CHAT_TOOL_AVAILABILITY_PROPERTY);
        ButtonGroup buttonGroup = new ButtonGroup();
        addMenuItem(toolAvailabilityMenu, buttonGroup, ChatToolAvailability.EDITING);
        addMenuItem(toolAvailabilityMenu, buttonGroup, ChatToolAvailability.READING);
        addMenuItem(toolAvailabilityMenu, buttonGroup, ChatToolAvailability.DISABLED);
        menuPopup.add(toolAvailabilityMenu);
    }

    void refreshSelection() {
        ChatToolAvailability effectiveToolAvailability = effectiveToolAvailabilitySupplier.get();
        for (ChatToolAvailability toolAvailability : ChatToolAvailability.values()) {
            JRadioButtonMenuItem menuItem = toolAvailabilityMenuItems.get(toolAvailability);
            if (menuItem != null) {
                menuItem.setSelected(toolAvailability == effectiveToolAvailability);
            }
        }
    }

    private void addMenuItem(JMenu menu, ButtonGroup buttonGroup, ChatToolAvailability toolAvailability) {
        String labelKey = "OptionPanel." + ChatToolAvailabilitySettings.CHAT_TOOL_AVAILABILITY_PROPERTY + "."
            + toolAvailability.getPreferenceValue();
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem();
        buttonGroup.add(menuItem);
        LabelAndMnemonicSetter.setLabelAndMnemonic(menuItem, TextUtils.getRawText(labelKey));
        TranslatedElement.TEXT.setKey(menuItem, labelKey);
        TranslatedElementFactory.createTooltip(menuItem, labelKey + ".tooltip");
        menuItem.addActionListener(event -> explicitUserSelectionHandler.accept(toolAvailability));
        toolAvailabilityMenuItems.put(toolAvailability, menuItem);
        menu.add(menuItem);
    }
}

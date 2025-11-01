package org.freeplane.view.swing.map.outline;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;

class OutlineActions {
    private final OutlineActionTargetProvider provider;

    final Action navigateUp = new AbstractAction("Navigate Up") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().navigateUp(); }
    };
    final Action navigateDown = new AbstractAction("Navigate Down") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().navigateDown(); }
    };
    final Action navigatePageUp = new AbstractAction("Page Up") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().navigatePageUp(); }
    };
    final Action navigatePageDown = new AbstractAction("Page Down") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().navigatePageDown(); }
    };
    final Action goParent = new AbstractAction("Go to Parent") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().collapseOrGoToParent(); }
    };
    final Action goChild = new AbstractAction("Go to First Child") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().expandOrGoToChild(); }
    };
    final Action reduceExpansion = new AbstractAction("Reduce Expansion") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().reduceSelectedExpansion(); }
    };
    final Action expandMore = new AbstractAction("Expand More") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().expandSelectedMore(); }
    };
    final Action toggleExpand = new AbstractAction("Toggle Expand/Collapse") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().toggleExpandSelected(); }
    };
    final Action selectInMap = new AbstractAction("Select in Map") {
        @Override public void actionPerformed(ActionEvent e) { provider.getController().selectSelectedInMap(); }
    };
    final Action openPreferences = new AbstractAction("Preferences") {
        @Override public void actionPerformed(ActionEvent e) {
            final Controller controller = Controller.getCurrentController();
            final MModeController modeController = (MModeController) controller.getModeController(MModeController.MODENAME);
            modeController.showPreferences("Appearance", "outline_panel");
        }
    };

    OutlineActions(OutlineActionTargetProvider provider) {
        this.provider = provider;
        navigateUp.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("UP"));
        navigateDown.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("DOWN"));
        navigatePageUp.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("PAGE_UP"));
        navigatePageDown.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("PAGE_DOWN"));
        goParent.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("LEFT"));
        goChild.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("RIGHT"));
        reduceExpansion.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control LEFT"));
        expandMore.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control RIGHT"));
        toggleExpand.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("SPACE"));
        selectInMap.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("ENTER"));
    }

    void installOn(JComponent component, int condition) {
        InputMap inputMap = component.getInputMap(condition);
        ActionMap actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "navigateUp");
        actionMap.put("navigateUp", navigateUp);

        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "navigateDown");
        actionMap.put("navigateDown", navigateDown);

        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "navigatePageUp");
        actionMap.put("navigatePageUp", navigatePageUp);

        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "navigatePageDown");
        actionMap.put("navigatePageDown", navigatePageDown);

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "goParent");
        actionMap.put("goParent", goParent);

        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "goChild");
        actionMap.put("goChild", goChild);

        inputMap.put(KeyStroke.getKeyStroke("control LEFT"), "reduceExpansion");
        actionMap.put("reduceExpansion", reduceExpansion);

        inputMap.put(KeyStroke.getKeyStroke("control RIGHT"), "expandMore");
        actionMap.put("expandMore", expandMore);
    }

    JPopupMenu buildMenuLocalized() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(TranslatedElementFactory.createMenuItem(selectInMap, "outline.select.in.map"));
        menu.addSeparator();
        menu.add(TranslatedElementFactory.createMenuItem(navigateUp, "outline.navigate.up"));
        menu.add(TranslatedElementFactory.createMenuItem(navigateDown, "outline.navigate.down"));
        menu.add(TranslatedElementFactory.createMenuItem(navigatePageUp, "outline.page.up"));
        menu.add(TranslatedElementFactory.createMenuItem(navigatePageDown, "outline.page.down"));
        menu.addSeparator();
        menu.add(TranslatedElementFactory.createMenuItem(goParent, "outline.go.parent"));
        menu.add(TranslatedElementFactory.createMenuItem(goChild, "outline.go.child"));
        menu.addSeparator();
        menu.add(TranslatedElementFactory.createMenuItem(expandMore, "outline.expand.more"));
        menu.add(TranslatedElementFactory.createMenuItem(reduceExpansion, "outline.reduce.expansion"));
        menu.add(TranslatedElementFactory.createMenuItem(toggleExpand, "outline.toggle.expand"));
        menu.addSeparator();
        menu.add(TranslatedElementFactory.createMenuItem(openPreferences, "preferences"));
        return menu;
    }
}

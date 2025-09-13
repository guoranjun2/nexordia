package org.freeplane.view.swing.map.outline;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

class OutlineActions {
    private final OutlineActionTargetProvider provider;

    final Action navigateUp = new AbstractAction("Navigate Up") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().navigateUp(); }
    };
    final Action navigateDown = new AbstractAction("Navigate Down") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().navigateDown(); }
    };
    final Action navigatePageUp = new AbstractAction("Page Up") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().navigatePageUp(); }
    };
    final Action navigatePageDown = new AbstractAction("Page Down") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().navigatePageDown(); }
    };
    final Action goParent = new AbstractAction("Go to Parent") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().goToParent(); }
    };
    final Action goChild = new AbstractAction("Go to First Child") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().goToChild(); }
    };
    final Action reduceExpansion = new AbstractAction("Reduce Expansion") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().reduceSelectedExpansion(); }
    };
    final Action expandMore = new AbstractAction("Expand More") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().expandSelectedMore(); }
    };
    final Action toggleExpand = new AbstractAction("Toggle Expand/Collapse") {
        @Override public void actionPerformed(ActionEvent e) { provider.getTarget().toggleExpandSelected(); }
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
        return menu;
    }
}

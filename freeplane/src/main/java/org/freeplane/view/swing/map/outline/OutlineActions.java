package org.freeplane.view.swing.map.outline;

import javax.swing.*;
import java.awt.event.ActionEvent;

class OutlineActions {
    private final OutlineActionTarget target;

    OutlineActions(OutlineActionTarget target) {
        this.target = target;
    }

    void installOn(JComponent component, int condition) {
        InputMap inputMap = component.getInputMap(condition);
        ActionMap actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "navigateUp");
        actionMap.put("navigateUp", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { target.navigateUp(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "navigateDown");
        actionMap.put("navigateDown", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { target.navigateDown(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "navigatePageUp");
        actionMap.put("navigatePageUp", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { target.navigatePageUp(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "navigatePageDown");
        actionMap.put("navigatePageDown", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { target.navigatePageDown(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "goParent");
        actionMap.put("goParent", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { target.goToParent(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "goChild");
        actionMap.put("goChild", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { target.goToChild(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("control LEFT"), "reduceExpansion");
        actionMap.put("reduceExpansion", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { target.reduceSelectedExpansion(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("control RIGHT"), "expandMore");
        actionMap.put("expandMore", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { target.expandSelectedMore(); }
        });
    }
}


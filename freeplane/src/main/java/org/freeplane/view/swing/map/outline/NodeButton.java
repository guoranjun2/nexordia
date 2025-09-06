package org.freeplane.view.swing.map.outline;

import javax.swing.JButton;

class NodeButton extends JButton {
    private static final long serialVersionUID = 1L;
    private final TreeNode node;

    NodeButton(TreeNode node) {
        super();
        this.node = node;
    }

    TreeNode getNode() {
        return node;
    }
}



package org.freeplane.view.swing.map.outline;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.KeyStroke;

class BlockPanel extends JPanel {
	private static final long serialVersionUID = 1L;

    private final OutlineSelection selection;

    BlockPanel(List<TreeNode> nodes, int firstIdx, int rowHeight, ScrollableTreePanel parentPanel, int breadcrumbNodeCount, OutlineSelection selection) {
        setLayout(null);
        setOpaque(false);
        this.selection = selection;

        createNodeComponents(nodes, firstIdx, rowHeight, parentPanel, breadcrumbNodeCount);
    }

    private void createNodeComponents(List<TreeNode> nodes, int firstIdx, int rowHeight, ScrollableTreePanel parentPanel, int breadcrumbNodeCount) {
        int visibleButtonIndex = 0;
        for (int i = 0; i < nodes.size(); i++) {
            TreeNode node = nodes.get(i);
            int idx = firstIdx + i;

            if (idx >= breadcrumbNodeCount) {
                int y = visibleButtonIndex * rowHeight;
                createActionButton(node, y, rowHeight, parentPanel);
                visibleButtonIndex++;
            }
        }
    }

    @SuppressWarnings("serial")
    private void createActionButton(TreeNode node, int y, int rowHeight, ScrollableTreePanel parentPanel) {
        String buttonText = node.getTitle();
        NodeButton button = new NodeButton(node);
        button.setFont(button.getFont().deriveFont(8f));
        button.setText(buttonText);

        int computedLevel = node.getLevel();
        int actionX = parentPanel.geometry.calculateTextButtonX(computedLevel);

        button.setBounds(actionX, y, button.getPreferredSize().width, rowHeight);

        final AbstractAction selectAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentPanel.setSelectedNode(node, true);
                parentPanel.selectMapNodeById(node.getId());
            }
        };
        button.addActionListener(selectAction);

        InputMap im = button.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = button.getActionMap();
        im.put(KeyStroke.getKeyStroke("ENTER"), "selectMapNode");
        am.put("selectMapNode", selectAction);
        im.put(KeyStroke.getKeyStroke("SPACE"), "toggleExpand");
        am.put("toggleExpand", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentPanel.toggleExpandSelected();
            }
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                parentPanel.onContentButtonHovered(node);
            }
        });

        add(button);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        ScrollableTreePanel parentPanel = null;
        Container parent = getParent();
        while (parent != null && !(parent instanceof ScrollableTreePanel)) {
            parent = parent.getParent();
        }
        if (parent instanceof ScrollableTreePanel) {
            parentPanel = (ScrollableTreePanel) parent;
        }

        if (parentPanel != null) {
            SelectionPainter.paintForBlockPanel(this, parentPanel, selection, g);
        }
    }

}

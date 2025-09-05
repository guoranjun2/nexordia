
package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

class BlockPanel extends JPanel {

    private final int breadcrumbNodeCount;
    private final OutlineSelection selection;

    BlockPanel(List<FlatNode> nodes, int firstIdx, int rowHeight, int indent, ScrollableTreePanel parentPanel, int breadcrumbNodeCount, OutlineSelection selection) {
        setLayout(null);
        setOpaque(false);
        this.breadcrumbNodeCount = breadcrumbNodeCount;
        this.selection = selection;

        createNodeComponents(nodes, firstIdx, rowHeight, indent, parentPanel, breadcrumbNodeCount);
    }

    private void createNodeComponents(List<FlatNode> nodes, int firstIdx, int rowHeight, int indent, ScrollableTreePanel parentPanel, int breadcrumbNodeCount) {
        int visibleButtonIndex = 0;
        for (int i = 0; i < nodes.size(); i++) {
            FlatNode flat = nodes.get(i);
            int idx = firstIdx + i;

            if (idx >= breadcrumbNodeCount) {
                int y = visibleButtonIndex * rowHeight;
                createActionButton(flat, y, rowHeight, indent, parentPanel, idx);
                visibleButtonIndex++;
            }
        }
    }

    private void createActionButton(FlatNode flat, int y, int rowHeight, int indent, ScrollableTreePanel parentPanel, int idx) {
        String buttonText = flat.node.title;
        JButton button = new JButton();
        button.setFont(button.getFont().deriveFont(8f));
        button.setText(buttonText);

        int actionX = parentPanel.geometry.calculateTextButtonX(flat.depth);

        button.setBounds(actionX, y, button.getPreferredSize().width, rowHeight);

        
        button.putClientProperty("treeNode", flat.node);

        button.addActionListener(e -> {
            parentPanel.selectNodeById(flat.node.id);
        });

        // Map SPACE on the button to toggle expansion of the selected node
        javax.swing.InputMap im = button.getInputMap(JButton.WHEN_FOCUSED);
        javax.swing.ActionMap am = button.getActionMap();
        im.put(javax.swing.KeyStroke.getKeyStroke("SPACE"), "toggleExpand");
        am.put("toggleExpand", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                parentPanel.toggleExpandSelected();
            }
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                parentPanel.onContentButtonHovered(flat.node);
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
            for (Component comp : getComponents()) {
                if (comp instanceof JButton) {
                    JButton btn = (JButton) comp;
                    TreeNode buttonNode = (TreeNode) btn.getClientProperty("treeNode");
                    if (buttonNode != null && selection.isSelected(buttonNode)) {
                        
                        boolean isInBreadcrumb = parentPanel.getVisibleState().isNodeInBreadcrumbArea(buttonNode, parentPanel.geometry.rowHeight);

                        
                        if (!isInBreadcrumb) {
                            Icon icon = parentPanel.selectionIcon;

                            Point iconPosition = parentPanel.getNodePositioning().calculateSelectionIconPosition(buttonNode, comp.getBounds());
                            if (iconPosition != null) {
                                icon.paintIcon(this, g, iconPosition.x, iconPosition.y);
                            }
                        }
                    }
                }
            }
        }
    }
}

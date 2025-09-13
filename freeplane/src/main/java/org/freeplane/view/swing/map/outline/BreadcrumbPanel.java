package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@SuppressWarnings("serial")
class BreadcrumbPanel extends JPanel {
    private OutlineController controller;
    private OutlineSelection selection;
    private int currentBreadcrumbHeight = 0;
    private List<TreeNode> currentBreadcrumbNodes = new ArrayList<>();
	private OutlineSelectionBridge selectionBridge;

    BreadcrumbPanel() {
        setLayout(null);
        setOpaque(true);
    }

    void initialize(OutlineController controller, OutlineSelection selection) {
        this.controller = controller;
        this.selection = selection;
        setupKeyBindings();
    }

    @SuppressWarnings("serial")
    private void setupKeyBindings() {
        new OutlineActions(() -> controller).installOn(this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    void update(BreadcrumbState state) {
        currentBreadcrumbHeight = state.getBreadcrumbHeight();
        this.currentBreadcrumbNodes = new ArrayList<>(state.getBreadcrumbNodes());

        controller.setBreadcrumbAreaHeight(currentBreadcrumbHeight);
        updateNavigationButtons();
    }

    void updateNavigationButtons() {
        removeAll();
        for (int i = 0; i < currentBreadcrumbNodes.size(); i++) {
            TreeNode node = currentBreadcrumbNodes.get(i);
            int level = getNodeLevel(node);
            int y = i * controller.getRowHeight();

            int actionX = controller.calcTextButtonX(level);

            NodeButton breadcrumbButton = new NodeButton(node);
            breadcrumbButton.setFont(breadcrumbButton.getFont().deriveFont(8f));
            breadcrumbButton.setText(node.getTitle());
            breadcrumbButton.setBounds(actionX, y, breadcrumbButton.getPreferredSize().width, controller.getRowHeight());

            final TreeNode nodeToSelect = node;
            final int rowIndex = i;
            final AbstractAction selectAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.selectNode(nodeToSelect, true);
                    selectionBridge.selectMapNodeById(nodeToSelect.getId());
                }
            };
            breadcrumbButton.addActionListener(selectAction);

            InputMap im = breadcrumbButton.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = breadcrumbButton.getActionMap();
            im.put(KeyStroke.getKeyStroke("ENTER"), "selectMapNode");
            am.put("selectMapNode", selectAction);
            im.put(KeyStroke.getKeyStroke("SPACE"), "toggleExpand");
            am.put("toggleExpand", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.toggleNodeExpansion(nodeToSelect);
                }
            });

            breadcrumbButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    showNavigationButtonsForBreadcrumb(nodeToSelect, rowIndex);
                }
            });

            add(breadcrumbButton);

        }

        TreeNode hoveredNode = controller.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.getChildren().isEmpty()) {
            boolean isInBreadcrumb = controller.isNodeInBreadcrumbPath(hoveredNode, currentBreadcrumbNodes);

            if (isInBreadcrumb) {
                int hoveredRowIndex = controller.findNodeIndexInBreadcrumbPath(hoveredNode, currentBreadcrumbNodes);
                if (hoveredRowIndex >= 0) {
                    controller.attachNavigationNode(hoveredNode, true, hoveredRowIndex);
                }
            } else {
                boolean buttonsCurrentlyVisible = controller.areNavButtonsVisible();

                if (buttonsCurrentlyVisible) {
                    controller.attachNavigationNode(hoveredNode, false, -1);
                }
            }
        }

        revalidate();
        repaint();
	}


    Rectangle calculateBounds() {
        int width = controller.getViewportWidth();
        return new Rectangle(0, 0, width, currentBreadcrumbHeight);
    }

    List<TreeNode> getCurrentBreadcrumbNodes() {
        return new ArrayList<>(currentBreadcrumbNodes);
    }

    void setSelectionBridge(OutlineSelectionBridge bridge) {
        this.selectionBridge = bridge;
    }


    private int getNodeLevel(TreeNode node) {
        return controller.calculateNodeLevel(node);
    }

    private void showNavigationButtonsForBreadcrumb(TreeNode node, int rowIndex) {
        if (node.getChildren().isEmpty()) {
            return;
        }

        controller.attachNavigationNode(node, true, rowIndex);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        SelectionPainter.paintForBreadcrumbPanel(this, controller, selection, g);

        if (currentBreadcrumbHeight > 0) {
            g.setColor(getForeground());
            g.drawLine(0, currentBreadcrumbHeight - 1, getWidth(), currentBreadcrumbHeight - 1);
        }
    }
}

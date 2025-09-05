package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Icon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class BreadcrumbPanel extends JPanel {
    private OutlineController controller;
    private OutlineSelection selection;
    private int currentBreadcrumbHeight = 0;
    private List<TreeNode> currentBreadcrumbNodes = new ArrayList<>();

    public BreadcrumbPanel() {
        setLayout(null);
        setOpaque(true);
    }

    public void initialize(OutlineController controller, OutlineSelection selection) {
        this.controller = controller;
        this.selection = selection;
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("UP"), "outline.up");
        im.put(KeyStroke.getKeyStroke("DOWN"), "outline.down");
        im.put(KeyStroke.getKeyStroke("PAGE_UP"), "outline.pageUp");
        im.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "outline.pageDown");
        im.put(KeyStroke.getKeyStroke("LEFT"), "outline.parent");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "outline.child");
        im.put(KeyStroke.getKeyStroke("control LEFT"), "outline.reduce");
        im.put(KeyStroke.getKeyStroke("control RIGHT"), "outline.expandMore");

        am.put("outline.up", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.navigateUp(); }});
        am.put("outline.down", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.navigateDown(); }});
        am.put("outline.pageUp", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.navigatePageUp(); }});
        am.put("outline.pageDown", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.navigatePageDown(); }});
        am.put("outline.parent", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.goToParent(); }});
        am.put("outline.child", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.goToChild(); }});
        am.put("outline.reduce", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.reduceSelectedExpansion(); }});
        am.put("outline.expandMore", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.expandSelectedMore(); }});
    }

    public void update(BreadcrumbState state) {
        removeAll();

        currentBreadcrumbHeight = state.breadcrumbHeight;
        this.currentBreadcrumbNodes = new ArrayList<>(state.breadcrumbNodes);

        controller.setBreadcrumbAreaHeight(currentBreadcrumbHeight);
        updateNavigationButtons();
    }

	void updateNavigationButtons() {
		for (int i = 0; i < currentBreadcrumbNodes.size(); i++) {
            TreeNode node = currentBreadcrumbNodes.get(i);
            int depth = getNodeDepth(node);
            int y = i * controller.getRowHeight();

            int actionX = controller.calcTextButtonX(depth);

            JButton breadcrumbButton = new JButton();
            breadcrumbButton.setFont(breadcrumbButton.getFont().deriveFont(8f));
            breadcrumbButton.setText(node.title);
            breadcrumbButton.setBounds(actionX, y, breadcrumbButton.getPreferredSize().width, controller.getRowHeight());

            breadcrumbButton.putClientProperty("treeNode", node);

            final TreeNode nodeToSelect = node;
            final int rowIndex = i;
            breadcrumbButton.addActionListener(e -> controller.selectNodeById(nodeToSelect.id));

            // Map SPACE to toggle expansion for breadcrumb button's node
            javax.swing.InputMap im = breadcrumbButton.getInputMap(JButton.WHEN_FOCUSED);
            javax.swing.ActionMap am = breadcrumbButton.getActionMap();
            im.put(javax.swing.KeyStroke.getKeyStroke("SPACE"), "toggleExpand");
            am.put("toggleExpand", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
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
        if (hoveredNode != null && !hoveredNode.children.isEmpty()) {
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


    public Rectangle calculateBounds() {
        int width = controller.getViewportWidth();
        return new Rectangle(0, 0, width, currentBreadcrumbHeight);
    }

    public int getCurrentHeight() {
        return currentBreadcrumbHeight;
    }

    public List<TreeNode> getCurrentBreadcrumbNodes() {
        return new ArrayList<>(currentBreadcrumbNodes);
    }

    private int getNodeDepth(TreeNode node) {
        return controller.calculateNodeDepth(node);
    }

    private void showNavigationButtonsForBreadcrumb(TreeNode node, int rowIndex) {
        if (node.children.isEmpty()) {
            return;
        }

        controller.attachNavigationNode(node, true, rowIndex);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Component comp : getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                TreeNode buttonNode = (TreeNode) btn.getClientProperty("treeNode");
                if (buttonNode != null && selection.isSelected(buttonNode)) {
                    Icon icon = controller.getSelectionIcon();
                    Point iconPosition = controller.calculateSelectionIconPosition(buttonNode, comp.getBounds());
                    if (iconPosition != null) {
                        icon.paintIcon(this, g, iconPosition.x, iconPosition.y);
                    }
                }
            }
        }

        if (currentBreadcrumbHeight > 0) {
            g.setColor(Color.RED);
            g.drawLine(0, currentBreadcrumbHeight - 1, getWidth(), currentBreadcrumbHeight - 1);
        }
    }

    private class SelectionIndicator extends JPanel {
        private final SelectionCircleIcon icon;
        private final JButton targetButton;

        SelectionIndicator(SelectionCircleIcon icon, JButton targetButton) {
            this.icon = icon;
            this.targetButton = targetButton;
            setOpaque(false);
            setFocusable(false);


            setBounds(0, 0, icon.getIconWidth(), controller.getRowHeight());
        }

        private void updatePosition() {
            int iconX = targetButton.getX() - icon.getIconWidth();
            setBounds(iconX, targetButton.getY(), icon.getIconWidth(), controller.getRowHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int iconY = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g, 0, iconY);
        }
    }
}

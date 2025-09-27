package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;

@SuppressWarnings("serial")
class BreadcrumbPanel extends JPanel {
    private OutlineController controller;
    private OutlineSelection selection;
    private int preferredBreadcrumbHeight = 0;
    private List<TreeNode> currentBreadcrumbNodes = new ArrayList<>();
	private OutlineSelectionBridge selectionBridge;
	private Supplier<Color> backgroundColorSupplier;

    BreadcrumbPanel() {
        setLayout(null);
        setOpaque(true);
    }



    @Override
	public Color getBackground() {
		 if (backgroundColorSupplier != null) {
			final Color suppliedColor = backgroundColorSupplier.get();
			if(suppliedColor != null)
				return suppliedColor;
		 }
		 return super.getBackground();
	}

	void setBackgroundColorSupplier(Supplier<Color> backgroundColorSupplier) {
		this.backgroundColorSupplier = backgroundColorSupplier;
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

    void update(List<TreeNode> breadcrumbNodes) {
    	final int lastIndex = breadcrumbNodes.size() - 1;
		if(currentBreadcrumbNodes.size() != breadcrumbNodes.size()
				|| ! (breadcrumbNodes.isEmpty()
						|| breadcrumbNodes.get(lastIndex) == currentBreadcrumbNodes.get(lastIndex))) {
			preferredBreadcrumbHeight = OutlineGeometry.getInstance().calculateHeight(breadcrumbNodes);
			this.currentBreadcrumbNodes = breadcrumbNodes;

			controller.setBreadcrumbAreaHeight(preferredBreadcrumbHeight);
			updateNodeButtons();
		}
		else
			attachNavigationButtons();
    }

    void updateNodeButtons() {
        removeAll();
        final boolean useColoredOutlineItems = ResourceController.getResourceController().getBooleanProperty("useColoredOutlineItems", false);
        final int rowHeight = controller.getRowHeight();
        for (int i = 0; i < currentBreadcrumbNodes.size(); i++) {
            TreeNode node = currentBreadcrumbNodes.get(i);
            int level = getNodeLevel(node);
			int y = i * rowHeight;

            int actionX = controller.calcTextButtonX(level);

			NodeButton breadcrumbButton = new NodeButton(node, useColoredOutlineItems, Font.BOLD | Font.ITALIC);
            breadcrumbButton.setBounds(actionX, y, breadcrumbButton.getPreferredSize().width, rowHeight);

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
                    controller.toggleBreadcrumbNodeExpansion(nodeToSelect);
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

        attachNavigationButtons();

        revalidate();
        repaint();
	}



	private void attachNavigationButtons() {
		TreeNode hoveredNode = controller.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.getChildren().isEmpty()) {
            boolean isInBreadcrumb = controller.isHoveredNodeContainedInBreadcrumb()
            		&& currentBreadcrumbNodes.contains(hoveredNode);

            if (isInBreadcrumb) {
                int hoveredRowIndex = currentBreadcrumbNodes.indexOf(hoveredNode);
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
	}


    int getPreferredBreadcrumbHeight() {
        return preferredBreadcrumbHeight;
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
        if (preferredBreadcrumbHeight > 0) {
            g.setColor(UITools.getDisabledTextColorForBackground(getBackground()));
            g.drawLine(0, preferredBreadcrumbHeight - 1, getWidth(), preferredBreadcrumbHeight - 1);
        }
        SelectionPainter.paintForBreadcrumbPanel(this, controller, selection, g);
    }
}

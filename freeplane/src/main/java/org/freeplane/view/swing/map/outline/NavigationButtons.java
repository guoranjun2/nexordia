package org.freeplane.view.swing.map.outline;

import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;

import java.awt.Point;

class NavigationButtons {
    final JButton expandBtn;
    final JButton collapseBtn;
    final JButton expandMoreBtn;
    final JButton reduceBtn;

    private final OutlineGeometry geometry;
    private final ExpansionControls expansionControls;
    private JPanel currentParent;
	private TreeNode node;

    NavigationButtons(OutlineGeometry geometry, ExpansionControls expansionControls) {
        this.geometry = geometry;
        this.expansionControls = expansionControls;

        expandBtn = new JButton("▶");
        collapseBtn = new JButton("◀");
        expandMoreBtn = new JButton("▼");
        reduceBtn = new JButton("▲");

        configureNavigationButtons();
    }

    private void configureNavigationButtons() {
        configureNavButton(expandBtn, e -> {
            expansionControls.expandNode(node);
        });
        configureNavButton(collapseBtn, e -> {
            expansionControls.collapseNode(node);
        });
        configureNavButton(expandMoreBtn, e -> {
            expansionControls.expandNodeMore(node);
        });
        configureNavButton(reduceBtn, e -> {
            expansionControls.reduceNodeExpansion(node);
        });
    }

    private void configureNavButton(JButton button, ActionListener actionListener) {
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFont(button.getFont().deriveFont(OutlineGeometry.buttonFontSize()));
        button.setFocusable(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setVisible(false);
        button.addActionListener(actionListener);
    }

    public void attachToNode(TreeNode node, JPanel targetPanel, boolean isBreadcrumb, int rowIndex, int breadcrumbAreaHeight, NodePositioning nodePositioning) {
        this.node = node;
        detachFromCurrentParent();
		if (node.getChildren().isEmpty()) {
            return;
        }

        targetPanel.add(expandBtn);
        targetPanel.add(collapseBtn);
        targetPanel.add(expandMoreBtn);
        targetPanel.add(reduceBtn);

        currentParent = targetPanel;


        final boolean showFoldingButtons = ResourceController.getResourceController().getBooleanProperty("showOutlineFoldingButtons", true);
        if(showFoldingButtons) {
        	Point position = nodePositioning.calculateNavigationButtonPosition(node, isBreadcrumb, rowIndex, breadcrumbAreaHeight);
        	if (position == null) return;

        	int baseX = position.x;
        	int y = position.y;
        	int level = nodePositioning.calculateNodeLevel(node);
        	showButtons(baseX, y, level);
        }
    }

    private void detachFromCurrentParent() {
        hideNavigationButtons();
        if (currentParent != null) {
            if (expandBtn.getParent() == currentParent) {
                currentParent.remove(expandBtn);
            }
            if (collapseBtn.getParent() == currentParent) {
                currentParent.remove(collapseBtn);
            }
            if (expandMoreBtn.getParent() == currentParent) {
                currentParent.remove(expandMoreBtn);
            }
            if (reduceBtn.getParent() == currentParent) {
                currentParent.remove(reduceBtn);
            }
        }
    }

    private void showButtons(int baseX, int y, int level) {
		final int minimalLevel = ResourceController.getResourceController().getIntProperty("minimalFoldableOutlineLevel", 1);
        if (level >= minimalLevel) {
        	final JButton toggleButton = node.isExpanded() ? collapseBtn : expandBtn;
        	toggleButton.setBounds(baseX, y, geometry.navButtonWidth, geometry.rowHeight);
        	toggleButton.setVisible(true);
        }

        int expandX = baseX + geometry.navButtonWidth;
        expandMoreBtn.setBounds(expandX, y, geometry.navButtonWidth, geometry.rowHeight);
        expandMoreBtn.setVisible(true);

        if(node.isExpanded() && (level > 0  || node.getMaxExpansionLevel() > 1) ) {
        	int reduceX = expandX + geometry.navButtonWidth;
        	reduceBtn.setBounds(reduceX, y, geometry.navButtonWidth, geometry.rowHeight);
        	reduceBtn.setVisible(true);
        }
    }

    void hideNavigationButtons() {
        expandBtn.setVisible(false);
        collapseBtn.setVisible(false);
        expandMoreBtn.setVisible(false);
        reduceBtn.setVisible(false);
    }
}
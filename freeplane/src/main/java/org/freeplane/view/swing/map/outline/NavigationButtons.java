package org.freeplane.view.swing.map.outline;

import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.freeplane.core.resources.ResourceController;

class NavigationButtons {
    final JButton expandBtn;
    final JButton collapseBtn;
    final JButton expandMoreBtn;
    final JButton reduceBtn;

    private OutlineGeometry geometry;
    private final ExpansionControls expansionControls;
    private JPanel currentParent;
	private TreeNode node;
	private final OutlineDisplayMode displayMode;

    NavigationButtons(OutlineGeometry geometry, OutlineDisplayMode displayMode, ExpansionControls expansionControls) {
        this.geometry = geometry;
        this.expansionControls = expansionControls;
		this.displayMode = displayMode;

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
        applyButtonFont(button);
        button.setFocusable(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setVisible(false);
        button.addActionListener(actionListener);
    }

    private void applyButtonFont(JButton button) {
        button.setFont(button.getFont().deriveFont(geometry.getItemFontSize()));
    }

    public void attachToNode(TreeNode node, JPanel targetPanel, int rowIndex) {
        this.node = node;
        hideNavigationButtons();
		if (node.getChildren().isEmpty()) {
            return;
        }
		final boolean showFoldingButtons = ResourceController.getResourceController().getBooleanProperty("showOutlineFoldingButtons", true);
		if(showFoldingButtons) {

            targetPanel.add(expandBtn);
            targetPanel.add(collapseBtn);
            targetPanel.add(expandMoreBtn);
            targetPanel.add(reduceBtn);

            currentParent = targetPanel;


        	Point position = calculateNavigationButtonPosition(node, rowIndex);
        	if (position == null) return;

        	int baseX = position.x;
        	int y = position.y;
        	int level = node.getLevel();
        	showButtons(baseX, y, level);
        	NavigationButtonHider.INSTANCE.enable(targetPanel, this);
        }
    }

    private Point calculateNavigationButtonPosition(TreeNode node, int nodeIndex) {
        final int level = node.getLevel();
        final int baseX = geometry.calculateNavigationButtonX(level);
        final int rowHeight = geometry.rowHeight;
        int y = nodeIndex * rowHeight + 1;
		return new Point(baseX, y);
    }


    void hideNavigationButtons() {
        expandBtn.setVisible(false);
        collapseBtn.setVisible(false);
        expandMoreBtn.setVisible(false);
        reduceBtn.setVisible(false);
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
            currentParent.revalidate();
            currentParent.repaint();
        }
    }

    private void showButtons(int baseX, int y, int level) {
    	if(displayMode == OutlineDisplayMode.BOOKMARK)
    		return;
		final int minimalLevel = displayMode.getMinimalOutlineLevel();
		final int buttonY = y + 2;
        final int buttonHeight = geometry.rowHeight - 4;
		if (level >= minimalLevel) {
        	final JButton toggleButton = node.isExpanded() ? collapseBtn : expandBtn;
        	toggleButton.setBounds(baseX, buttonY, geometry.navButtonWidth, buttonHeight);
        	toggleButton.setVisible(true);
        }

        int expandX = baseX + geometry.navButtonWidth;
        expandMoreBtn.setBounds(expandX, buttonY, geometry.navButtonWidth, buttonHeight);
        expandMoreBtn.setVisible(true);

        if(node.isExpanded() && (level > 0  || node.getMaxExpansionLevel() > 1) ) {
        	int reduceX = expandX + geometry.navButtonWidth;
        	reduceBtn.setBounds(reduceX, buttonY, geometry.navButtonWidth, buttonHeight);
        	reduceBtn.setVisible(true);
        }
    }

    void updateGeometry(OutlineGeometry geometry) {
        this.geometry = geometry;
        applyButtonFont(expandBtn);
        applyButtonFont(collapseBtn);
        applyButtonFont(expandMoreBtn);
        applyButtonFont(reduceBtn);
    }
}

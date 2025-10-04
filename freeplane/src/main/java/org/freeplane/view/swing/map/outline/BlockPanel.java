
package org.freeplane.view.swing.map.outline;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.freeplane.core.resources.ResourceController;

class BlockPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG = Boolean.getBoolean("org.freeplane.view.swing.map.outline.BlockPanel.DEBUG");

    private final OutlineSelection selection;
    private final List<TreeNode> nodes;
    private final int rowHeight;
    private final ScrollableTreePanel parentPanel;

    BlockPanel(List<TreeNode> nodes, int rowHeight, ScrollableTreePanel parentPanel, OutlineSelection selection) {
        setLayout(null);
        setOpaque(false);
        this.selection = selection;
        this.nodes = new ArrayList<>(nodes);
        this.rowHeight = rowHeight;
        this.parentPanel = parentPanel;

        rebuildNodeButtons();
    }

    void rebuildNodeButtons() {
        removeAll();
        int visibleButtonIndex = 0;
        final boolean useColoredOutlineItems = ResourceController.getResourceController().getBooleanProperty("useColoredOutlineItems", false);
        for (TreeNode node : nodes) {
            int y = visibleButtonIndex * rowHeight;
            createNodeButton(node, y, rowHeight, useColoredOutlineItems, parentPanel);
            visibleButtonIndex++;
        }
        revalidate();
        repaint();
    }

    private void createNodeButton(TreeNode node, int y, int rowHeight, boolean useColoredOutlineItems, ScrollableTreePanel parentPanel) {
		NodeButton button = new NodeButton(node, useColoredOutlineItems);
        if(DEBUG && useColoredOutlineItems)
        	button.setText("" + getComponentCount());

        int computedLevel = node.getLevel();
        int actionX = OutlineGeometry.getInstance().calculateNodeButtonX(parentPanel.getDisplayMode().showsNavigationButtons(), computedLevel);

        button.setBounds(actionX, y, button.getPreferredSize().width, rowHeight);

        @SuppressWarnings("serial")
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
        @SuppressWarnings("serial")
        final AbstractAction expandAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentPanel.toggleExpandSelected();
            }
        };
		am.put("toggleExpand", expandAction);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                parentPanel.onContentButtonHovered(node);
            }
        });

        add(button);
    }


    @Override
	public int getWidth() {
    	final Container parent = getParent();
    	return parent != null ? parent.getWidth() : 0;
	}

	@Override
	protected void paintChildren(Graphics g) {
		 super.paintChildren(g);
		 if (parentPanel != null) {
			 SelectionPainter.paintForBlockPanel(this, parentPanel, selection, g);
		 }
	}
}

package org.freeplane.view.swing.map.outline;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.features.bookmarks.mindmapmode.NodeNavigator;
import org.freeplane.features.map.NodeModel;

class NodeButton extends JButton {
    private static final long serialVersionUID = 1L;
    private final TreeNode node;

    NodeButton(TreeNode node) {
        super();
        this.node = node;
        installNodePopupMenu();
    }

    TreeNode getNode() {
        return node;
    }

    private void installNodePopupMenu() {
        if (!(node instanceof MapTreeNode)) {
            return;
        }
        MapTreeNode mapTreeNode = (MapTreeNode) node;
        NodeModel nodeModel = mapTreeNode.getNodeModel();
        if (nodeModel == null) {
            return;
        }
        NodeNavigator nodeNavigator = new NodeNavigator(nodeModel);

        JPopupMenu popupMenu = new JPopupMenu();
        addGotoNodeMenuItem(popupMenu, nodeNavigator);
        addOpenAsRootDirectMenuItem(popupMenu, nodeNavigator);
        addOpenAsNewViewRootMenuItem(popupMenu, nodeNavigator);
        setComponentPopupMenu(popupMenu);
    }

    private void addGotoNodeMenuItem(JPopupMenu popupMenu, NodeNavigator nodeNavigator) {
        JMenuItem menuItem = TranslatedElementFactory.createMenuItem("bookmark.goto_node");
        menuItem.addActionListener(actionEvent -> nodeNavigator.open(false));
        popupMenu.add(menuItem);
    }

    private void addOpenAsRootDirectMenuItem(JPopupMenu popupMenu, NodeNavigator nodeNavigator) {
        JMenuItem menuItem = TranslatedElementFactory.createMenuItem("bookmark.open_as_root");
        menuItem.addActionListener(actionEvent -> nodeNavigator.open(true));
        popupMenu.add(menuItem);
    }

    private void addOpenAsNewViewRootMenuItem(JPopupMenu popupMenu, NodeNavigator nodeNavigator) {
        JMenuItem menuItem = TranslatedElementFactory.createMenuItem("bookmark.open_as_new_view_root");
        menuItem.addActionListener(actionEvent -> nodeNavigator.openAsNewView());
        popupMenu.add(menuItem);
    }
}

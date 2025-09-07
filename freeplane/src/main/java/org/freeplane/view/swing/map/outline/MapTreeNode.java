
package org.freeplane.view.swing.map.outline;

import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeDeletionEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import javax.swing.SwingUtilities;

/**
 * TreeNode that wraps a NodeModel and implements INodeView to receive
 * live updates when the underlying node changes.
 */
class MapTreeNode extends TreeNode implements INodeView {

    private final NodeModel nodeModel;
    private final OutlinePane outlinePane;

    MapTreeNode(NodeModel nodeModel, OutlinePane outlinePane) {
        super(getNodeText(nodeModel), nodeModel.getID());
        this.nodeModel = nodeModel;
        this.outlinePane = outlinePane;
    }

    private static String getNodeText(NodeModel nodeModel) {
        return TextController.getController().getShortPlainText(nodeModel);
    }

    @Override
    public void nodeChanged(NodeChangeEvent event) {
    	if (event.getNode() == nodeModel) {

    		String newText = getNodeText(nodeModel);
    		setTitle(newText);
    		SwingUtilities.invokeLater(() -> {
    			outlinePane.updateNodeTitle(this);
    		});
    	}
    }

    @Override
    public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
        if (parent == nodeModel) {

            MapTreeNode childTreeNode = createMapTreeNodeRecursively(child, outlinePane);

            add(childTreeNode, newIndex);
            childTreeNode.setParent(this);

            SwingUtilities.invokeLater(() -> {
            	outlinePane.rebuildFromNode(this);
            });
        }
    }

    private static MapTreeNode createMapTreeNodeRecursively(NodeModel nodeModel, OutlinePane outlinePane) {
        MapTreeNode treeNode = new MapTreeNode(nodeModel, outlinePane);
        nodeModel.addViewer(treeNode);

        for (NodeModel childNode : nodeModel.getChildren()) {
            MapTreeNode childTreeNode = createMapTreeNodeRecursively(childNode, outlinePane);
            treeNode.addChild(childTreeNode);
        }

        return treeNode;
    }

    @Override
    public void onNodeDeleted(NodeDeletionEvent nodeDeletionEvent) {
        NodeModel deletedNode = nodeDeletionEvent.node;


        MapTreeNode toRemove = null;
        for (TreeNode child : getChildren()) {
            if (child instanceof MapTreeNode) {
                MapTreeNode mapChild = (MapTreeNode) child;
                if (mapChild.nodeModel == deletedNode) {
                    toRemove = mapChild;
                    break;
                }
            }
        }

        if (toRemove != null) {
        	if(outlinePane.isSelected(toRemove))
        		outlinePane.setSelected(toRemove.getParent());
            deletedNode.removeViewer(toRemove);


            remove(toRemove);
            toRemove.setParent(null);


            toRemove.cleanupListeners();


            SwingUtilities.invokeLater(() -> {
            	outlinePane.rebuildFromNode(this);
            });
        }
    }

    @Override
	public
    boolean hasStandardLayoutWithRootNode(NodeModel root) {
        return false;
    }

    @Override
	public
    boolean isTopOrLeft() {
        return true;
    }

    /**
     * Recursively cleanup all INodeView listeners for this node and its children.
     * Called when the tree is being destroyed or replaced.
     */
    void cleanupListeners() {

        if (nodeModel != null) {
            nodeModel.removeViewer(this);
        }


        for (TreeNode child : getChildren()) {
            if (child instanceof MapTreeNode) {
                ((MapTreeNode) child).cleanupListeners();
            }
        }
    }

    boolean isContainedIn(MapAwareOutlinePane pane) {
    	return pane== outlinePane;
    }
}

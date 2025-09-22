
package org.freeplane.view.swing.map.outline;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.freeplane.core.ui.components.TextIcon;
import org.freeplane.core.util.ColorUtils;
import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeDeletionEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;
import org.freeplane.features.text.TextController;

/**
 * TreeNode that wraps a NodeModel and implements INodeView to receive
 * live updates when the underlying node changes.
 */
class MapTreeNode extends TreeNode implements INodeView {

    private final NodeModel nodeModel;
    private final OutlinePane outlinePane;
    private Icon icon;
	private final NodeStyleController styleController;
	private final Color mapBackground;

    MapTreeNode(NodeModel nodeModel, OutlinePane outlinePane, NodeStyleController styleController, Color mapBackground) {
        super(nodeModel.createID(), null);
        this.nodeModel = nodeModel;
        this.outlinePane = outlinePane;
		this.styleController = styleController;
		this.mapBackground = mapBackground;
        setTitleSupplier(this::getNodeText);
    }

    public MapTreeNode(MapTreeNode parent, NodeModel child, OutlinePane pane) {
    	this(child, pane, parent.styleController, parent.mapBackground);
    	parent.addChild(this);
	}

	NodeModel getNodeModel() {
        return nodeModel;
    }

    private String getNodeText() {
        return TextController.getController().getShortPlainText(nodeModel);
    }

    @Override
    public void nodeChanged(NodeChangeEvent event) {
    	if (event.getNode() == nodeModel) {
    		update();
    		SwingUtilities.invokeLater(() -> {
    			outlinePane.updateNodeTitle(this);
    		});
    	}
    }

	@Override
	void update() {
        this.icon = null;
        super.update();
    }


    @Override
    public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
        if (parent == nodeModel) {

            MapTreeNode childTreeNode = createMapTreeNodeRecursively(child);

            add(childTreeNode, newIndex);
            childTreeNode.setParent(this);

            SwingUtilities.invokeLater(() -> {
            	outlinePane.rebuildFromNode(this);
            });
        }
    }

    private MapTreeNode createMapTreeNodeRecursively(NodeModel nodeModel) {
        MapTreeNode treeNode = new MapTreeNode(nodeModel, outlinePane, styleController, mapBackground);
        nodeModel.addViewer(treeNode);

        for (NodeModel childNode : nodeModel.getChildren()) {
            MapTreeNode childTreeNode = createMapTreeNodeRecursively(childNode);
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

	public Icon getIcon(JComponent component) {
		if(icon ==  null)
			icon =createIcon(component);
		return icon;
	}



	private Icon createIcon(JComponent component) {
		Color color = styleController.getColor(nodeModel, StyleOption.FOR_UNSELECTED_NODE);
		Color backgroundColor = styleController.getBackgroundColor(nodeModel, StyleOption.FOR_UNSELECTED_NODE);
        if (backgroundColor != null) {
            if (backgroundColor.getAlpha() < 255) {
            	backgroundColor = ColorUtils.blendColors(backgroundColor, mapBackground);
            }
        }
        else
        	backgroundColor = mapBackground;
		final TextIcon textIcon = new TextIcon(getTitle(), component.getFontMetrics(component.getFont()));
		textIcon.setIconTextColor(color);
		textIcon.setIconBackgroundColor(backgroundColor);
		return textIcon;
	}

}


package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.List;

class TreeNode {
    private String title;
    private final String id;
    private final List<TreeNode> children = new ArrayList<>();
    private int expansionLevel = 0;
    private TreeNode parent = null;

    TreeNode(String title, String id) {
        this.setTitle(title);
        this.id = id;
    }

    void setTitle(String newTitle) {
        this.title = newTitle;
    }

    void addChild(TreeNode child) {
        child.setParent(this);
        getChildren().add(child);
        if (getExpansionLevel() > 0) {
            child.setExpansionLevel(getExpansionLevel() - 1);
            child.applyExpansionLevel(child.getExpansionLevel());
        }
    }

    void applyExpansionLevel(int level) {
        this.setExpansionLevel(level);
        if (level > 0) {
            for (TreeNode child : getChildren()) {
                child.applyExpansionLevel(level - 1);
            }
        } else {
            for (TreeNode child : getChildren()) {
                child.applyExpansionLevel(0);
            }
        }
    }

    int getMaxExpansionDepth() {
        if (getExpansionLevel() == 0 || getChildren().isEmpty()) {
            return 0;
        }
        int maxDepth = 0;
        for (TreeNode child : getChildren()) {
            maxDepth = Math.max(maxDepth, 1 + child.getMaxExpansionDepth());
        }
        return maxDepth;
    }

    boolean isExpanded() {
        return getExpansionLevel() > 0;
    }

    int getExpansionLevel() {
        return expansionLevel;
    }


    @Override
	public String toString() {
        return "TreeNode [title=" + getTitle() + "]";
    }

	TreeNode getParent() {
		return parent;
	}

	void setParent(TreeNode parent) {
		this.parent = parent;
	}

	private void setExpansionLevel(int expansionLevel) {
		this.expansionLevel = expansionLevel;
	}

	List<TreeNode> getChildren() {
		return children;
	}

	void add(MapTreeNode node, int index) {
		if (index < children.size()) {
		    children.add(index, node);
		} else {
		    children.add(node);
		}
	}

	boolean remove(MapTreeNode toRemove) {
		return children.remove(toRemove);
	}

	String getId() {
		return id;
	}

	String getTitle() {
		return title;
	}

	TreeNode findVisibleAncestorOrSelf() {
		TreeNode node = this;
		for(;;) {
			TreeNode parent = node.getParent();
			if(parent == null || parent.isExpanded())
				return node;
			node = parent;
		}
	}
}

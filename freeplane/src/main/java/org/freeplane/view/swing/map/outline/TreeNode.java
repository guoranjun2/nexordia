
package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private String title;
    private final String id;
    private final List<TreeNode> children = new ArrayList<>();
    private int expansionLevel = 0;
    private TreeNode parent = null;

    public TreeNode(String title, String id) {
        this.setTitle(title);
        this.id = id;
    }

    public void setTitle(String newTitle) {
        this.title = newTitle;
    }

    public void addChild(TreeNode child) {
        child.setParent(this);
        getChildren().add(child);
        if (getExpansionLevel() > 0) {
            child.setExpansionLevel(getExpansionLevel() - 1);
            child.applyExpansionLevel(child.getExpansionLevel());
        }
    }

    public boolean removeChild(TreeNode child) {
        child.setParent(null);
        return getChildren().remove(child);
    }

    public void applyExpansionLevel(int level) {
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

    public int getMaxExpansionDepth() {
        if (getExpansionLevel() == 0 || getChildren().isEmpty()) {
            return 0;
        }
        int maxDepth = 0;
        for (TreeNode child : getChildren()) {
            maxDepth = Math.max(maxDepth, 1 + child.getMaxExpansionDepth());
        }
        return maxDepth;
    }

    public boolean isExpanded() {
        return getExpansionLevel() > 0;
    }

    public int getExpansionLevel() {
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

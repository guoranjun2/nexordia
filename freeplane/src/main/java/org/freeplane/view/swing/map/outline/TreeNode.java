
package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.function.Consumer;

class TreeNode {
    private String title;
    private final String id;
    private final List<TreeNode> children = new ArrayList<>();
    private int expansionLevel = -1;
    private TreeNode parent = null;
    private int level = 0;

    TreeNode(String title, String id) {
        this.setTitle(title);
        this.id = id;
    }

    void setTitle(String newTitle) {
        this.title = newTitle;
    }

    void addChild(TreeNode child) {
        child.setParent(this);
        children.add(child);
        if (expansionLevel >= 0) {
            child.setExpansionLevel(expansionLevel - 1);
            child.applyExpansionLevel(child.expansionLevel);
        }
    }

    void applyExpansionLevel(int level) {
        this.setExpansionLevel(level);
        if (level >= 0) {
            for (TreeNode child : getChildren()) {
                child.applyExpansionLevel(level - 1);
            }
        } else {
            for (TreeNode child : getChildren()) {
                child.applyExpansionLevel(-1);
            }
        }
    }

    int getMaxExpansionLevel() {
        if (expansionLevel <= 0 || getChildren().isEmpty()) {
            return expansionLevel;
        }
        int maxLevel = 0;
        for (TreeNode child : getChildren()) {
            maxLevel = Math.max(maxLevel, 1 + child.getMaxExpansionLevel());
        }
        return maxLevel;
    }

    boolean isExpanded() {
        return expansionLevel > 0;
    }

    boolean isVisible() {
        return expansionLevel >= 0;
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
        if(parent != null)
        	refreshLevelsRecursively();
    }

	private void setExpansionLevel(int expansionLevel) {
		this.expansionLevel = expansionLevel;
	}

    List<TreeNode> getChildren() { return Collections.unmodifiableList(children); }

    int childCount() { return children.size(); }
    TreeNode childAt(int index) { return children.get(index); }
    void forEachChild(Consumer<TreeNode> action) { children.forEach(action); }

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

        for(TreeNode node = this;
        		node != null;
        		node = node.getParent()) {
            if(node.isVisible())
                return node;
        }
        return null;
    }

    int getLevel() {
        return level;
    }

    private void refreshLevelsRecursively() {
    	level = parent.level + 1;
        for (TreeNode child : children) {
            child.refreshLevelsRecursively();
        }
    }
}

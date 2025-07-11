/*
 * Created on 8 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.List;

class TreeNode {
        final String title;
    final String id;
        final List<TreeNode> children = new ArrayList<>();
    int expansionLevel = 0;
    TreeNode parent = null;

    TreeNode(String title, String id) {
        this.title = title;
        this.id = id;
    }

        public void addChild(TreeNode child) {
            child.parent = this;
            children.add(child);
            if (expansionLevel > 0) {
                child.expansionLevel = expansionLevel - 1;
                child.applyExpansionLevel(child.expansionLevel);
            }
        }

        public boolean removeChild(TreeNode child) {
            child.parent = null;
            return children.remove(child);
        }

        public void applyExpansionLevel(int level) {
            this.expansionLevel = level;
            if (level > 0) {
                for (TreeNode child : children) {
                    child.applyExpansionLevel(level - 1);
                }
            } else {
                for (TreeNode child : children) {
                    child.applyExpansionLevel(0);
                }
            }
        }

        public int getMaxExpansionDepth() {
        if (expansionLevel == 0 || children.isEmpty()) {
                return 0;
            }
            int maxDepth = 0;
            for (TreeNode child : children) {
                maxDepth = Math.max(maxDepth, 1 + child.getMaxExpansionDepth());
            }
            return maxDepth;
        }

    public TreeNode createChild(String baseName, String childId) {
        TreeNode newChild = new TreeNode(baseName, childId);
            addChild(newChild);
            return newChild;
        }

    public boolean isExpanded() {
        return expansionLevel > 0;
    }

	@Override
	public String toString() {
		return "TreeNode [title=" + title + "]";
	}


}
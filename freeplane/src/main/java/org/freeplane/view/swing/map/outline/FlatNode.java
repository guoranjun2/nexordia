
package org.freeplane.view.swing.map.outline;

class FlatNode {
    final TreeNode node;
    final int depth;

    FlatNode(TreeNode node, int depth) {
        this.node = node;
        this.depth = depth;
    }

	@Override
	public String toString() {
		return "FlatNode [node=" + node + ", depth=" + depth + "]";
	}
}
package org.freeplane.view.swing.map.outline;

interface ExpansionHandler {
    void expandNode(TreeNode node);
    void collapseNode(TreeNode node);
    void expandNodeMore(TreeNode node);
    void reduceNodeExpansion(TreeNode node);
}


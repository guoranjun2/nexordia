
package org.freeplane.view.swing.map.outline;

class DemoTreeFactory {
    private static int nodeCounter = 1;

    static TreeNode createDemoRoot() {
        nodeCounter = 1; 
        TreeNode root = new TreeNode("Root", "root");
        createRandomChildren(root, 1, 5);
        return root;
    }

    private static void createRandomChildren(TreeNode parent, int currentLevel, int maxLevels) {
        if (currentLevel >= maxLevels) {
            return;
        }

        int numChildren = 2 + (int)(Math.random() * 4);

        for (int i = 1; i <= numChildren; i++) {
            String childId = "Child " + nodeCounter++;
            
            String childTitle = "Level " + currentLevel + " " + childId;

            TreeNode child = new TreeNode(childTitle, childId);

            boolean shouldHaveChildren = Math.random() < 0.7 && currentLevel < maxLevels - 1;

            if (shouldHaveChildren) {
                createRandomChildren(child, currentLevel + 1, maxLevels);
            }

            parent.addChild(child);
        }
    }
}
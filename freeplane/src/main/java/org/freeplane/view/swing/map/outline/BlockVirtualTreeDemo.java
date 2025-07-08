/*
 * Created on 6 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;
import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class BlockVirtualTreeDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Block Virtual Tree Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ScrollableTreePanel panel = new ScrollableTreePanel(DemoTreeFactory.createDemoRoot());
            OutlinePane outlinePane = new OutlinePane(panel);

            frame.setLayout(new BorderLayout());
            frame.add(outlinePane, BorderLayout.CENTER);
            frame.setSize(700, 600);
            frame.setVisible(true);
        });
    }
}

class DemoTreeFactory {
    private static int nodeCounter = 1;
    
    static TreeNode createDemoRoot() {
        nodeCounter = 1; // Reset counter for each new tree
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
            String childId = parent.id + "-child-" + i;
            // Make title unique using global counter
            String childTitle = "Level " + currentLevel + " Child " + nodeCounter++;
            TreeNode child = new TreeNode(childTitle, childId);

            boolean shouldHaveChildren = Math.random() < 0.7 && currentLevel < maxLevels - 1;

            if (shouldHaveChildren) {
                createRandomChildren(child, currentLevel + 1, maxLevels);
            }

            parent.addChild(child);
        }
    }
}
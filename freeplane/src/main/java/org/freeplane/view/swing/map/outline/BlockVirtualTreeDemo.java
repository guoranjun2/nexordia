
package org.freeplane.view.swing.map.outline;
import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class BlockVirtualTreeDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Block Virtual Tree Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            
            OutlinePane outlinePane = new OutlinePane(DemoTreeFactory.createDemoRoot());

            frame.setLayout(new BorderLayout());
            frame.add(outlinePane, BorderLayout.CENTER);
            frame.setSize(700, 600);
            frame.setVisible(true);
        });
    }
}
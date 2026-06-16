/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2026
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.map;

import java.awt.Graphics2D;

class MapCloudPainter {
    static final String CLOUD_IMAGE_OPACITY_PROPERTY = "cloud_image_opacity";

    void paintClouds(final Graphics2D g2, final NodeView root) {
        final Graphics2D g = (Graphics2D) g2.create();
        try {
            g.translate(root.getX(), root.getY());
            root.paintCloudTree(g);
        }
        finally {
            g.dispose();
        }
    }

    void paintCloudTexts(final Graphics2D g2, final NodeView root) {
        final Graphics2D g = (Graphics2D) g2.create();
        try {
            g.translate(root.getX(), root.getY());
            root.paintCloudTextTree(g);
        }
        finally {
            g.dispose();
        }
    }
}

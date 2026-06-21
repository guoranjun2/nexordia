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
package org.freeplane.view.swing.map.overview;

import java.awt.image.BufferedImage;

import javax.swing.JComponent;

interface MapBackgroundVideoPlayer {
    JComponent component();
    boolean isReady();
    void play();
    void pause();
    void dispose();
    default boolean requiresHostWindowTransparency() {
        return false;
    }
    default boolean isPaintedInFront() {
        return false;
    }
    default void updateForegroundOpacity() {
    }
    default void setOverlay(final BufferedImage overlay) {
    }
}

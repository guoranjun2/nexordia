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

import java.net.URI;
import java.util.Locale;

import org.freeplane.core.resources.ResourceController;

public final class MapBackgroundVideo {
    public static final String FOREGROUND_OPACITY_PROPERTY = "background_video_foreground_opacity";
    public static final int DEFAULT_FOREGROUND_OPACITY = 35;

    private MapBackgroundVideo() {
    }

    public static boolean isSupportedVideoUri(final URI uri) {
        if(uri == null)
            return false;
        final String scheme = uri.getScheme();
        if(scheme != null && ! isSupportedScheme(scheme))
            return false;
        final String path = uri.getPath();
        if(path == null)
            return false;
        final String normalizedPath = path.toLowerCase(Locale.ROOT);
        return normalizedPath.endsWith(".mp4")
                || normalizedPath.endsWith(".m4v")
                || normalizedPath.endsWith(".mov");
    }

    private static boolean isSupportedScheme(final String scheme) {
        return "file".equalsIgnoreCase(scheme)
                || "http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme);
    }

    public static double foregroundOpacity() {
        return foregroundOpacity(ResourceController.getResourceController()
                .getProperty(FOREGROUND_OPACITY_PROPERTY, Integer.toString(DEFAULT_FOREGROUND_OPACITY)));
    }

    public static double foregroundOpacity(final String value) {
        if(value == null)
            return DEFAULT_FOREGROUND_OPACITY / 100d;
        try {
            final String trimmed = value.trim();
            final String number = trimmed.endsWith("%") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
            final double parsed = Double.parseDouble(number);
            final boolean fractionalValue = parsed > 0d && parsed < 1d || parsed == 1d && number.indexOf('.') >= 0;
            final double percent = fractionalValue ? parsed * 100d : parsed;
            return Math.max(0d, Math.min(100d, percent)) / 100d;
        }
        catch (final NumberFormatException e) {
            return DEFAULT_FOREGROUND_OPACITY / 100d;
        }
    }
}

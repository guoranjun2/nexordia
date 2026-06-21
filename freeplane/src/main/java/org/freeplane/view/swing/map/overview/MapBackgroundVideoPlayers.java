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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;

import javax.swing.JComponent;

import org.freeplane.core.util.LogUtils;

class MapBackgroundVideoPlayers {
    private static final String MAC_NATIVE_PLAYER = "org.freeplane.plugin.macos.MacNativeMapBackgroundVideo";
    private static final String MAC_NATIVE_ENABLED_PROPERTY = "org.freeplane.backgroundVideo.nativeMac";

    private MapBackgroundVideoPlayers() {
    }

    static MapBackgroundVideoPlayer create(final JComponent host, final URI uri,
                                           final Runnable readyHandler, final Runnable nativeErrorHandler,
                                           final Runnable javaFxErrorHandler) {
        final MapBackgroundVideoPlayer nativePlayer = createNativePlayer(host, uri, readyHandler, nativeErrorHandler);
        if(nativePlayer != null)
            return nativePlayer;
        LogUtils.info("background video player: JavaFX JFXPanel");
        return createJavaFx(uri, readyHandler, javaFxErrorHandler);
    }

    static MapBackgroundVideoPlayer createJavaFx(final URI uri, final Runnable readyHandler,
                                                 final Runnable errorHandler) {
        return new JavaFxMapBackgroundVideo(uri, readyHandler, errorHandler);
    }

    private static MapBackgroundVideoPlayer createNativePlayer(final JComponent host, final URI uri,
                                                               final Runnable readyHandler, final Runnable errorHandler) {
        if(! System.getProperty("os.name", "").startsWith("Mac"))
            return null;
        if(! Boolean.parseBoolean(System.getProperty(MAC_NATIVE_ENABLED_PROPERTY, "true")))
            return null;
        try {
            final Class<?> playerClass = Class.forName(MAC_NATIVE_PLAYER);
            final Constructor<?> constructor = playerClass.getConstructor(JComponent.class, URI.class, Runnable.class, Runnable.class);
            return new ReflectivePlayer(constructor.newInstance(host, uri, readyHandler, errorHandler));
        }
        catch (final Throwable e) {
            LogUtils.warn("macOS native background video is unavailable", e);
            return null;
        }
    }

    private static class ReflectivePlayer implements MapBackgroundVideoPlayer {
        private final Object delegate;
        private final JComponent component;
        private final Method isReady;
        private final Method play;
        private final Method pause;
        private final Method dispose;
        private final Method isPaintedInFront;
        private final Method updateForegroundOpacity;

        ReflectivePlayer(final Object delegate) throws NoSuchMethodException {
            this.delegate = delegate;
            this.component = (JComponent) delegate;
            final Class<?> type = delegate.getClass();
            isReady = type.getMethod("isReady");
            play = type.getMethod("play");
            pause = type.getMethod("pause");
            dispose = type.getMethod("dispose");
            isPaintedInFront = optionalMethod(type, "isPaintedInFront");
            updateForegroundOpacity = optionalMethod(type, "updateForegroundOpacity");
        }

        @Override
        public JComponent component() {
            return component;
        }

        @Override
        public boolean isReady() {
            try {
                return (Boolean) isReady.invoke(delegate);
            }
            catch (final Exception e) {
                LogUtils.warn(e);
                return false;
            }
        }

        @Override
        public void play() {
            invoke(play);
        }

        @Override
        public void pause() {
            invoke(pause);
        }

        @Override
        public void dispose() {
            invoke(dispose);
        }

        @Override
        public boolean isPaintedInFront() {
            if(isPaintedInFront == null)
                return false;
            try {
                return (Boolean) isPaintedInFront.invoke(delegate);
            }
            catch (final Exception e) {
                LogUtils.warn(e);
                return false;
            }
        }

        @Override
        public void updateForegroundOpacity() {
            if(updateForegroundOpacity != null)
                invoke(updateForegroundOpacity);
        }

        private void invoke(final Method method) {
            try {
                method.invoke(delegate);
            }
            catch (final Exception e) {
                LogUtils.warn(e);
            }
        }

        private Method optionalMethod(final Class<?> type, final String name) {
            try {
                return type.getMethod(name);
            }
            catch (final NoSuchMethodException e) {
                return null;
            }
        }
    }
}

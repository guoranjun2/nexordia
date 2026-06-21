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
package org.freeplane.plugin.macos;

import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.freeplane.core.util.LogUtils;
import org.freeplane.view.swing.map.MapBackgroundVideo;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class MacNativeMapBackgroundVideo extends JPanel {
    private static final long serialVersionUID = 1L;

    static {
        loadFramework("Foundation");
        loadFramework("AppKit");
        loadFramework("AVFoundation");
        loadFramework("QuartzCore");
    }

    private static final String NATIVE_WINDOW_ENABLED_PROPERTY = "org.freeplane.backgroundVideo.nativeMac.window";
    private static final String NATIVE_WINDOW_FRONT_PROPERTY = "org.freeplane.backgroundVideo.nativeMac.windowFront";
    private static final String NATIVE_WINDOW_FRONT_TEST_PROPERTY = "org.freeplane.backgroundVideo.nativeMac.windowFrontTest";
    private static final String NATIVE_WINDOW_OPACITY_PROPERTY = "org.freeplane.backgroundVideo.nativeMac.opacity";

    private static final Pointer NS_AUTORELEASE_POOL = Foundation.cls("NSAutoreleasePool");
    private static final Pointer NS_STRING = Foundation.cls("NSString");
    private static final Pointer NS_URL = Foundation.cls("NSURL");
    private static final Pointer NS_ARRAY = Foundation.cls("NSArray");
    private static final Pointer NS_COLOR = Foundation.cls("NSColor");
    private static final Pointer NS_WINDOW = Foundation.cls("NSWindow");
    private static final Pointer AV_PLAYER_ITEM = Foundation.cls("AVPlayerItem");
    private static final Pointer AV_QUEUE_PLAYER = Foundation.cls("AVQueuePlayer");
    private static final Pointer AV_PLAYER_LOOPER = Foundation.cls("AVPlayerLooper");
    private static final Pointer AV_PLAYER_LAYER = Foundation.cls("AVPlayerLayer");

    private static final Pointer ALLOC = Foundation.sel("alloc");
    private static final Pointer INIT = Foundation.sel("init");
    private static final Pointer DRAIN = Foundation.sel("drain");
    private static final Pointer RETAIN = Foundation.sel("retain");
    private static final Pointer RELEASE = Foundation.sel("release");
    private static final Pointer SUPERVIEW = Foundation.sel("superview");
    private static final Pointer WINDOW = Foundation.sel("window");
    private static final Pointer CONTENT_VIEW = Foundation.sel("contentView");
    private static final Pointer LAYER = Foundation.sel("layer");
    private static final Pointer SET_WANTS_LAYER = Foundation.sel("setWantsLayer:");
    private static final Pointer SET_FRAME = Foundation.sel("setFrame:");
    private static final Pointer SET_FRAME_DISPLAY = Foundation.sel("setFrame:display:");
    private static final Pointer SET_HIDDEN = Foundation.sel("setHidden:");
    private static final Pointer SET_OPAQUE = Foundation.sel("setOpaque:");
    private static final Pointer SET_BACKGROUND_COLOR = Foundation.sel("setBackgroundColor:");
    private static final Pointer SET_IGNORES_MOUSE_EVENTS = Foundation.sel("setIgnoresMouseEvents:");
    private static final Pointer SET_HAS_SHADOW = Foundation.sel("setHasShadow:");
    private static final Pointer SET_RELEASED_WHEN_CLOSED = Foundation.sel("setReleasedWhenClosed:");
    private static final Pointer SET_MASKS_TO_BOUNDS = Foundation.sel("setMasksToBounds:");
    private static final Pointer SET_LEVEL = Foundation.sel("setLevel:");
    private static final Pointer SET_ALPHA_VALUE = Foundation.sel("setAlphaValue:");
    private static final Pointer SET_VIDEO_GRAVITY = Foundation.sel("setVideoGravity:");
    private static final Pointer INIT_WITH_CONTENT_RECT = Foundation.sel("initWithContentRect:styleMask:backing:defer:");
    private static final Pointer INSERT_SUBLAYER_BELOW = Foundation.sel("insertSublayer:below:");
    private static final Pointer ADD_SUBLAYER = Foundation.sel("addSublayer:");
    private static final Pointer ADD_CHILD_WINDOW_ORDERED = Foundation.sel("addChildWindow:ordered:");
    private static final Pointer REMOVE_CHILD_WINDOW = Foundation.sel("removeChildWindow:");
    private static final Pointer ORDER_FRONT = Foundation.sel("orderFront:");
    private static final Pointer ORDER_FRONT_REGARDLESS = Foundation.sel("orderFrontRegardless");
    private static final Pointer ORDER_BACK = Foundation.sel("orderBack:");
    private static final Pointer ORDER_OUT = Foundation.sel("orderOut:");
    private static final Pointer CLOSE = Foundation.sel("close");
    private static final Pointer REMOVE_FROM_SUPERLAYER = Foundation.sel("removeFromSuperlayer");
    private static final Pointer PLAY = Foundation.sel("play");
    private static final Pointer PAUSE = Foundation.sel("pause");
    private static final Pointer SET_MUTED = Foundation.sel("setMuted:");
    private static final Pointer SET_VOLUME = Foundation.sel("setVolume:");
    private static final Pointer STRING_WITH_UTF8 = Foundation.sel("stringWithUTF8String:");
    private static final Pointer FILE_URL_WITH_PATH = Foundation.sel("fileURLWithPath:");
    private static final Pointer URL_WITH_STRING = Foundation.sel("URLWithString:");
    private static final Pointer ARRAY_WITH_OBJECT = Foundation.sel("arrayWithObject:");
    private static final Pointer CLEAR_COLOR = Foundation.sel("clearColor");
    private static final Pointer CG_COLOR = Foundation.sel("CGColor");
    private static final Pointer PLAYER_ITEM_WITH_URL = Foundation.sel("playerItemWithURL:");
    private static final Pointer QUEUE_PLAYER_WITH_ITEMS = Foundation.sel("queuePlayerWithItems:");
    private static final Pointer PLAYER_LOOPER_WITH_PLAYER_TEMPLATE_ITEM = Foundation.sel("playerLooperWithPlayer:templateItem:");
    private static final Pointer PLAYER_LAYER_WITH_PLAYER = Foundation.sel("playerLayerWithPlayer:");

    private static final int NS_BORDERLESS_WINDOW_MASK = 0;
    private static final int NS_BACKING_STORE_BUFFERED = 2;
    private static final int NS_WINDOW_BELOW = -1;
    private static final int NS_WINDOW_ABOVE = 1;
    private static final int NS_NORMAL_WINDOW_LEVEL = 0;
    private static final int NS_FLOATING_WINDOW_LEVEL = 3;

    private final JComponent host;
    private final URI uri;
    private final Runnable readyHandler;
    private final Runnable errorHandler;
    private final boolean nativeWindowMode;
    private final boolean nativeWindowFront;
    private final boolean nativeWindowFrontTest;

    private volatile boolean ready;
    private volatile boolean disposed;
    private volatile boolean playRequested;
    private Pointer player;
    private Pointer item;
    private Pointer looper;
    private Pointer layer;
    private Pointer parentLayer;
    private Pointer videoWindow;
    private Pointer parentWindow;
    private Pointer videoContentLayer;
    private Object windowPeer;
    private Object platformWindow;
    private Object contentView;
    private ComponentListener componentListener;
    private ComponentListener windowComponentListener;
    private HierarchyBoundsListener hierarchyBoundsListener;
    private HierarchyListener hierarchyListener;
    private Window observedWindow;
    private boolean boundsSyncQueued;
    private int installAttempts;

    public MacNativeMapBackgroundVideo(final JComponent host, final URI uri,
                                       final Runnable readyHandler, final Runnable errorHandler) {
        this.host = host;
        this.uri = uri;
        this.readyHandler = readyHandler;
        this.errorHandler = errorHandler;
        nativeWindowMode = Boolean.parseBoolean(System.getProperty(NATIVE_WINDOW_ENABLED_PROPERTY, "true"));
        nativeWindowFrontTest = Boolean.getBoolean(NATIVE_WINDOW_FRONT_TEST_PROPERTY);
        nativeWindowFront = nativeWindowFrontTest
                || Boolean.parseBoolean(System.getProperty(NATIVE_WINDOW_FRONT_PROPERTY, "true"));
        setOpaque(false);
        installBoundsListeners();
        SwingUtilities.invokeLater(this::install);
    }

    public boolean isReady() {
        return ready;
    }

    public void play() {
        playRequested = true;
        runOnAppKitLaterIfAvailable(() -> {
            if(! disposed && player != null)
                Foundation.msg(player, PLAY);
        });
    }

    public void pause() {
        playRequested = false;
        runOnAppKitLaterIfAvailable(() -> {
            if(! disposed && player != null)
                Foundation.msg(player, PAUSE);
        });
    }

    public void dispose() {
        disposed = true;
        playRequested = false;
        removeBoundsListeners();
        runOnAppKitLaterIfAvailable(this::disposeOnAppKit);
    }

    public boolean requiresHostWindowTransparency() {
        return ! isPaintedInFront();
    }

    public boolean isPaintedInFront() {
        return nativeWindowMode && nativeWindowFront;
    }

    public void updateForegroundOpacity() {
        runOnAppKitLaterIfAvailable(() -> {
            if(! disposed && videoWindow != null)
                Foundation.msg(videoWindow, SET_ALPHA_VALUE, nativeWindowOpacity());
        });
    }

    private void install() {
        if(disposed || player != null)
            return;
        final CGRect frame = nativeWindowMode ? currentScreenFrame() : currentFrame();
        if(nativeWindowMode && frame == null) {
            if(installAttempts++ < 40)
                installLater();
            else
                SwingUtilities.invokeLater(errorHandler);
            return;
        }
        installAttempts = 0;
        try {
            runOnAppKit(() -> {
                final Pointer pool = newPool();
                try {
                    final AwtView awtView = awtView();
                    if(awtView == null)
                        throw new IllegalStateException("no AWT NSView");
                    Foundation.msg(awtView.pointer, SET_WANTS_LAYER, (byte) 1);
                    final Pointer awtLayer = Foundation.msg(awtView.pointer, LAYER);
                    if(awtLayer == null)
                        throw new IllegalStateException("no AWT backing layer");
                    windowPeer = awtView.peer;
                    platformWindow = awtView.platformWindow;
                    contentView = awtView.contentView;
                    makeAwtLayerTransparent(awtView, awtLayer);
                    final Pointer url = nsUrl();
                    item = retain(Foundation.msg(AV_PLAYER_ITEM, PLAYER_ITEM_WITH_URL, url));
                    final Pointer items = Foundation.msg(NS_ARRAY, ARRAY_WITH_OBJECT, item);
                    player = retain(Foundation.msg(AV_QUEUE_PLAYER, QUEUE_PLAYER_WITH_ITEMS, items));
                    Foundation.msg(player, SET_MUTED, (byte) 1);
                    Foundation.msg(player, SET_VOLUME, 0f);
                    looper = retain(Foundation.msg(AV_PLAYER_LOOPER, PLAYER_LOOPER_WITH_PLAYER_TEMPLATE_ITEM, player, item));
                    layer = retain(Foundation.msg(AV_PLAYER_LAYER, PLAYER_LAYER_WITH_PLAYER, player));
                    Foundation.msg(layer, SET_VIDEO_GRAVITY, nsString("AVLayerVideoGravityResizeAspectFill"));
                    if(nativeWindowMode)
                        installWindowLayer(awtView, frame);
                    else
                        installAwtSiblingLayer(awtView, awtLayer, frame);
                    ready = true;
                    LogUtils.info("macOS native background video ready: " + uri);
                    SwingUtilities.invokeLater(readyHandler);
                    if(playRequested)
                        Foundation.msg(player, PLAY);
                }
                catch (final Throwable e) {
                    LogUtils.warn("macOS native background video failed: " + uri, e);
                    disposeOnAppKit();
                    SwingUtilities.invokeLater(errorHandler);
                }
                finally {
                    Foundation.msg(pool, DRAIN);
                }
            });
        }
        catch (final RuntimeException e) {
            LogUtils.warn("macOS native background video failed: " + uri, e);
            SwingUtilities.invokeLater(errorHandler);
        }
    }

    private void installLater() {
        final Timer timer = new Timer(50, e -> install());
        timer.setRepeats(false);
        timer.start();
    }

    private void installAwtSiblingLayer(final AwtView awtView, final Pointer awtLayer, final CGRect frame) {
        final Pointer superview = Foundation.msg(awtView.pointer, SUPERVIEW);
        if(superview == null)
            throw new IllegalStateException("no AWT superview");
        Foundation.msg(superview, SET_WANTS_LAYER, (byte) 1);
        parentLayer = Foundation.msg(superview, LAYER);
        if(parentLayer == null)
            throw new IllegalStateException("no AWT superview backing layer");
        makeLayerTransparent(parentLayer);
        Foundation.msg(parentLayer, SET_MASKS_TO_BOUNDS, (byte) 1);
        setFrameOnAppKit(frame);
        Foundation.msg(parentLayer, INSERT_SUBLAYER_BELOW, layer, awtLayer);
    }

    private void installWindowLayer(final AwtView awtView, final CGRect frame) {
        if(frame == null)
            throw new IllegalStateException("no video frame");
        parentWindow = Foundation.msg(awtView.pointer, WINDOW);
        if(parentWindow == null)
            throw new IllegalStateException("no parent NSWindow");
        videoWindow = retain(Foundation.msg(Foundation.msg(NS_WINDOW, ALLOC), INIT_WITH_CONTENT_RECT,
                frame, NS_BORDERLESS_WINDOW_MASK, NS_BACKING_STORE_BUFFERED, (byte) 0));
        if(videoWindow == null)
            throw new IllegalStateException("no video NSWindow");
        Foundation.msg(videoWindow, SET_RELEASED_WHEN_CLOSED, (byte) 0);
        Foundation.msg(videoWindow, SET_OPAQUE, (byte) 0);
        Foundation.msg(videoWindow, SET_BACKGROUND_COLOR, Foundation.msg(NS_COLOR, CLEAR_COLOR));
        Foundation.msg(videoWindow, SET_ALPHA_VALUE, nativeWindowOpacity());
        Foundation.msg(videoWindow, SET_IGNORES_MOUSE_EVENTS, (byte) 1);
        Foundation.msg(videoWindow, SET_HAS_SHADOW, (byte) 0);
        Foundation.msg(videoWindow, SET_LEVEL, nativeWindowFrontTest ? NS_FLOATING_WINDOW_LEVEL : NS_NORMAL_WINDOW_LEVEL);
        final Pointer contentView = Foundation.msg(videoWindow, CONTENT_VIEW);
        Foundation.msg(contentView, SET_WANTS_LAYER, (byte) 1);
        videoContentLayer = Foundation.msg(contentView, LAYER);
        if(videoContentLayer == null)
            throw new IllegalStateException("no video content layer");
        Foundation.msg(videoContentLayer, SET_MASKS_TO_BOUNDS, (byte) 1);
        Foundation.msg(videoContentLayer, ADD_SUBLAYER, layer);
        setFrameOnAppKit(frame);
        if(nativeWindowFront) {
            Foundation.msg(parentWindow, ADD_CHILD_WINDOW_ORDERED, videoWindow, NS_WINDOW_ABOVE);
            Foundation.msg(videoWindow, ORDER_FRONT, (Pointer) null);
            if(nativeWindowFrontTest)
                Foundation.msg(videoWindow, ORDER_FRONT_REGARDLESS);
        }
        else {
            Foundation.msg(parentWindow, ADD_CHILD_WINDOW_ORDERED, videoWindow, NS_WINDOW_BELOW);
            Foundation.msg(videoWindow, ORDER_BACK, (Pointer) null);
        }
    }

    private double nativeWindowOpacity() {
        final String systemOpacity = System.getProperty(NATIVE_WINDOW_OPACITY_PROPERTY);
        return systemOpacity != null ? MapBackgroundVideo.foregroundOpacity(systemOpacity)
                : MapBackgroundVideo.foregroundOpacity();
    }

    private AwtView awtView() throws Exception {
        final Window window = SwingUtilities.getWindowAncestor(host);
        if(window == null || ! window.isDisplayable())
            return null;
        final Class<?> awtAccessor = Class.forName("sun.awt.AWTAccessor");
        final Object componentAccessor = invoke(awtAccessor, null, "getComponentAccessor");
        final Class<?> componentAccessorInterface = Class.forName("sun.awt.AWTAccessor$ComponentAccessor");
        final Object peer = invoke(componentAccessorInterface, componentAccessor, "getPeer", java.awt.Component.class, window);
        if(peer == null)
            return null;
        final Object platformWindow = invoke(peer.getClass(), peer, "getPlatformWindow");
        final Object contentView = invoke(platformWindow.getClass(), platformWindow, "getContentView");
        final long awtView = (Long) invoke(contentView.getClass(), contentView, "getAWTView");
        return awtView == 0 ? null : new AwtView(new Pointer(awtView), peer, platformWindow, contentView);
    }

    private void makeAwtLayerTransparent(final AwtView awtView, final Pointer awtLayer) throws Exception {
        invoke(awtView.peer.getClass(), awtView.peer, "setOpaque", boolean.class, false);
        invoke(awtView.peer.getClass(), awtView.peer, "setBackground", java.awt.Color.class,
                new java.awt.Color(0, 0, 0, 0));
        invoke(awtView.platformWindow.getClass(), awtView.platformWindow, "setOpaque", boolean.class, false);
        invoke(awtView.contentView.getClass(), awtView.contentView, "setWindowLayerOpaque", boolean.class, false);
        makeLayerTransparent(awtLayer);
    }

    private void makeLayerTransparent(final Pointer layer) {
        Foundation.msg(layer, SET_OPAQUE, (byte) 0);
        Foundation.msg(layer, SET_BACKGROUND_COLOR, Foundation.msg(Foundation.msg(NS_COLOR, CLEAR_COLOR), CG_COLOR));
    }

    private Object invoke(final Class<?> type, final Object target, final String name) throws Exception {
        final Method method = type.getMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private Object invoke(final Class<?> type, final Object target, final String name,
                          final Class<?> argumentType, final Object argument) throws Exception {
        final Method method = type.getMethod(name, argumentType);
        method.setAccessible(true);
        return method.invoke(target, argument);
    }

    private static class AwtView {
        private final Pointer pointer;
        private final Object peer;
        private final Object platformWindow;
        private final Object contentView;

        AwtView(final Pointer pointer, final Object peer, final Object platformWindow, final Object contentView) {
            this.pointer = pointer;
            this.peer = peer;
            this.platformWindow = platformWindow;
            this.contentView = contentView;
        }
    }

    private Pointer nsUrl() {
        if("file".equalsIgnoreCase(uri.getScheme()))
            return Foundation.msg(NS_URL, FILE_URL_WITH_PATH, nsString(uri.getPath()));
        return Foundation.msg(NS_URL, URL_WITH_STRING, nsString(uri.toASCIIString()));
    }

    private Pointer nsString(final String value) {
        return Foundation.msg(NS_STRING, STRING_WITH_UTF8, value);
    }

    private Pointer retain(final Pointer pointer) {
        if(pointer != null)
            Foundation.msg(pointer, RETAIN);
        return pointer;
    }

    private Pointer newPool() {
        return Foundation.msg(Foundation.msg(NS_AUTORELEASE_POOL, ALLOC), INIT);
    }

    private void syncBounds() {
        if(boundsSyncQueued || disposed)
            return;
        boundsSyncQueued = true;
        SwingUtilities.invokeLater(() -> {
            boundsSyncQueued = false;
            updateObservedWindow();
            final CGRect frame = nativeWindowMode ? currentScreenFrame() : currentFrame();
            runOnAppKitLaterIfAvailable(() -> setFrameOnAppKit(frame));
        });
    }

    private CGRect currentFrame() {
        if(host.getWidth() <= 0 || host.getHeight() <= 0)
            return null;
        final Window window = SwingUtilities.getWindowAncestor(host);
        if(window == null)
            return null;
        final Rectangle bounds = SwingUtilities.convertRectangle(host, new Rectangle(0, 0, host.getWidth(), host.getHeight()), window);
        final double parentHeight = window.getHeight();
        return new CGRect(bounds.x, parentHeight - bounds.y - bounds.height, bounds.width, bounds.height);
    }

    private CGRect currentScreenFrame() {
        if(host.getWidth() <= 0 || host.getHeight() <= 0)
            return null;
        final Window window = SwingUtilities.getWindowAncestor(host);
        if(window == null || ! window.isShowing())
            return null;
        try {
            final Rectangle bounds = SwingUtilities.convertRectangle(host,
                    new Rectangle(0, 0, host.getWidth(), host.getHeight()), window);
            final java.awt.Point windowLocation = window.getLocationOnScreen();
            final int screenHeight = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
            return new CGRect(windowLocation.x + bounds.x, screenHeight - windowLocation.y - bounds.y - bounds.height,
                    bounds.width, bounds.height);
        }
        catch (final RuntimeException e) {
            return null;
        }
    }

    private void setFrameOnAppKit(final CGRect frame) {
        if(layer == null || frame == null)
            return;
        if(videoWindow != null) {
            Foundation.msg(videoWindow, SET_FRAME_DISPLAY, frame, (byte) 1);
            Foundation.msg(layer, SET_FRAME, new CGRect(0, 0, frame.size.width, frame.size.height));
        }
        else if(parentLayer != null)
            Foundation.msg(layer, SET_FRAME, frame);
    }

    private void disposeOnAppKit() {
        if(player != null)
            Foundation.msg(player, PAUSE);
        if(parentWindow != null && videoWindow != null)
            Foundation.msg(parentWindow, REMOVE_CHILD_WINDOW, videoWindow);
        if(videoWindow != null) {
            Foundation.msg(videoWindow, ORDER_OUT, (Pointer) null);
            Foundation.msg(videoWindow, CLOSE);
        }
        if(layer != null) {
            Foundation.msg(layer, SET_HIDDEN, (byte) 1);
            Foundation.msg(layer, REMOVE_FROM_SUPERLAYER);
        }
        release(videoWindow);
        release(layer);
        release(looper);
        release(player);
        release(item);
        videoWindow = null;
        parentWindow = null;
        videoContentLayer = null;
        layer = null;
        looper = null;
        player = null;
        item = null;
        parentLayer = null;
        ready = false;
        restoreAwtOpacity();
    }

    private void restoreAwtOpacity() {
        try {
            if(contentView != null)
                invoke(contentView.getClass(), contentView, "setWindowLayerOpaque", boolean.class, true);
            if(platformWindow != null)
                invoke(platformWindow.getClass(), platformWindow, "setOpaque", boolean.class, true);
            if(windowPeer != null) {
                final Window window = SwingUtilities.getWindowAncestor(host);
                if(window != null)
                    invoke(windowPeer.getClass(), windowPeer, "setBackground", java.awt.Color.class, window.getBackground());
                invoke(windowPeer.getClass(), windowPeer, "setOpaque", boolean.class, true);
            }
        }
        catch (final Exception e) {
            LogUtils.warn(e);
        }
        finally {
            windowPeer = null;
            contentView = null;
            platformWindow = null;
        }
    }

    private void release(final Pointer pointer) {
        if(pointer != null)
            Foundation.msg(pointer, RELEASE);
    }

    private void installBoundsListeners() {
        componentListener = new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                syncBounds();
            }

            @Override
            public void componentMoved(final ComponentEvent e) {
                syncBounds();
            }
        };
        host.addComponentListener(componentListener);
        hierarchyBoundsListener = new HierarchyBoundsAdapter() {
            @Override
            public void ancestorMoved(final HierarchyEvent e) {
                syncBounds();
            }

            @Override
            public void ancestorResized(final HierarchyEvent e) {
                syncBounds();
            }
        };
        host.addHierarchyBoundsListener(hierarchyBoundsListener);
        addComponentListener(componentListener);
        hierarchyListener = new HierarchyListener() {
            @Override
            public void hierarchyChanged(final HierarchyEvent e) {
                if((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    updateObservedWindow();
                    syncBounds();
                }
            }
        };
        addHierarchyListener(hierarchyListener);
        windowComponentListener = new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                syncBounds();
            }

            @Override
            public void componentMoved(final ComponentEvent e) {
                syncBounds();
            }
        };
        updateObservedWindow();
    }

    private void updateObservedWindow() {
        if(windowComponentListener == null)
            return;
        final Window window = SwingUtilities.getWindowAncestor(host);
        if(observedWindow == window)
            return;
        if(observedWindow != null)
            observedWindow.removeComponentListener(windowComponentListener);
        observedWindow = window;
        if(observedWindow != null)
            observedWindow.addComponentListener(windowComponentListener);
    }

    private void removeBoundsListeners() {
        if(componentListener != null) {
            host.removeComponentListener(componentListener);
            removeComponentListener(componentListener);
            componentListener = null;
        }
        if(hierarchyBoundsListener != null) {
            host.removeHierarchyBoundsListener(hierarchyBoundsListener);
            hierarchyBoundsListener = null;
        }
        if(hierarchyListener != null) {
            removeHierarchyListener(hierarchyListener);
            hierarchyListener = null;
        }
        if(observedWindow != null && windowComponentListener != null) {
            observedWindow.removeComponentListener(windowComponentListener);
            observedWindow = null;
        }
        windowComponentListener = null;
    }

    private void runOnAppKit(final Runnable runnable) {
        try {
            final Class<?> toolkit = Class.forName("sun.lwawt.macosx.LWCToolkit");
            toolkit.getMethod("performOnMainThreadAndWait", Runnable.class).invoke(null, runnable);
        }
        catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            if(cause instanceof Error)
                throw (Error) cause;
            throw new IllegalStateException(cause);
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void runOnAppKitIfAvailable(final Runnable runnable) {
        try {
            runOnAppKit(runnable);
        }
        catch (final RuntimeException e) {
            LogUtils.warn(e);
        }
    }

    private void runOnAppKitLaterIfAvailable(final Runnable runnable) {
        try {
            final Class<?> toolkit = Class.forName("sun.lwawt.macosx.LWCToolkit");
            final Method method = toolkit.getDeclaredMethod("performOnMainThreadAfterDelay", Runnable.class, long.class);
            method.setAccessible(true);
            method.invoke(null, runnable, 0L);
        }
        catch (final Exception e) {
            LogUtils.warn(e);
        }
    }

    private static void loadFramework(final String name) {
        NativeLibrary.getInstance("/System/Library/Frameworks/" + name + ".framework/" + name);
    }

    public static class CGPoint extends Structure {
        public static class ByValue extends CGPoint implements Structure.ByValue {
            public ByValue() {
            }

            ByValue(final double x, final double y) {
                super(x, y);
            }
        }

        public double x;
        public double y;

        public CGPoint() {
        }

        CGPoint(final double x, final double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("x", "y");
        }
    }

    public static class CGSize extends Structure {
        public static class ByValue extends CGSize implements Structure.ByValue {
            public ByValue() {
            }

            ByValue(final double width, final double height) {
                super(width, height);
            }
        }

        public double width;
        public double height;

        public CGSize() {
        }

        CGSize(final double width, final double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("width", "height");
        }
    }

    public static class CGRect extends Structure implements Structure.ByValue {
        public CGPoint.ByValue origin;
        public CGSize.ByValue size;

        public CGRect() {
            origin = new CGPoint.ByValue();
            size = new CGSize.ByValue();
        }

        CGRect(final double x, final double y, final double width, final double height) {
            origin = new CGPoint.ByValue(x, y);
            size = new CGSize.ByValue(width, height);
        }

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("origin", "size");
        }
    }

    public interface ObjC extends Library {
        Pointer objc_getClass(String name);
        Pointer sel_registerName(String name);
        Pointer objc_msgSend(Pointer receiver, Pointer selector);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, Pointer argument);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, Pointer first, Pointer second);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, Pointer argument, int index);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, String argument);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, byte argument);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, int argument);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, float argument);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, double argument);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, CGRect frame);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, CGRect frame, byte display);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, CGRect frame, long styleMask, long backing, byte defer);
    }

    private static class Foundation {
        private static final ObjC OBJC = Native.load("objc", ObjC.class);

        static Pointer cls(final String name) {
            return OBJC.objc_getClass(name);
        }

        static Pointer sel(final String name) {
            return OBJC.sel_registerName(name);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector) {
            return OBJC.objc_msgSend(receiver, selector);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final Pointer argument) {
            return OBJC.objc_msgSend(receiver, selector, argument);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final Pointer first, final Pointer second) {
            return OBJC.objc_msgSend(receiver, selector, first, second);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final Pointer argument, final int index) {
            return OBJC.objc_msgSend(receiver, selector, argument, index);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final String argument) {
            return OBJC.objc_msgSend(receiver, selector, argument);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final byte argument) {
            return OBJC.objc_msgSend(receiver, selector, argument);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final int argument) {
            return OBJC.objc_msgSend(receiver, selector, argument);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final float argument) {
            return OBJC.objc_msgSend(receiver, selector, argument);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final double argument) {
            return OBJC.objc_msgSend(receiver, selector, argument);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final CGRect frame) {
            return OBJC.objc_msgSend(receiver, selector, frame);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final CGRect frame, final byte display) {
            return OBJC.objc_msgSend(receiver, selector, frame, display);
        }

        static Pointer msg(final Pointer receiver, final Pointer selector, final CGRect frame,
                           final long styleMask, final long backing, final byte defer) {
            return OBJC.objc_msgSend(receiver, selector, frame, styleMask, backing, defer);
        }
    }
}

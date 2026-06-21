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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.net.URI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.freeplane.core.util.LogUtils;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;

class JavaFxMapBackgroundVideo extends JPanel implements MapBackgroundVideoPlayer {
    private static final long serialVersionUID = 1L;
    private static final String JAVAFX_ANIMATION_FRAMERATE_PROPERTY = "javafx.animation.framerate";
    private static final String JAVAFX_ANIMATION_PULSE_PROPERTY = "javafx.animation.pulse";
    private static final String DEFAULT_JAVAFX_FRAMERATE = "30";

    private final URI uri;
    private final JFXPanel fxPanel;
    private final Runnable readyHandler;
    private final Runnable errorHandler;

    private volatile boolean disposed;
    private volatile boolean playRequested;
    private volatile boolean ready;
    private volatile boolean errorReported;

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ImageView overlayView;
    private Rectangle clip;
    private double mediaWidth = 1d;
    private double mediaHeight = 1d;

    JavaFxMapBackgroundVideo(final URI uri, final Runnable readyHandler, final Runnable errorHandler) {
        super(new BorderLayout(0, 0));
        this.uri = uri;
        this.readyHandler = readyHandler;
        this.errorHandler = errorHandler;
        setDefaultJavaFxFramerate();
        setOpaque(false);
        fxPanel = new JFXPanel();
        fxPanel.setOpaque(true);
        fxPanel.setBackground(Color.BLACK);
        fxPanel.setFocusable(false);
        add(fxPanel, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                updateMediaViewSize();
            }
        });
        createPlayer();
    }

    private static void setDefaultJavaFxFramerate() {
        if(System.getProperty(JAVAFX_ANIMATION_FRAMERATE_PROPERTY) == null
                && System.getProperty(JAVAFX_ANIMATION_PULSE_PROPERTY) == null)
            System.setProperty(JAVAFX_ANIMATION_FRAMERATE_PROPERTY, DEFAULT_JAVAFX_FRAMERATE);
    }

    @Override
    public JComponent component() {
        return this;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void play() {
        playRequested = true;
        Platform.runLater(() -> {
            if(! disposed && mediaPlayer != null)
                mediaPlayer.play();
        });
    }

    @Override
    public void pause() {
        playRequested = false;
        Platform.runLater(() -> {
            if(! disposed && mediaPlayer != null)
                mediaPlayer.pause();
        });
    }

    @Override
    public void dispose() {
        disposed = true;
        playRequested = false;
        Platform.runLater(() -> {
            if(mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
                mediaView = null;
                overlayView = null;
                clip = null;
            }
            fxPanel.setScene(null);
        });
    }

    @Override
    public void setOverlay(final BufferedImage overlay) {
        Platform.runLater(() -> {
            if(disposed || overlayView == null)
                return;
            overlayView.setImage(overlay != null ? SwingFXUtils.toFXImage(overlay, null) : null);
        });
    }

    private void createPlayer() {
        Platform.runLater(() -> {
            if(disposed)
                return;
            try {
                Platform.setImplicitExit(false);
                final Media media = new Media(uri.toASCIIString());
                final MediaPlayer player = new MediaPlayer(media);
                final MediaView view = new MediaView(player);
                final ImageView overlay = new ImageView();
                final Group root = new Group(view, overlay);
                final Rectangle rootClip = new Rectangle();
                view.setPreserveRatio(false);
                view.setSmooth(false);
                overlay.setPreserveRatio(false);
                overlay.setSmooth(false);
                root.setClip(rootClip);
                player.setMute(true);
                player.setVolume(0d);
                player.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer = player;
                mediaView = view;
                overlayView = overlay;
                clip = rootClip;
                media.setOnError(() -> reportError(media.getError()));
                player.setOnError(() -> reportError(player.getError()));
                player.setOnReady(() -> {
                    if(disposed)
                        return;
                    mediaWidth = Math.max(1d, media.getWidth());
                    mediaHeight = Math.max(1d, media.getHeight());
                    updateMediaViewSize();
                    ready = true;
                    LogUtils.info("JavaFX JFXPanel background video ready: " + uri);
                    SwingUtilities.invokeLater(readyHandler);
                    if(playRequested)
                        player.play();
                });
                fxPanel.setScene(new Scene(root, javafx.scene.paint.Color.BLACK));
            }
            catch (final RuntimeException e) {
                reportError(e);
            }
        });
    }

    private void updateMediaViewSize() {
        final Dimension size = getSize();
        final double width = Math.max(1, size.getWidth());
        final double height = Math.max(1, size.getHeight());
        Platform.runLater(() -> {
            if(clip != null) {
                clip.setWidth(width);
                clip.setHeight(height);
            }
            if(mediaView != null)
                updateMediaViewSize(width, height);
            if(overlayView != null) {
                overlayView.setFitWidth(width);
                overlayView.setFitHeight(height);
            }
        });
    }

    private void updateMediaViewSize(final double width, final double height) {
        final double widthScale = width / mediaWidth;
        final double heightScale = height / mediaHeight;
        final double scale = Math.max(widthScale, heightScale);
        final double scaledWidth = mediaWidth * scale;
        final double scaledHeight = mediaHeight * scale;
        mediaView.setFitWidth(scaledWidth);
        mediaView.setFitHeight(scaledHeight);
        mediaView.setTranslateX((width - scaledWidth) / 2d);
        mediaView.setTranslateY((height - scaledHeight) / 2d);
    }

    private void reportError(final Throwable error) {
        if(disposed)
            return;
        if(errorReported)
            return;
        errorReported = true;
        LogUtils.warn("background video failed: " + uri, error);
        SwingUtilities.invokeLater(errorHandler);
    }
}

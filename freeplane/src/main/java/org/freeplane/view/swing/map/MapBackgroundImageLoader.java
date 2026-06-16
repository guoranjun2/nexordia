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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.view.swing.features.filepreview.IViewerFactory;

class MapBackgroundImageLoader {
    private static final ExecutorService BACKGROUND_IMAGE_LOADER = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "Freeplane background image loader");
        thread.setDaemon(true);
        return thread;
    });

    private final BiConsumer<IViewerFactory, URI> backgroundImageSetter;
    private final Runnable backgroundImageClearer;
    private final Runnable backgroundUpdater;
    private final Runnable repainter;
    private final AtomicLong loadingSequence = new AtomicLong();

    MapBackgroundImageLoader(final BiConsumer<IViewerFactory, URI> backgroundImageSetter,
                             final Runnable backgroundImageClearer, final Runnable backgroundUpdater,
                             final Runnable repainter) {
        this.backgroundImageSetter = backgroundImageSetter;
        this.backgroundImageClearer = backgroundImageClearer;
        this.backgroundUpdater = backgroundUpdater;
        this.repainter = repainter;
    }

    void loadBackgroundImage(final IViewerFactory factory, final URI uri, final boolean enabled) {
        final long loading = loadingSequence.incrementAndGet();
        if(! enabled || uri == null || factory == null) {
            clearBackgroundImage();
            return;
        }
        loadBackgroundImage(factory, uri, loading);
        repainter.run();
    }

    private void clearBackgroundImage() {
        backgroundImageClearer.run();
        backgroundUpdater.run();
        repainter.run();
    }

    private void loadBackgroundImage(final IViewerFactory factory, final URI uri, final long loading) {
        if(! isRemoteUri(uri)) {
            backgroundImageSetter.accept(factory, uri);
            return;
        }
        final File cacheFile = backgroundImageCacheFile(uri);
        if(cacheFile != null && cacheFile.isFile()) {
            backgroundImageSetter.accept(factory, cacheFile.toURI());
            return;
        }
        BACKGROUND_IMAGE_LOADER.execute(() -> loadRemoteBackgroundImage(factory, uri, loading));
    }

    private void loadRemoteBackgroundImage(final IViewerFactory factory, final URI uri, final long loading) {
        final URI backgroundImageUri = cacheBackgroundImage(uri);
        if(backgroundImageUri == null)
            return;
        SwingUtilities.invokeLater(() -> {
            if(loadingSequence.get() == loading)
                backgroundImageSetter.accept(factory, backgroundImageUri);
        });
    }

    private URI cacheBackgroundImage(final URI uri) {
        if(! isRemoteUri(uri))
            return uri;
        final File cacheFile = backgroundImageCacheFile(uri);
        if(cacheFile == null)
            return null;
        if(cacheFile.isFile())
            return cacheFile.toURI();
        try {
            cacheFile.getParentFile().mkdirs();
            final File tempFile = File.createTempFile(cacheFile.getName(), ".download", cacheFile.getParentFile());
            try {
                final URLConnection connection = uri.toURL().openConnection();
                configureBackgroundImageConnection(connection, uri);
                try(InputStream inputStream = connection.getInputStream()) {
                    Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(tempFile.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                if(tempFile.isFile())
                    tempFile.delete();
            }
            return cacheFile.toURI();
        }
        catch (final IOException e) {
            LogUtils.warn(e);
            return null;
        }
    }

    private void configureBackgroundImageConnection(final URLConnection connection, final URI uri) {
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36 Freeplane/1.13.3");
        connection.setRequestProperty("Accept", "image/png,image/jpeg,image/gif,image/*;q=0.8,*/*;q=0.5");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        final String referer = backgroundImageReferer(uri);
        if(referer != null)
            connection.setRequestProperty("Referer", referer);
    }

    private String backgroundImageReferer(final URI uri) {
        final String host = uri.getHost();
        if(host == null)
            return null;
        if(host.endsWith("pexels.com"))
            return "https://www.pexels.com/";
        return null;
    }

    private boolean isRemoteUri(final URI uri) {
        final String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private File backgroundImageCacheFile(final URI uri) {
        final String userDirectory = ResourceController.getResourceController().getFreeplaneUserDirectory();
        if(userDirectory == null)
            return null;
        return new File(new File(new File(userDirectory, "resources"), "background"),
                sha256(uri.toString()) + backgroundImageExtension(uri));
    }

    private String backgroundImageExtension(final URI uri) {
        final String path = uri.getPath();
        if(path == null)
            return ".image";
        final int suffixStart = path.lastIndexOf('.');
        if(suffixStart == -1 || suffixStart == path.length() - 1)
            return ".image";
        return path.substring(suffixStart).toLowerCase(Locale.ROOT);
    }

    private String sha256(final String value) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            final byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            final StringBuilder builder = new StringBuilder();
            for (byte b : digest)
                builder.append(String.format("%02x", b & 0xff));
            return builder.toString();
        }
        catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

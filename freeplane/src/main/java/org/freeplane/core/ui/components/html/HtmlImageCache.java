package org.freeplane.core.ui.components.html;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.SwingUtilities;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;

class HtmlImageCache {
	static class ImageLoader {
		private static final int CONNECTION_TIMEOUT_MILLISECONDS = 10_000;
		private static final int READ_TIMEOUT_MILLISECONDS = 10_000;
		private static final String USER_AGENT = "Freeplane";

		Dimension readSize(URL source) throws IOException {
			if(isSvg(source))
				return readSvgSize(source);
			try (InputStream inputStream = openStream(source);
					ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
				final ImageReader reader = imageReader(imageInputStream);
				try {
					reader.setInput(imageInputStream, true, true);
					return new Dimension(reader.getWidth(0), reader.getHeight(0));
				}
				finally {
					reader.dispose();
				}
			}
		}

		BufferedImage load(URL source, int targetWidth, int targetHeight) throws IOException {
			if(isSvg(source))
				return loadSvg(source, targetWidth, targetHeight);
			try (InputStream inputStream = openStream(source);
					ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
				final ImageReader reader = imageReader(imageInputStream);
				try {
					reader.setInput(imageInputStream, true, true);
					final int sourceWidth = reader.getWidth(0);
					final int sourceHeight = reader.getHeight(0);
					final Dimension targetSize = fitWithinSourceSize(sourceWidth, sourceHeight, targetWidth, targetHeight);
					final ImageReadParam readParam = reader.getDefaultReadParam();
					final int subsampling = subsampling(sourceWidth, sourceHeight, targetSize.width, targetSize.height);
					if(subsampling > 1)
						readParam.setSourceSubsampling(subsampling, subsampling, 0, 0);
					final BufferedImage image = reader.read(0, readParam);
					return scale(image, targetSize.width, targetSize.height);
				}
				finally {
					reader.dispose();
				}
			}
		}

		private static Dimension readSvgSize(URL source) throws IOException {
			final SVGDiagram diagram = svgDiagram(source);
			return new Dimension(Math.max(1, (int) Math.ceil(diagram.getWidth())),
					Math.max(1, (int) Math.ceil(diagram.getHeight())));
		}

		private static BufferedImage loadSvg(URL source, int targetWidth, int targetHeight) throws IOException {
			final SVGDiagram diagram = svgDiagram(source);
			final BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D graphics = image.createGraphics();
			try {
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.scale(targetWidth / (double) diagram.getWidth(), targetHeight / (double) diagram.getHeight());
				diagram.render(graphics);
			}
			catch (SVGException e) {
				throw new IOException(e);
			}
			finally {
				graphics.dispose();
			}
			return image;
		}

		private static SVGDiagram svgDiagram(URL source) throws IOException {
			final SVGUniverse universe = new SVGUniverse();
			final URI svgUri = universe.loadSVG(openStream(source), source.toExternalForm());
			final SVGDiagram diagram = universe.getDiagram(svgUri);
			if(diagram == null || diagram.getWidth() <= 0 || diagram.getHeight() <= 0)
				throw new IOException("can not read svg image");
			return diagram;
		}

		private static InputStream openStream(URL source) throws IOException {
			final URLConnection connection = source.openConnection();
			connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
			connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
			if(connection instanceof HttpURLConnection)
				connection.setRequestProperty("User-Agent", USER_AGENT);
			return connection.getInputStream();
		}

		private static boolean isSvg(URL source) {
			final String sourceText = source.toExternalForm().toLowerCase(Locale.ROOT);
			if(sourceText.startsWith("data:image/svg+xml"))
				return true;
			int endIndex = sourceText.length();
			final int queryIndex = sourceText.indexOf('?');
			if(queryIndex >= 0)
				endIndex = queryIndex;
			final int fragmentIndex = sourceText.indexOf('#');
			if(fragmentIndex >= 0)
				endIndex = Math.min(endIndex, fragmentIndex);
			return sourceText.substring(0, endIndex).endsWith(".svg");
		}

		private static ImageReader imageReader(ImageInputStream imageInputStream) throws IOException {
			if(imageInputStream == null)
				throw new IOException("can not create image input stream");
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
			if(! readers.hasNext())
				throw new IOException("can not create image reader");
			return readers.next();
		}

		private static int subsampling(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
			if(targetWidth <= 0 || targetHeight <= 0)
				return 1;
			final int widthSubsampling = Math.max(1, sourceWidth / targetWidth);
			final int heightSubsampling = Math.max(1, sourceHeight / targetHeight);
			return Math.max(1, Math.min(widthSubsampling, heightSubsampling));
		}

		static Dimension fitWithinSourceSize(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
			final double scale = Math.min(1d,
					Math.min(sourceWidth / (double) targetWidth, sourceHeight / (double) targetHeight));
			return new Dimension(Math.max(1, (int) Math.floor(targetWidth * scale)),
					Math.max(1, (int) Math.floor(targetHeight * scale)));
		}

		private static BufferedImage scale(BufferedImage image, int targetWidth, int targetHeight) {
			if(image.getWidth() == targetWidth && image.getHeight() == targetHeight)
				return image;
			final BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D graphics = scaledImage.createGraphics();
			try {
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
			}
			finally {
				graphics.dispose();
				image.flush();
			}
			return scaledImage;
		}
	}

	private static class ImageKey {
		private final String sourceKey;
		private final int width;
		private final int height;

		ImageKey(String sourceKey, int width, int height) {
			this.sourceKey = sourceKey;
			this.width = width;
			this.height = height;
		}

		@Override
		public int hashCode() {
			int result = sourceKey.hashCode();
			result = 31 * result + width;
			result = 31 * result + height;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(! (obj instanceof ImageKey))
				return false;
			final ImageKey other = (ImageKey) obj;
			return width == other.width && height == other.height && sourceKey.equals(other.sourceKey);
		}
	}

	private static class Entry {
		private final BufferedImage image;
		private final int pixels;

		Entry(BufferedImage image) {
			this.image = image;
			this.pixels = image.getWidth() * image.getHeight();
		}
	}

	private static final int DEFAULT_MAX_CACHE_PIXELS = 64 * 1024 * 1024;
	private static final int DEFAULT_MAX_IMAGE_PIXELS = 4 * 1024 * 1024;
	private static final int DIMENSION_BUCKET = 32;
	private static final int IMAGE_LOADER_THREADS = 2;
	private static final int[] IMAGE_SIZE_LEVELS = {16, 32, 64, 128, 256, 512, 1024, 2048};
	private static final ExecutorService IMAGE_LOADER_EXECUTOR = Executors.newFixedThreadPool(IMAGE_LOADER_THREADS, runnable -> {
		final Thread thread = new Thread(runnable, "Freeplane HTML image loader");
		thread.setDaemon(true);
		return thread;
	});

	static final HtmlImageCache INSTANCE = new HtmlImageCache(new ImageLoader(), IMAGE_LOADER_EXECUTOR,
			DEFAULT_MAX_CACHE_PIXELS, DEFAULT_MAX_IMAGE_PIXELS);

	private final ImageLoader imageLoader;
	private final Executor executor;
	private final int maxCachePixels;
	private final int maxImagePixels;
	private final Map<ImageKey, Entry> images;
	private final Map<String, Dimension> imageSizes;
	private final Map<ImageKey, Set<Runnable>> pendingImageCallbacks;
	private final Map<String, Set<Runnable>> pendingSizeCallbacks;
	private final Map<String, ImageKey> latestPendingImageKeys;
	private final Set<ImageKey> failedImages;
	private final Set<String> failedImageSizes;
	private final Set<Runnable> pendingRepaintCallbacks;
	private int cachedPixels;
	private boolean repaintScheduled;

	HtmlImageCache(ImageLoader imageLoader, Executor executor, int maxCachePixels, int maxImagePixels) {
		this.imageLoader = imageLoader;
		this.executor = executor;
		this.maxCachePixels = maxCachePixels;
		this.maxImagePixels = maxImagePixels;
		this.images = new LinkedHashMap<ImageKey, Entry>(16, 0.75f, true);
		this.imageSizes = new HashMap<String, Dimension>();
		this.pendingImageCallbacks = new HashMap<ImageKey, Set<Runnable>>();
		this.pendingSizeCallbacks = new HashMap<String, Set<Runnable>>();
		this.latestPendingImageKeys = new HashMap<String, ImageKey>();
		this.failedImages = new HashSet<ImageKey>();
		this.failedImageSizes = new HashSet<String>();
		this.pendingRepaintCallbacks = new LinkedHashSet<Runnable>();
	}

	BufferedImage getOrSchedule(URL source, String sourceKey, int targetWidth, int targetHeight, Runnable repaintCallback) {
		final Dimension targetSize = targetSize(targetWidth, targetHeight);
		final ImageKey key = new ImageKey(sourceKey, targetSize.width, targetSize.height);
		final BufferedImage fallbackImage;
		synchronized(this) {
			final Entry entry = images.get(key);
			if(entry != null)
				return entry.image;
			fallbackImage = fallbackImage(key);
			if(failedImages.contains(key))
				return fallbackImage;
			if(pendingImageCallbacks.containsKey(key)) {
				latestPendingImageKeys.put(sourceKey, key);
				addRepaintCallback(key, repaintCallback);
				return fallbackImage;
			}
			latestPendingImageKeys.put(sourceKey, key);
			pendingImageCallbacks.put(key, repaintCallbacks(repaintCallback));
		}
		executor.execute(() -> loadImage(source, key));
		return fallbackImage;
	}

	private BufferedImage fallbackImage(ImageKey targetKey) {
		ImageKey bestOversizedKey = null;
		ImageKey bestUndersizedKey = null;
		int bestOversizedPixels = Integer.MAX_VALUE;
		int largestUndersizedPixels = -1;
		final int targetPixels = targetKey.width * targetKey.height;
		for(Map.Entry<ImageKey, Entry> imageEntry : images.entrySet()) {
			final ImageKey candidateKey = imageEntry.getKey();
			if(! candidateKey.sourceKey.equals(targetKey.sourceKey))
				continue;
			final int candidatePixels = imageEntry.getValue().pixels;
			if(candidatePixels >= targetPixels) {
				if(candidatePixels < bestOversizedPixels) {
					bestOversizedPixels = candidatePixels;
					bestOversizedKey = candidateKey;
				}
			}
			else if(candidatePixels > largestUndersizedPixels) {
				largestUndersizedPixels = candidatePixels;
				bestUndersizedKey = candidateKey;
			}
		}
		final ImageKey bestKey = bestOversizedKey != null ? bestOversizedKey : bestUndersizedKey;
		final Entry fallbackEntry = bestKey != null ? images.get(bestKey) : null;
		return fallbackEntry != null ? fallbackEntry.image : null;
	}

	Dimension getImageSize(URL source, String sourceKey) {
		synchronized(this) {
			final Dimension cachedSize = imageSizes.get(sourceKey);
			if(cachedSize != null)
				return new Dimension(cachedSize);
		}
		try {
			final Dimension imageSize = imageLoader.readSize(source);
			synchronized(this) {
				imageSizes.put(sourceKey, new Dimension(imageSize));
			}
			return imageSize;
		}
		catch (IOException e) {
			return null;
		}
	}

	Dimension getImageSizeOrSchedule(URL source, String sourceKey, Runnable repaintCallback) {
		synchronized(this) {
			final Dimension cachedSize = imageSizes.get(sourceKey);
			if(cachedSize != null)
				return new Dimension(cachedSize);
			if(failedImageSizes.contains(sourceKey))
				return null;
			if(pendingSizeCallbacks.containsKey(sourceKey)) {
				addSizeRepaintCallback(sourceKey, repaintCallback);
				return null;
			}
			pendingSizeCallbacks.put(sourceKey, repaintCallbacks(repaintCallback));
		}
		executor.execute(() -> readImageSize(source, sourceKey));
		return null;
	}

	private void readImageSize(URL source, String sourceKey) {
		final Set<Runnable> repaintCallbacks;
		try {
			final Dimension imageSize = imageLoader.readSize(source);
			synchronized(this) {
				imageSizes.put(sourceKey, new Dimension(imageSize));
				repaintCallbacks = pendingSizeCallbacks.remove(sourceKey);
			}
			repaint(repaintCallbacks);
		}
		catch (IOException | RuntimeException e) {
			synchronized(this) {
				pendingSizeCallbacks.remove(sourceKey);
				failedImageSizes.add(sourceKey);
			}
		}
	}

	private Set<Runnable> repaintCallbacks(Runnable repaintCallback) {
		final Set<Runnable> repaintCallbacks = new LinkedHashSet<Runnable>();
		if(repaintCallback != null)
			repaintCallbacks.add(repaintCallback);
		return repaintCallbacks;
	}

	private void addRepaintCallback(ImageKey key, Runnable repaintCallback) {
		if(repaintCallback != null)
			pendingImageCallbacks.get(key).add(repaintCallback);
	}

	private void addSizeRepaintCallback(String sourceKey, Runnable repaintCallback) {
		if(repaintCallback != null)
			pendingSizeCallbacks.get(sourceKey).add(repaintCallback);
	}

	synchronized void removeRepaintCallback(Runnable repaintCallback) {
		if(repaintCallback == null)
			return;
		removeRepaintCallback(pendingImageCallbacks, repaintCallback);
		removeRepaintCallback(pendingSizeCallbacks, repaintCallback);
		pendingRepaintCallbacks.remove(repaintCallback);
	}

	private <K> void removeRepaintCallback(Map<K, Set<Runnable>> callbacks, Runnable repaintCallback) {
		for(Set<Runnable> repaintCallbacks : callbacks.values())
			repaintCallbacks.remove(repaintCallback);
	}

	private void loadImage(URL source, ImageKey key) {
		final Set<Runnable> repaintCallbacks;
		synchronized(this) {
			if(! isLatestPendingImageKey(key)) {
				pendingImageCallbacks.remove(key);
				return;
			}
		}
		try {
			final BufferedImage image = imageLoader.load(source, key.width, key.height);
			synchronized(this) {
				if(! isLatestPendingImageKey(key)) {
					image.flush();
					pendingImageCallbacks.remove(key);
					return;
				}
				put(key, image);
				repaintCallbacks = pendingImageCallbacks.remove(key);
				latestPendingImageKeys.remove(key.sourceKey);
			}
			repaint(repaintCallbacks);
		}
		catch (IOException | RuntimeException e) {
			synchronized(this) {
				pendingImageCallbacks.remove(key);
				if(isLatestPendingImageKey(key))
					latestPendingImageKeys.remove(key.sourceKey);
				failedImages.add(key);
			}
		}
	}

	private boolean isLatestPendingImageKey(ImageKey key) {
		return key.equals(latestPendingImageKeys.get(key.sourceKey));
	}

	private void repaint(Set<Runnable> repaintCallbacks) {
		if(repaintCallbacks == null || repaintCallbacks.isEmpty())
			return;
		synchronized(this) {
			pendingRepaintCallbacks.addAll(repaintCallbacks);
			if(repaintScheduled)
				return;
			repaintScheduled = true;
		}
		SwingUtilities.invokeLater(this::repaintPendingCallbacks);
	}

	private void repaintPendingCallbacks() {
		final Set<Runnable> repaintCallbacks;
		synchronized(this) {
			repaintCallbacks = new LinkedHashSet<Runnable>(pendingRepaintCallbacks);
			pendingRepaintCallbacks.clear();
			repaintScheduled = false;
		}
		for(Runnable repaintCallback : repaintCallbacks)
			repaintCallback.run();
	}

	private void put(ImageKey key, BufferedImage image) {
		final Entry oldEntry = images.remove(key);
		if(oldEntry != null) {
			cachedPixels -= oldEntry.pixels;
			oldEntry.image.flush();
		}
		final Entry entry = new Entry(image);
		images.put(key, entry);
		cachedPixels += entry.pixels;
		trim();
	}

	private void trim() {
		final Iterator<Map.Entry<ImageKey, Entry>> iterator = images.entrySet().iterator();
		while(cachedPixels > maxCachePixels && iterator.hasNext()) {
			final Entry entry = iterator.next().getValue();
			cachedPixels -= entry.pixels;
			entry.image.flush();
			iterator.remove();
		}
	}

	private Dimension targetSize(int width, int height) {
		final Dimension boundedSize = fitWithinMaxPixels(Math.max(1, width), Math.max(1, height), maxImagePixels);
		final Dimension leveledSize = imageSizeLevel(boundedSize);
		return fitWithinMaxPixels(leveledSize.width, leveledSize.height, maxImagePixels);
	}

	static Dimension fitWithinMaxPixels(int width, int height, int maxPixels) {
		final long pixels = (long) width * (long) height;
		if(pixels <= maxPixels)
			return new Dimension(width, height);
		final double scale = Math.sqrt(maxPixels / (double) pixels);
		int scaledWidth = Math.max(1, (int) Math.floor(width * scale));
		int scaledHeight = Math.max(1, (int) Math.floor(height * scale));
		if((long) scaledWidth * (long) scaledHeight > maxPixels) {
			if(scaledWidth >= scaledHeight)
				scaledWidth = Math.max(1, maxPixels / scaledHeight);
			else
				scaledHeight = Math.max(1, maxPixels / scaledWidth);
		}
		return new Dimension(scaledWidth, scaledHeight);
	}

	static Dimension imageSizeLevel(Dimension size) {
		final int maxLength = Math.max(size.width, size.height);
		final int level = imageSizeLevel(maxLength);
		final double scale = level / (double) maxLength;
		return new Dimension(Math.max(1, (int)Math.ceil(size.width * scale)),
				Math.max(1, (int)Math.ceil(size.height * scale)));
	}

	private static int imageSizeLevel(int length) {
		for(int level : IMAGE_SIZE_LEVELS) {
			if(length <= level)
				return level;
		}
		return ((length + DIMENSION_BUCKET - 1) / DIMENSION_BUCKET) * DIMENSION_BUCKET;
	}

	static String sourceKey(URL source) {
		return sourceKey(source.toExternalForm() + sourceVersion(source));
	}

	private static String sourceVersion(URL source) {
		if(! "file".equals(source.getProtocol()))
			return "";
		try {
			final URLConnection connection = source.openConnection();
			final long lastModified = connection.getLastModified();
			return lastModified > 0 ? "#" + lastModified : "";
		}
		catch (IOException e) {
			return "";
		}
	}

	private static String sourceKey(String source) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hash = digest.digest(source.getBytes("UTF-8"));
			final StringBuilder builder = new StringBuilder(hash.length * 2);
			for(byte b : hash) {
				final int value = b & 0xff;
				if(value < 16)
					builder.append('0');
				builder.append(Integer.toHexString(value));
			}
			return builder.toString();
		}
		catch (NoSuchAlgorithmException | IOException e) {
			return source;
		}
	}

	synchronized void clear() {
		for(Entry entry : images.values())
			entry.image.flush();
		images.clear();
		imageSizes.clear();
		pendingImageCallbacks.clear();
		pendingSizeCallbacks.clear();
		latestPendingImageKeys.clear();
		failedImages.clear();
		failedImageSizes.clear();
		pendingRepaintCallbacks.clear();
		repaintScheduled = false;
		cachedPixels = 0;
	}
}

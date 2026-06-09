package org.freeplane.core.ui.components.html;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

class HtmlImageCache {
	static class ImageLoader {
		Dimension readSize(URL source) throws IOException {
			try (InputStream inputStream = source.openStream();
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
			try (InputStream inputStream = source.openStream();
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

		ImageIcon loadAnimated(URL source) throws IOException {
			try (InputStream inputStream = source.openStream()) {
				return loadAnimated(bytes(inputStream));
			}
		}

		ImageIcon loadAnimated(byte[] imageData) throws IOException {
			return loadedIcon(new ImageIcon(imageData));
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

		private static ImageIcon loadedIcon(ImageIcon icon) throws IOException {
			if(icon.getImageLoadStatus() != MediaTracker.COMPLETE)
				throw new IOException("can not load animated image");
			return icon;
		}

		private static byte[] bytes(InputStream inputStream) throws IOException {
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			final byte[] buffer = new byte[8192];
			int length;
			while((length = inputStream.read(buffer)) >= 0)
				outputStream.write(buffer, 0, length);
			return outputStream.toByteArray();
		}
	}

	private interface AnimatedImageLoader {
		ImageIcon load() throws IOException;
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

	private static final int DEFAULT_MAX_CACHE_PIXELS = 32 * 1024 * 1024;
	private static final int DEFAULT_MAX_IMAGE_PIXELS = 4 * 1024 * 1024;
	private static final int DEFAULT_MAX_ANIMATED_IMAGES = 4;
	private static final int DIMENSION_BUCKET = 32;
	private static final int IMAGE_LOADER_THREADS = 4;
	private static final ExecutorService IMAGE_LOADER_EXECUTOR = Executors.newFixedThreadPool(IMAGE_LOADER_THREADS, runnable -> {
		final Thread thread = new Thread(runnable, "Freeplane HTML image loader");
		thread.setDaemon(true);
		return thread;
	});

	static final HtmlImageCache INSTANCE = new HtmlImageCache(new ImageLoader(), IMAGE_LOADER_EXECUTOR,
			DEFAULT_MAX_CACHE_PIXELS, DEFAULT_MAX_IMAGE_PIXELS, DEFAULT_MAX_ANIMATED_IMAGES);

	private final ImageLoader imageLoader;
	private final Executor executor;
	private final int maxCachePixels;
	private final int maxImagePixels;
	private final int maxAnimatedImages;
	private final Map<ImageKey, Entry> images;
	private final Map<String, ImageIcon> animatedImages;
	private final Map<String, Dimension> imageSizes;
	private final Map<ImageKey, Set<Runnable>> pendingImageCallbacks;
	private final Map<String, Set<Runnable>> pendingAnimatedImageCallbacks;
	private final Set<ImageKey> failedImages;
	private final Set<String> failedAnimatedImages;
	private int cachedPixels;

	HtmlImageCache(ImageLoader imageLoader, Executor executor, int maxCachePixels, int maxImagePixels) {
		this(imageLoader, executor, maxCachePixels, maxImagePixels, DEFAULT_MAX_ANIMATED_IMAGES);
	}

	HtmlImageCache(ImageLoader imageLoader, Executor executor, int maxCachePixels, int maxImagePixels,
			int maxAnimatedImages) {
		this.imageLoader = imageLoader;
		this.executor = executor;
		this.maxCachePixels = maxCachePixels;
		this.maxImagePixels = maxImagePixels;
		this.maxAnimatedImages = maxAnimatedImages;
		this.images = new LinkedHashMap<ImageKey, Entry>(16, 0.75f, true);
		this.animatedImages = new LinkedHashMap<String, ImageIcon>(16, 0.75f, true);
		this.imageSizes = new HashMap<String, Dimension>();
		this.pendingImageCallbacks = new HashMap<ImageKey, Set<Runnable>>();
		this.pendingAnimatedImageCallbacks = new HashMap<String, Set<Runnable>>();
		this.failedImages = new HashSet<ImageKey>();
		this.failedAnimatedImages = new HashSet<String>();
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
				addRepaintCallback(key, repaintCallback);
				return fallbackImage;
			}
			pendingImageCallbacks.put(key, repaintCallbacks(repaintCallback));
		}
		executor.execute(() -> loadImage(source, key));
		return fallbackImage;
	}

	ImageIcon getOrScheduleAnimated(URL source, String sourceKey, Runnable repaintCallback) {
		return getOrScheduleAnimated(sourceKey, () -> imageLoader.loadAnimated(source), repaintCallback);
	}

	ImageIcon getOrScheduleAnimated(byte[] imageData, String sourceKey, Runnable repaintCallback) {
		return getOrScheduleAnimated(sourceKey, () -> imageLoader.loadAnimated(imageData), repaintCallback);
	}

	private ImageIcon getOrScheduleAnimated(String sourceKey, AnimatedImageLoader animatedImageLoader,
			Runnable repaintCallback) {
		synchronized(this) {
			final ImageIcon image = animatedImages.get(sourceKey);
			if(image != null)
				return image;
			if(failedAnimatedImages.contains(sourceKey))
				return null;
			if(pendingAnimatedImageCallbacks.containsKey(sourceKey)) {
				addAnimatedRepaintCallback(sourceKey, repaintCallback);
				return null;
			}
			pendingAnimatedImageCallbacks.put(sourceKey, repaintCallbacks(repaintCallback));
		}
		executor.execute(() -> loadAnimatedImage(sourceKey, animatedImageLoader));
		return null;
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

	private void addAnimatedRepaintCallback(String sourceKey, Runnable repaintCallback) {
		if(repaintCallback != null)
			pendingAnimatedImageCallbacks.get(sourceKey).add(repaintCallback);
	}

	private void loadImage(URL source, ImageKey key) {
		final Set<Runnable> repaintCallbacks;
		try {
			final BufferedImage image = imageLoader.load(source, key.width, key.height);
			synchronized(this) {
				put(key, image);
				repaintCallbacks = pendingImageCallbacks.remove(key);
			}
			repaint(repaintCallbacks);
		}
		catch (IOException | RuntimeException e) {
			synchronized(this) {
				pendingImageCallbacks.remove(key);
				failedImages.add(key);
			}
		}
	}

	private void loadAnimatedImage(String sourceKey, AnimatedImageLoader animatedImageLoader) {
		final Set<Runnable> repaintCallbacks;
		try {
			final ImageIcon image = animatedImageLoader.load();
			synchronized(this) {
				putAnimated(sourceKey, image);
				repaintCallbacks = pendingAnimatedImageCallbacks.remove(sourceKey);
			}
			repaint(repaintCallbacks);
		}
		catch (IOException | RuntimeException e) {
			synchronized(this) {
				pendingAnimatedImageCallbacks.remove(sourceKey);
				failedAnimatedImages.add(sourceKey);
			}
		}
	}

	private void repaint(Set<Runnable> repaintCallbacks) {
		if(repaintCallbacks == null || repaintCallbacks.isEmpty())
			return;
		SwingUtilities.invokeLater(() -> {
			for(Runnable repaintCallback : repaintCallbacks)
				repaintCallback.run();
		});
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

	private void putAnimated(String sourceKey, ImageIcon image) {
		final ImageIcon oldImage = animatedImages.remove(sourceKey);
		if(oldImage != null)
			oldImage.getImage().flush();
		animatedImages.put(sourceKey, image);
		trimAnimated();
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

	private void trimAnimated() {
		final Iterator<Map.Entry<String, ImageIcon>> iterator = animatedImages.entrySet().iterator();
		while(animatedImages.size() > maxAnimatedImages && iterator.hasNext()) {
			iterator.next().getValue().getImage().flush();
			iterator.remove();
		}
	}

	private Dimension targetSize(int width, int height) {
		final Dimension boundedSize = fitWithinMaxPixels(Math.max(1, width), Math.max(1, height), maxImagePixels);
		final int bucketedWidth = bucketLength(boundedSize.width);
		final int bucketedHeight = bucketLength(boundedSize.height);
		return fitWithinMaxPixels(bucketedWidth, bucketedHeight, maxImagePixels);
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

	private static int bucketLength(int value) {
		if(value <= DIMENSION_BUCKET)
			return value;
		return ((value + DIMENSION_BUCKET - 1) / DIMENSION_BUCKET) * DIMENSION_BUCKET;
	}

	static String sourceKey(URL source) {
		return sourceKey(source.toExternalForm());
	}

	static String sourceKey(String source) {
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
		for(ImageIcon image : animatedImages.values())
			image.getImage().flush();
		images.clear();
		animatedImages.clear();
		imageSizes.clear();
		pendingImageCallbacks.clear();
		pendingAnimatedImageCallbacks.clear();
		failedImages.clear();
		failedAnimatedImages.clear();
		cachedPixels = 0;
	}
}

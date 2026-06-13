package org.freeplane.core.ui.components.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

public class HtmlImageCacheTest {
	@Test
	public void cachesImagesForSameSourceAndTargetBucket() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 1_000_000, 1_000_000);
		final URL source = source("image-a");
		final String sourceKey = HtmlImageCache.sourceKey(source);

		assertThat(cache.getOrSchedule(source, sourceKey, 100, 50, null)).isNull();
		assertThat(cache.getOrSchedule(source, sourceKey, 100, 50, null)).isNotNull();
		assertThat(loader.loadCount).isEqualTo(1);
	}

	@Test
	public void evictsLeastRecentlyUsedImagesWhenPixelBudgetIsExceeded() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 150, 1_000_000);
		final URL firstSource = source("image-a");
		final URL secondSource = source("image-b");
		final String firstKey = HtmlImageCache.sourceKey(firstSource);
		final String secondKey = HtmlImageCache.sourceKey(secondSource);

		cache.getOrSchedule(firstSource, firstKey, 10, 10, null);
		cache.getOrSchedule(secondSource, secondKey, 10, 10, null);
		cache.getOrSchedule(firstSource, firstKey, 10, 10, null);

		assertThat(loader.loadCount).isEqualTo(3);
	}

	@Test
	public void keepsDifferentTargetBucketsSeparate() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 1_000_000, 1_000_000);
		final URL source = source("image-a");
		final String sourceKey = HtmlImageCache.sourceKey(source);

		cache.getOrSchedule(source, sourceKey, 100, 100, null);
		cache.getOrSchedule(source, sourceKey, 200, 200, null);

		assertThat(loader.loadCount).isEqualTo(2);
	}

	@Test
	public void returnsPreviousSameSourceImageWhileLoadingNewTargetBucket() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 1_000_000, 1_000_000);
		final URL source = source("image-a");
		final String sourceKey = HtmlImageCache.sourceKey(source);

		cache.getOrSchedule(source, sourceKey, 100, 100, null);
		final BufferedImage fallbackImage = cache.getOrSchedule(source, sourceKey, 200, 200, null);
		final BufferedImage targetImage = cache.getOrSchedule(source, sourceKey, 200, 200, null);

		assertThat(fallbackImage).isNotNull();
		assertThat(fallbackImage.getWidth()).isEqualTo(128);
		assertThat(targetImage.getWidth()).isEqualTo(256);
		assertThat(loader.loadCount).isEqualTo(2);
	}

	@Test
	public void usesClosestPreviousSameSourceImageAsFallback() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 1_000_000, 1_000_000);
		final URL source = source("image-a");
		final String sourceKey = HtmlImageCache.sourceKey(source);

		cache.getOrSchedule(source, sourceKey, 80, 80, null);
		cache.getOrSchedule(source, sourceKey, 120, 120, null);
		final BufferedImage fallbackImage = cache.getOrSchedule(source, sourceKey, 200, 200, null);

		assertThat(fallbackImage).isNotNull();
		assertThat(fallbackImage.getWidth()).isEqualTo(128);
	}

	@Test
	public void repaintsAllCallersWaitingForSamePendingImage() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final QueuedExecutor executor = new QueuedExecutor();
		final HtmlImageCache cache = new HtmlImageCache(loader, executor, 1_000_000, 1_000_000);
		final URL source = source("image-a");
		final String sourceKey = HtmlImageCache.sourceKey(source);
		final AtomicInteger firstRepaints = new AtomicInteger();
		final AtomicInteger secondRepaints = new AtomicInteger();

		assertThat(cache.getOrSchedule(source, sourceKey, 100, 100, () -> firstRepaints.incrementAndGet())).isNull();
		assertThat(cache.getOrSchedule(source, sourceKey, 100, 100, () -> secondRepaints.incrementAndGet())).isNull();

		assertThat(executor.taskCount()).isEqualTo(1);
		executor.runNext();
		SwingUtilities.invokeAndWait(() -> {
		});

		assertThat(firstRepaints.get()).isEqualTo(1);
		assertThat(secondRepaints.get()).isEqualTo(1);
		assertThat(loader.loadCount).isEqualTo(1);
	}

	@Test
	public void skipsQueuedImageLoadsReplacedByNewerTargetSize() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final QueuedExecutor executor = new QueuedExecutor();
		final HtmlImageCache cache = new HtmlImageCache(loader, executor, 1_000_000, 1_000_000);
		final URL source = source("image-a");
		final String sourceKey = HtmlImageCache.sourceKey(source);
		final AtomicInteger oldTargetRepaints = new AtomicInteger();
		final AtomicInteger newTargetRepaints = new AtomicInteger();

		cache.getOrSchedule(source, sourceKey, 100, 100, () -> oldTargetRepaints.incrementAndGet());
		cache.getOrSchedule(source, sourceKey, 200, 200, () -> newTargetRepaints.incrementAndGet());

		assertThat(executor.taskCount()).isEqualTo(2);
		executor.runNext();
		assertThat(loader.loadCount).isEqualTo(0);
		executor.runNext();
		SwingUtilities.invokeAndWait(() -> {
		});

		assertThat(loader.loadCount).isEqualTo(1);
		assertThat(oldTargetRepaints.get()).isEqualTo(0);
		assertThat(newTargetRepaints.get()).isEqualTo(1);
	}

	@Test
	public void groupsNearbyTargetSizesIntoImageSizeLevels() {
		assertThat(HtmlImageCache.imageSizeLevel(new Dimension(12, 6))).isEqualTo(new Dimension(16, 8));
		assertThat(HtmlImageCache.imageSizeLevel(new Dimension(24, 12))).isEqualTo(new Dimension(32, 16));
		assertThat(HtmlImageCache.imageSizeLevel(new Dimension(100, 50))).isEqualTo(new Dimension(128, 64));
		assertThat(HtmlImageCache.imageSizeLevel(new Dimension(120, 60))).isEqualTo(new Dimension(128, 64));
		assertThat(HtmlImageCache.imageSizeLevel(new Dimension(200, 100))).isEqualTo(new Dimension(256, 128));
	}

	@Test
	public void includesFileModificationTimeInSourceKey() throws Exception {
		final File imageFile = File.createTempFile("freeplane-html-image-cache", ".png");
		try {
			assertThat(imageFile.setLastModified(1_000L)).isTrue();
			final String firstKey = HtmlImageCache.sourceKey(imageFile.toURI().toURL());

			assertThat(imageFile.setLastModified(2_000L)).isTrue();
			final String secondKey = HtmlImageCache.sourceKey(imageFile.toURI().toURL());

			assertThat(secondKey).isNotEqualTo(firstKey);
		}
		finally {
			imageFile.delete();
		}
	}

	@Test
	public void cachesImageSizes() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 1_000_000, 1_000_000);
		final URL source = source("image-a");
		final String sourceKey = HtmlImageCache.sourceKey(source);

		assertThat(cache.getImageSize(source, sourceKey)).isEqualTo(new Dimension(640, 480));
		assertThat(cache.getImageSize(source, sourceKey)).isEqualTo(new Dimension(640, 480));
		assertThat(loader.sizeReadCount).isEqualTo(1);
	}

	@Test
	public void readsImageSizesAsynchronously() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final QueuedExecutor executor = new QueuedExecutor();
		final HtmlImageCache cache = new HtmlImageCache(loader, executor, 1_000_000, 1_000_000);
		final URL source = source("image-a");
		final String sourceKey = HtmlImageCache.sourceKey(source);
		final AtomicInteger repaints = new AtomicInteger();

		assertThat(cache.getImageSizeOrSchedule(source, sourceKey, () -> repaints.incrementAndGet())).isNull();
		assertThat(cache.getImageSizeOrSchedule(source, sourceKey, () -> repaints.incrementAndGet())).isNull();

		assertThat(executor.taskCount()).isEqualTo(1);
		executor.runNext();
		SwingUtilities.invokeAndWait(() -> {
		});

		assertThat(cache.getImageSizeOrSchedule(source, sourceKey, null)).isEqualTo(new Dimension(640, 480));
		assertThat(loader.sizeReadCount).isEqualTo(1);
		assertThat(repaints.get()).isEqualTo(2);
	}

	@Test
	public void loadsSvgImagesAtTargetSize() throws Exception {
		final File svgFile = File.createTempFile("freeplane-html-image-cache", ".svg");
		try {
			writeSvg(svgFile, "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"10\"><rect width=\"20\" height=\"10\" fill=\"red\"/></svg>");
			final HtmlImageCache.ImageLoader loader = new HtmlImageCache.ImageLoader();

			assertThat(loader.readSize(svgFile.toURI().toURL())).isEqualTo(new Dimension(20, 10));
			final BufferedImage image = loader.load(svgFile.toURI().toURL(), 40, 20);

			assertThat(image.getWidth()).isEqualTo(40);
			assertThat(image.getHeight()).isEqualTo(20);
		}
		finally {
			svgFile.delete();
		}
	}

	@Test
	public void sendsUserAgentWhenReadingHttpImages() throws Exception {
		final BufferedImage sourceImage = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
		final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/image.png", exchange -> {
			if(exchange.getRequestHeaders().containsKey("User-Agent")) {
				exchange.getResponseHeaders().set("Content-Type", "image/png");
				exchange.sendResponseHeaders(200, 0);
				ImageIO.write(sourceImage, "png", exchange.getResponseBody());
			}
			else
				exchange.sendResponseHeaders(403, -1);
			exchange.close();
		});
		server.start();
		try {
			final URL source = new URL("http://localhost:" + server.getAddress().getPort() + "/image.png");
			final HtmlImageCache.ImageLoader loader = new HtmlImageCache.ImageLoader();

			assertThat(loader.readSize(source)).isEqualTo(new Dimension(2, 1));
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	public void limitsScaledImageSizeToSourcePixels() {
		final Dimension targetSize = HtmlImageCache.ImageLoader.fitWithinSourceSize(640, 480, 8320, 6240);

		assertThat(targetSize).isEqualTo(new Dimension(640, 480));
	}

	@Test
	public void keepsExtremeAspectRatioWithinPixelBudget() {
		final Dimension targetSize = HtmlImageCache.fitWithinMaxPixels(100_000_000, 1, 4_000_000);

		assertThat((long) targetSize.width * targetSize.height).isLessThanOrEqualTo(4_000_000L);
	}

	private URL source(String name) throws Exception {
		return url(name + ".png");
	}

	private URL url(String fileName) throws Exception {
		return new URL("file:/" + fileName);
	}

	private void writeSvg(File file, String text) throws IOException {
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			writer.write(text);
		}
	}

	private static class CountingImageLoader extends HtmlImageCache.ImageLoader {
		private final Map<String, Dimension> imageSizes = new HashMap<String, Dimension>();
		private int loadCount;
		private int sizeReadCount;

		CountingImageLoader() {
			imageSizes.put("image-a.png", new Dimension(640, 480));
			imageSizes.put("image-b.png", new Dimension(320, 240));
		}

		@Override
		Dimension readSize(URL source) throws IOException {
			sizeReadCount++;
			return new Dimension(imageSizes.get(fileName(source)));
		}

		@Override
		BufferedImage load(URL source, int targetWidth, int targetHeight) throws IOException {
			loadCount++;
			return new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		}

		private String fileName(URL source) {
			final String externalForm = source.toExternalForm();
			return externalForm.substring(externalForm.lastIndexOf('/') + 1);
		}
	}

	private static class QueuedExecutor implements Executor {
		private final Queue<Runnable> tasks = new ArrayDeque<Runnable>();

		@Override
		public void execute(Runnable command) {
			tasks.add(command);
		}

		void runNext() {
			tasks.remove().run();
		}

		int taskCount() {
			return tasks.size();
		}
	}
}

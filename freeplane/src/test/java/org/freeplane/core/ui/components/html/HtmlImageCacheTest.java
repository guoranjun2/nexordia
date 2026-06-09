package org.freeplane.core.ui.components.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.junit.Test;

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
		assertThat(targetImage.getWidth()).isEqualTo(224);
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
	public void limitsScaledImageSizeToSourcePixels() {
		final Dimension targetSize = HtmlImageCache.ImageLoader.fitWithinSourceSize(640, 480, 8320, 6240);

		assertThat(targetSize).isEqualTo(new Dimension(640, 480));
	}

	@Test
	public void keepsExtremeAspectRatioWithinPixelBudget() {
		final Dimension targetSize = HtmlImageCache.fitWithinMaxPixels(100_000_000, 1, 4_000_000);

		assertThat((long) targetSize.width * targetSize.height).isLessThanOrEqualTo(4_000_000L);
	}

	@Test
	public void cachesAnimatedImagesAfterLoading() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 1_000_000, 1_000_000);
		final URL source = url("animated-a.gif");
		final String sourceKey = HtmlImageCache.sourceKey(source);

		assertThat(cache.getOrScheduleAnimated(source, sourceKey, null)).isNull();
		assertThat(cache.getOrScheduleAnimated(source, sourceKey, null)).isNotNull();
		assertThat(loader.animatedLoadCount).isEqualTo(1);
	}

	@Test
	public void repaintsAllCallersWaitingForSamePendingAnimatedImage() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final QueuedExecutor executor = new QueuedExecutor();
		final HtmlImageCache cache = new HtmlImageCache(loader, executor, 1_000_000, 1_000_000);
		final URL source = url("animated-a.gif");
		final String sourceKey = HtmlImageCache.sourceKey(source);
		final AtomicInteger firstRepaints = new AtomicInteger();
		final AtomicInteger secondRepaints = new AtomicInteger();

		assertThat(cache.getOrScheduleAnimated(source, sourceKey, () -> firstRepaints.incrementAndGet())).isNull();
		assertThat(cache.getOrScheduleAnimated(source, sourceKey, () -> secondRepaints.incrementAndGet())).isNull();

		assertThat(executor.taskCount()).isEqualTo(1);
		executor.runNext();
		SwingUtilities.invokeAndWait(() -> {
		});

		assertThat(firstRepaints.get()).isEqualTo(1);
		assertThat(secondRepaints.get()).isEqualTo(1);
		assertThat(loader.animatedLoadCount).isEqualTo(1);
	}

	@Test
	public void evictsLeastRecentlyUsedAnimatedImagesWhenLimitIsExceeded() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 1_000_000, 1_000_000, 2);
		final URL firstSource = url("animated-a.gif");
		final URL secondSource = url("animated-b.gif");
		final URL thirdSource = url("animated-c.gif");
		final String firstKey = HtmlImageCache.sourceKey(firstSource);
		final String secondKey = HtmlImageCache.sourceKey(secondSource);
		final String thirdKey = HtmlImageCache.sourceKey(thirdSource);

		cache.getOrScheduleAnimated(firstSource, firstKey, null);
		cache.getOrScheduleAnimated(secondSource, secondKey, null);
		cache.getOrScheduleAnimated(firstSource, firstKey, null);
		cache.getOrScheduleAnimated(thirdSource, thirdKey, null);
		cache.getOrScheduleAnimated(secondSource, secondKey, null);

		assertThat(loader.animatedLoadCount).isEqualTo(4);
	}

	@Test
	public void doesNotReloadFailedAnimatedImages() throws Exception {
		final CountingImageLoader loader = new CountingImageLoader();
		loader.failAnimatedImages = true;
		final HtmlImageCache cache = new HtmlImageCache(loader, Runnable::run, 1_000_000, 1_000_000);
		final URL source = url("animated-a.gif");
		final String sourceKey = HtmlImageCache.sourceKey(source);

		assertThat(cache.getOrScheduleAnimated(source, sourceKey, null)).isNull();
		assertThat(cache.getOrScheduleAnimated(source, sourceKey, null)).isNull();
		assertThat(loader.animatedLoadCount).isEqualTo(1);
	}

	@Test
	public void loadsAnimatedImagesFromUrlBytes() throws Exception {
		final File imageFile = File.createTempFile("freeplane-animated-image", ".gif");
		imageFile.deleteOnExit();
		try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
			outputStream.write(Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=="));
		}

		final ImageIcon image = new HtmlImageCache.ImageLoader().loadAnimated(imageFile.toURI().toURL());

		assertThat(image.getIconWidth()).isEqualTo(1);
		assertThat(image.getIconHeight()).isEqualTo(1);
	}

	private URL source(String name) throws Exception {
		return url(name + ".png");
	}

	private URL url(String fileName) throws Exception {
		return new URL("file:/" + fileName);
	}

	private static class CountingImageLoader extends HtmlImageCache.ImageLoader {
		private final Map<String, Dimension> imageSizes = new HashMap<String, Dimension>();
		private int loadCount;
		private int animatedLoadCount;
		private int sizeReadCount;
		private boolean failAnimatedImages;

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

		@Override
		ImageIcon loadAnimated(URL source) throws IOException {
			animatedLoadCount++;
			if(failAnimatedImages)
				throw new IOException("failed animated image");
			return new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
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

package org.freeplane.features.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ImageStorageShould {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void deduplicateClipboardImagesNextToMap() throws Exception {
		final File mapFile = temporaryFolder.newFile("sample.mm");
		final ImageStorage imageStorage = new ImageStorage(temporaryFolder.getRoot(), ImageStorageLocation.CURRENT);
		final BufferedImage image = image(Color.RED);

		final StoredImage first = imageStorage.storeClipboardImage(image, mapFile);
		final StoredImage second = imageStorage.storeClipboardImage(image, mapFile);

		assertThat(second.getSource()).isEqualTo(first.getSource());
		assertThat(first.getSource()).isEqualTo("sample_files/img/" + first.getFileName());
		assertThat(new File(temporaryFolder.getRoot(), first.getSource())).isFile();
		assertThat(new File(temporaryFolder.getRoot(), "sample_files/img").listFiles()).hasSize(1);
	}

	@Test
	public void storeImagesInUserDirectory() throws Exception {
		final ImageStorage imageStorage = new ImageStorage(temporaryFolder.getRoot(), ImageStorageLocation.GLOBAL);
		final File imageFile = temporaryFolder.newFile("source.png");
		ImageIO.write(image(Color.BLUE), "png", imageFile);

		final StoredImage storedImage = imageStorage.storeImageFile(imageFile, null);

		assertThat(storedImage.getSource()).startsWith(new File(temporaryFolder.getRoot(), "image").toURI().toString());
		assertThat(new File(temporaryFolder.getRoot(), "image/" + storedImage.getFileName())).isFile();
		assertThat(storedImage.getFileName()).endsWith(".png");
	}

	@Test
	public void embedImagesAsDataUri() throws Exception {
		final ImageStorage imageStorage = new ImageStorage(temporaryFolder.getRoot(), ImageStorageLocation.EMBEDDED);

		final StoredImage storedImage = imageStorage.storeClipboardImage(image(Color.GREEN), null);

		assertThat(storedImage.getSource()).startsWith("data:image/png;base64,");
		assertThat(new File(temporaryFolder.getRoot(), "image")).doesNotExist();
	}

	private BufferedImage image(final Color color) {
		final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, color.getRGB());
		image.setRGB(1, 0, color.getRGB());
		image.setRGB(0, 1, color.getRGB());
		image.setRGB(1, 1, color.getRGB());
		return image;
	}
}

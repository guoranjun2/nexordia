package org.freeplane.features.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ImageHtmlInserterShould {
	private final ImageHtmlInserter inserter = new ImageHtmlInserter();

	@Test
	public void createImageOnlyHtml() {
		final String html = inserter.createImageHtml("sample_files/img/img-a.png");

		assertThat(html).contains("<body><p><img src=\"sample_files/img/img-a.png\"></p></body>");
	}

	@Test
	public void createImageOnlyHtmlWithDimensions() {
		final String html = inserter.createImageHtml(new StoredImage("sample_files/img/img-a.png", "img-a.png", 320, 240));

		assertThat(html).contains("<img src=\"sample_files/img/img-a.png\" width=\"320\" height=\"240\" src-width=\"320\" src-height=\"240\">");
	}

	@Test
	public void insertImageBeforePlainText() {
		final String html = inserter.insertImageAtTop("hello", "image/img-a.png");

		assertThat(html).contains("<body><p><img src=\"image/img-a.png\"></p><p>hello</p></body>");
	}

	@Test
	public void insertImageBeforeExistingHtmlBody() {
		final String html = inserter.insertImageAtTop("<html><body><p>old</p></body></html>", "image/img-a.png");

		assertThat(html).contains("<body><p><img src=\"image/img-a.png\"></p><p>old</p></body>");
	}

	@Test
	public void resizeFirstImageKeepingSourceDimensions() {
		final String html = inserter.createImageHtml(new StoredImage("sample_files/img/img-a.png", "img-a.png", 320, 240));

		final String resizedHtml = inserter.resizeFirstImage(html, 160, 120);

		assertThat(resizedHtml).contains("width=\"160\" height=\"120\" src-width=\"320\" src-height=\"240\"");
	}

	@Test
	public void resizeImageByIndex() {
		final String html = "<html><body><p><img src=\"one.png\" width=\"320\" height=\"240\"></p><p><img src=\"two.png\" width=\"200\" height=\"100\"></p></body></html>";

		final String resizedHtml = inserter.resizeImage(html, 1, 80, 40);

		assertThat(resizedHtml).contains("<img src=\"one.png\" width=\"320\" height=\"240\">");
		assertThat(resizedHtml).contains("<img src=\"two.png\" width=\"80\" height=\"40\" src-width=\"200\" src-height=\"100\">");
	}

	@Test
	public void readImageSourceByIndex() {
		final String html = "<html><body><p><img src=\"one.png\"></p><p><img src=\"two.png\"></p></body></html>";

		assertThat(inserter.imageSource(html, 1)).isEqualTo("two.png");
	}

	@Test
	public void removeImageByIndex() {
		final String html = "<html><body><p><img src=\"one.png\"></p><p><img src=\"two.png\"></p><p>text</p></body></html>";

		final String updatedHtml = inserter.removeImage(html, 1);

		assertThat(updatedHtml).contains("<img src=\"one.png\">");
		assertThat(updatedHtml).doesNotContain("two.png");
		assertThat(updatedHtml).contains("<p>text</p>");
	}

	@Test
	public void removeLastImageAsEmptyContent() {
		final String html = inserter.createImageHtml("sample_files/img/img-a.png");

		assertThat(inserter.removeImage(html, 0)).isEqualTo("");
	}

	@Test
	public void keepOtherEmptyHtmlElementsWhenRemovingImage() {
		final String html = "<html><body><p><img src=\"one.png\"></p><table><tr><td></td></tr></table></body></html>";

		final String updatedHtml = inserter.removeImage(html, 0);

		assertThat(updatedHtml).contains("<table>");
	}

	@Test
	public void detectImageOnlyHtml() {
		final String html = inserter.createImageHtml(new StoredImage("sample_files/img/img-a.png", "img-a.png", 320, 240));

		assertThat(inserter.isImageOnly(html)).isTrue();
		assertThat(inserter.isImageOnly(inserter.insertImageAtTop("hello", "image/img-a.png"))).isFalse();
	}
}

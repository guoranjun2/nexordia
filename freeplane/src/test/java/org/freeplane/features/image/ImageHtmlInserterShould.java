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
	public void insertImageBeforePlainText() {
		final String html = inserter.insertImageAtTop("hello", "image/img-a.png");

		assertThat(html).contains("<body><p><img src=\"image/img-a.png\"></p><p>hello</p></body>");
	}

	@Test
	public void insertImageBeforeExistingHtmlBody() {
		final String html = inserter.insertImageAtTop("<html><body><p>old</p></body></html>", "image/img-a.png");

		assertThat(html).contains("<body><p><img src=\"image/img-a.png\"></p><p>old</p></body>");
	}
}

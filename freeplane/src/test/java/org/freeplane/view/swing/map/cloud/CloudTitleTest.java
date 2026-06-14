package org.freeplane.view.swing.map.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.junit.Test;

public class CloudTitleTest {
	@Test
	public void usesExistingTitleAttributeAsPlainText() {
		final CloudTitle title = CloudTitle.from(attributesWithTitle("abc"), "<html><body><img src='image.png'></body></html>");

		assertThat(title.getText()).isEqualTo("abc");
		assertThat(title.isImage()).isTrue();
		assertThat(title.getImageSource()).isEqualTo("image.png");
	}

	@Test
	public void usesExistingEmptyTitleAttributeWithoutFallback() {
		final CloudTitle title = CloudTitle.from(attributesWithTitle(""), "<html><body><img src='image.png'></body></html>");

		assertThat(title.getText()).isEmpty();
		assertThat(title.isImage()).isTrue();
		assertThat(title.getImageSource()).isEqualTo("image.png");
	}

	@Test
	public void usesFirstPlainTextLineWithoutTitleAttribute() {
		final CloudTitle title = CloudTitle.from(emptyAttributes(), "first\nsecond");

		assertThat(title.getText()).isEqualTo("first");
		assertThat(title.isImage()).isFalse();
	}

	@Test
	public void convertsHtmlWithoutImageToPlainText() {
		final CloudTitle title = CloudTitle.from(emptyAttributes(), "<html><body><b>first</b><br>second</body></html>");

		assertThat(title.getText()).isEqualTo("first");
		assertThat(title.isImage()).isFalse();
	}

	@Test
	public void usesImageSourceFromHtmlImage() {
		final CloudTitle title = CloudTitle.from(emptyAttributes(), "<html><body><IMG src='image.png'>caption</body></html>");

		assertThat(title.isImage()).isTrue();
		assertThat(title.getImageSource()).isEqualTo("image.png");
		assertThat(title.getText()).isEqualTo("caption");
	}

	@Test
	public void usesUnquotedImageSourceFromHtmlImage() {
		final CloudTitle title = CloudTitle.from(emptyAttributes(), "<html><body><img src=image.png></body></html>");

		assertThat(title.isImage()).isTrue();
		assertThat(title.getImageSource()).isEqualTo("image.png");
	}

	private NodeAttributeTableModel attributesWithTitle(String title) {
		final NodeAttributeTableModel attributes = emptyAttributes();
		attributes.getAttributes().add(new Attribute("title", title));
		return attributes;
	}

	private NodeAttributeTableModel emptyAttributes() {
		return new NodeAttributeTableModel();
	}
}

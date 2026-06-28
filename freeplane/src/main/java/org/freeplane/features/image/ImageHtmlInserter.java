package org.freeplane.features.image;

import org.freeplane.core.util.HtmlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ImageHtmlInserter {
	public static class ImageSize {
		private final int width;
		private final int height;

		public ImageSize(final int width, final int height) {
			this.width = width;
			this.height = height;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}
	}

	public String createImageHtml(final String source) {
		return createImageHtml(new StoredImage(source, source, -1, -1));
	}

	public String createImageHtml(final StoredImage image) {
		final Document document = createShell();
		appendImageParagraph(document.body(), image);
		return document.outerHtml();
	}

	public String insertImageAtTop(final String content, final String source) {
		return insertImageAtTop(content, new StoredImage(source, source, -1, -1));
	}

	public String insertImageAtTop(final String content, final StoredImage image) {
		final Document document;
		if (content == null || content.length() == 0) {
			document = createShell();
		}
		else if (HtmlUtils.isHtml(content)) {
			document = Jsoup.parse(content);
			document.outputSettings().prettyPrint(false);
		}
		else {
			document = Jsoup.parse(HtmlUtils.plainToHTML(content));
			document.outputSettings().prettyPrint(false);
		}
		final Element paragraph = imageParagraph(document, image);
		document.body().prependChild(paragraph);
		return document.outerHtml();
	}

	public String resizeFirstImage(final String content, final int width, final int height) {
		return resizeImage(content, 0, width, height);
	}

	public String resizeImage(final String content, final int imageIndex, final int width, final int height) {
		final Document document = parse(content);
		final Element image = imageAt(document, imageIndex);
		if (image == null) {
			return content;
		}
		final int boundedWidth = Math.max(1, width);
		final int boundedHeight = Math.max(1, height);
		if (!hasPositiveIntAttribute(image, "src-width")) {
			final int sourceWidth = positiveIntAttribute(image, "width", boundedWidth);
			image.attr("src-width", Integer.toString(sourceWidth));
		}
		if (!hasPositiveIntAttribute(image, "src-height")) {
			final int sourceHeight = positiveIntAttribute(image, "height", boundedHeight);
			image.attr("src-height", Integer.toString(sourceHeight));
		}
		image.attr("width", Integer.toString(boundedWidth));
		image.attr("height", Integer.toString(boundedHeight));
		return document.outerHtml();
	}

	public ImageSize firstImageSize(final String content) {
		return imageSize(content, 0);
	}

	public ImageSize imageSize(final String content, final int imageIndex) {
		final Document document = parse(content);
		final Element image = imageAt(document, imageIndex);
		if (image == null) {
			return null;
		}
		final int width = positiveIntAttribute(image, "width", positiveIntAttribute(image, "src-width", -1));
		final int height = positiveIntAttribute(image, "height", positiveIntAttribute(image, "src-height", -1));
		return width > 0 && height > 0 ? new ImageSize(width, height) : null;
	}

	public String imageSource(final String content, final int imageIndex) {
		final Document document = parse(content);
		final Element image = imageAt(document, imageIndex);
		return image != null ? image.attr("src") : null;
	}

	public String removeImage(final String content, final int imageIndex) {
		final Document document = parse(content);
		final Element image = imageAt(document, imageIndex);
		if (image == null) {
			return content;
		}
		final Element parent = image.parent();
		image.remove();
		if (parent != null && parent.tagName().equals("p")
				&& parent.text().trim().length() == 0
				&& parent.children().isEmpty()) {
			parent.remove();
		}
		if (document.body().children().isEmpty()) {
			return "";
		}
		return document.outerHtml();
	}

	public boolean isImageOnly(final String content) {
		final Document document = parse(content);
		return document.selectFirst("img") != null && document.body().text().trim().length() == 0;
	}

	private Document createShell() {
		final Document document = Document.createShell("");
		document.outputSettings().prettyPrint(false);
		return document;
	}

	private Document parse(final String content) {
		final Document document = content != null && HtmlUtils.isHtml(content)
				? Jsoup.parse(content)
				: Jsoup.parse(HtmlUtils.plainToHTML(content != null ? content : ""));
		document.outputSettings().prettyPrint(false);
		return document;
	}

	private void appendImageParagraph(final Element parent, final StoredImage image) {
		parent.appendChild(imageParagraph(parent.ownerDocument(), image));
	}

	private Element imageParagraph(final Document document, final StoredImage storedImage) {
		final Element paragraph = document.createElement("p");
		final Element image = paragraph.appendElement("img").attr("src", storedImage.getSource());
		if (storedImage.hasKnownSize()) {
			final String width = Integer.toString(storedImage.getWidth());
			final String height = Integer.toString(storedImage.getHeight());
			image.attr("width", width);
			image.attr("height", height);
			image.attr("src-width", width);
			image.attr("src-height", height);
		}
		return paragraph;
	}

	private Element imageAt(final Document document, final int imageIndex) {
		if (imageIndex < 0) {
			return null;
		}
		final org.jsoup.select.Elements images = document.select("img");
		return imageIndex < images.size() ? images.get(imageIndex) : null;
	}

	private boolean hasPositiveIntAttribute(final Element element, final String attributeName) {
		return positiveIntAttribute(element, attributeName, -1) > 0;
	}

	private int positiveIntAttribute(final Element element, final String attributeName, final int defaultValue) {
		final String value = element.attr(attributeName);
		if (value.length() == 0) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		}
		catch (final NumberFormatException e) {
			return defaultValue;
		}
	}
}

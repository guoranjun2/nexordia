package org.freeplane.view.swing.map.cloud;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.attribute.NodeAttributeTableModel;

class CloudTitle {
	private static final String TITLE_ATTRIBUTE = "title";
	private static final Pattern IMAGE_SOURCE = Pattern.compile(
			"(?is).*<\\s*img\\b[^>]*\\bsrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+)).*");

	static CloudTitle from(NodeAttributeTableModel attributes, String mainViewText) {
		final int titleIndex = attributes.getAttributeIndex(TITLE_ATTRIBUTE);
		if (titleIndex >= 0) {
			final Object value = attributes.getValue(titleIndex);
			final String title = value == null ? "" : value.toString();
			return plain(toFirstPlainTextLine(title));
		}
		if (mainViewText == null) {
			return plain("");
		}
		if (HtmlUtils.isHtml(mainViewText)) {
			final String imageSource = imageSource(mainViewText);
			if (imageSource != null) {
				return image(imageSource);
			}
		}
		return plain(toFirstPlainTextLine(mainViewText));
	}

	private static String imageSource(String text) {
		final Matcher matcher = IMAGE_SOURCE.matcher(text);
		if (!matcher.matches()) {
			return null;
		}
		for (int i = 1; i <= 3; i++) {
			final String source = matcher.group(i);
			if (source != null) {
				return source;
			}
		}
		return null;
	}

	private static String toFirstPlainTextLine(String text) {
		final String plainText = HtmlUtils.isHtml(text) ? HtmlUtils.htmlToPlain(text) : text;
		final int firstLineEnd = plainText.indexOf('\n');
		return firstLineEnd >= 0 ? plainText.substring(0, firstLineEnd) : plainText;
	}

	private static CloudTitle plain(String text) {
		return new CloudTitle(text, null);
	}

	private static CloudTitle image(String imageSource) {
		return new CloudTitle("", imageSource);
	}

	private final String text;
	private final String imageSource;

	private CloudTitle(String text, String imageSource) {
		this.text = text;
		this.imageSource = imageSource;
	}

	String getText() {
		return text;
	}

	boolean isImage() {
		return imageSource != null;
	}

	String getImageSource() {
		return imageSource;
	}
}

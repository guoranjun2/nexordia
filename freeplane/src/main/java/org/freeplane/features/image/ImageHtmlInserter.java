package org.freeplane.features.image;

import org.freeplane.core.util.HtmlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ImageHtmlInserter {
	public String createImageHtml(final String source) {
		final Document document = createShell();
		appendImageParagraph(document.body(), source);
		return document.outerHtml();
	}

	public String insertImageAtTop(final String content, final String source) {
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
		final Element paragraph = imageParagraph(document, source);
		document.body().prependChild(paragraph);
		return document.outerHtml();
	}

	private Document createShell() {
		final Document document = Document.createShell("");
		document.outputSettings().prettyPrint(false);
		return document;
	}

	private void appendImageParagraph(final Element parent, final String source) {
		parent.appendChild(imageParagraph(parent.ownerDocument(), source));
	}

	private Element imageParagraph(final Document document, final String source) {
		final Element paragraph = document.createElement("p");
		paragraph.appendElement("img").attr("src", source);
		return paragraph;
	}
}

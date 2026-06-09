package org.freeplane.core.ui.components.html;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.JLabel;
import javax.swing.text.View;

import org.junit.Test;

public class SynchronousScaledEditorKitTest {
	@Test
	public void detectsGifImageSources() {
		assertThat(SynchronousScaledEditorKit.isGifSource("file:/image.gif")).isTrue();
		assertThat(SynchronousScaledEditorKit.isGifSource("file:/image.GIF?x=1")).isTrue();
		assertThat(SynchronousScaledEditorKit.isGifSource("file:/image.gif#fragment")).isTrue();
		assertThat(SynchronousScaledEditorKit.isGifSource("data:image/gif;base64,R0lGODlhAQABAAAAACw=")).isTrue();
		assertThat(SynchronousScaledEditorKit.isGifSource("file:/image.png")).isFalse();
	}

	@Test
	public void usesDefaultImageViewForGifImages() {
		final ScaledHTML.Renderer renderer = htmlView(
				"<html><body><img src=\"file:/missing-image.gif\" width=\"12\" height=\"34\"></body></html>");

		assertThat(findView(renderer, LazyScaledImageView.class)).isNull();
	}

	@Test
	public void usesLazyScaledImageViewForOtherImages() {
		final ScaledHTML.Renderer renderer = htmlView(
				"<html><body><img src=\"file:/missing-image.png\" width=\"12\" height=\"34\"></body></html>");

		assertThat(findView(renderer, LazyScaledImageView.class)).isNotNull();
	}

	private ScaledHTML.Renderer htmlView(String html) {
		final JLabel label = new JLabel();
		final ScaledHTML.Renderer renderer = ScaledHTML.createHTMLView(label, html);
		renderer.getPreferredSpan(View.X_AXIS);
		return renderer;
	}

	private <T> T findView(View view, Class<T> viewType) {
		if(viewType.isInstance(view))
			return viewType.cast(view);
		for(int i = 0; i < view.getViewCount(); i++) {
			final T imageView = findView(view.getView(i), viewType);
			if(imageView != null)
				return imageView;
		}
		return null;
	}
}

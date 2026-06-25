package org.freeplane.core.ui.components.html;

import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.lightdev.app.shtm.SHTMLEditorKit;

@SuppressWarnings("serial")
public class LazyImageSHTMLEditorKit extends SHTMLEditorKit {
	private static final ViewFactory FACTORY = new SHTMLFactory() {
		@Override
		public View create(Element elem) {
			if(elem.getName().equals("img")) {
				if(SynchronousScaledEditorKit.isGifImage(elem))
					return new ScaledGifImageView(elem);
				return new LazyScaledImageView(elem);
			}
			return super.create(elem);
		}
	};

	@Override
	public ViewFactory getViewFactory() {
		return FACTORY;
	}
}

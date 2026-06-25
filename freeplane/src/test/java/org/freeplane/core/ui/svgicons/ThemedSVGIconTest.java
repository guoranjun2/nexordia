package org.freeplane.core.ui.svgicons;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.freeplane.core.resources.ResourceController;
import org.junit.Test;

public class ThemedSVGIconTest {
	@Test
	public void usesCurrentComponentForegroundForAccentColor() throws Exception {
		Icon icon = FreeplaneIconFactory.createSVGIcon(svgUrl("#0071bc"), 16);
		JLabel component = new JLabel();
		component.setForeground(new Color(0x22, 0x33, 0x44));

		Color firstColor = paintCenterColor(icon, component);

		component.setForeground(new Color(0xcc, 0xdd, 0xee));
		Color secondColor = paintCenterColor(icon, component);

		assertThat(firstColor).isEqualTo(new Color(0x22, 0x33, 0x44));
		assertThat(secondColor).isEqualTo(new Color(0xcc, 0xdd, 0xee));
	}

	@Test
	public void usesCurrentComponentForegroundForNeutralColor() throws Exception {
		Icon icon = FreeplaneIconFactory.createSVGIcon(svgUrl("#333333"), 16);
		JLabel component = new JLabel();
		component.setForeground(new Color(0x11, 0x66, 0x99));

		Color color = paintCenterColor(icon, component);

		assertThat(color).isEqualTo(new Color(0x11, 0x66, 0x99));
	}

	private URL svgUrl(String fill) throws Exception {
		File file = File.createTempFile("themed-icon", ".svg");
		file.deleteOnExit();
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			writer.write("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\">"
					+ "<rect x=\"0\" y=\"0\" width=\"16\" height=\"16\" fill=\"" + fill + "\"/>"
					+ "</svg>");
		}
		return new URL(file.toURI().toURL().toString() + ResourceController.USE_ACCENT_COLOR_QUERY);
	}

	private Color paintCenterColor(Icon icon, JLabel component) {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			icon.paintIcon(component, graphics, 0, 0);
		}
		finally {
			graphics.dispose();
		}
		return new Color(image.getRGB(8, 8), true);
	}
}

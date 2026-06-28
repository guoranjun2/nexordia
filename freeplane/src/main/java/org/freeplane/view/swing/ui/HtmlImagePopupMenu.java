package org.freeplane.view.swing.ui;

import java.awt.Component;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.Document;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.image.ImageHtmlInserter;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.main.application.Browser;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.ZoomableLabel;

public class HtmlImagePopupMenu {
	public enum Target {
		NODE_TEXT,
		DETAILS
	}

	private final HtmlImageHitDetector hitDetector = new HtmlImageHitDetector();
	private final ImageHtmlInserter htmlInserter = new ImageHtmlInserter();

	public boolean showIfImagePopup(final MouseEvent e, final Target target) {
		if (!Compat.isPopupTrigger(e) || !(e.getComponent() instanceof ZoomableLabel)) {
			return false;
		}
		final ZoomableLabel label = (ZoomableLabel) e.getComponent();
		final NodeView nodeView = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, label);
		if (nodeView == null) {
			return false;
		}
		final HtmlImageHitDetector.Hit hit = hitDetector.findImageAt(label, e.getPoint(),
				content(nodeView.getNode(), target));
		if (hit == null) {
			return false;
		}
		if (!nodeView.isSelected()) {
			Controller.getCurrentController().getSelection().selectAsTheOnlyOneSelected(nodeView.getNode());
		}
		final JPopupMenu menu = createMenu(label, nodeView, target, hit);
		new NodePopupMenuDisplayer().showMenuAndConsumeEvent(menu, e);
		return true;
	}

	private JPopupMenu createMenu(final ZoomableLabel label, final NodeView nodeView, final Target target,
			final HtmlImageHitDetector.Hit hit) {
		final URL imageUrl = resolveImageUrl(label, nodeView, hit.getSource());
		final File imageFile = fileFromUrl(imageUrl);
		final JPopupMenu menu = new JPopupMenu();

		final JMenuItem openItem = new JMenuItem(TextUtils.getText("breadcrumb.open", "Open"));
		openItem.setEnabled(imageUrl != null);
		openItem.addActionListener(e -> openImage(imageUrl));
		menu.add(openItem);

		final JMenuItem locateItem = new JMenuItem(TextUtils.getText("breadcrumb.locate", "Locate"));
		locateItem.setEnabled(imageFile != null);
		locateItem.addActionListener(e -> revealImageFile(label, imageFile));
		menu.add(locateItem);

		menu.addSeparator();

		final boolean canEdit = nodeView.getMap().getModeController().canEdit(nodeView.getNode().getMap());
		final JMenuItem deleteReferenceItem = new JMenuItem(
				TextUtils.getText("html_image.delete_reference", "Delete reference"));
		deleteReferenceItem.setEnabled(canEdit);
		deleteReferenceItem.addActionListener(e -> deleteReference(label, nodeView.getNode(), target,
				hit.getImageIndex(), TextUtils.getText("html_image.delete_reference.confirm",
						"Delete this image reference?")));
		menu.add(deleteReferenceItem);

		final JMenuItem deleteFileAndReferenceItem = new JMenuItem(
				TextUtils.getText("html_image.delete_file_and_reference", "Delete file and reference"));
		deleteFileAndReferenceItem.setForeground(Color.RED);
		deleteFileAndReferenceItem.setEnabled(canEdit && imageFile != null && imageFile.isFile());
		deleteFileAndReferenceItem.addActionListener(e -> deleteFileAndReference(label, nodeView.getNode(), target,
				hit.getImageIndex(), imageFile));
		menu.add(deleteFileAndReferenceItem);

		return menu;
	}

	private URL resolveImageUrl(final ZoomableLabel label, final NodeView nodeView, final String source) {
		if (source == null || source.length() == 0) {
			return null;
		}
		try {
			return new URL(source);
		}
		catch (final MalformedURLException e) {
		}
		final URL documentBase = documentBase(label);
		if (documentBase != null) {
			try {
				return new URL(documentBase, source);
			}
			catch (final MalformedURLException e) {
			}
		}
		final URL mapUrl = nodeView.getMap().getMap().getURL();
		if (mapUrl != null) {
			try {
				return new URL(mapUrl, source);
			}
			catch (final MalformedURLException e) {
			}
		}
		final File file = new File(source);
		try {
			return file.isAbsolute() ? file.toURI().toURL() : null;
		}
		catch (final MalformedURLException e) {
			return null;
		}
	}

	private URL documentBase(final ZoomableLabel label) {
		final View renderer = (View) label.getClientProperty(BasicHTML.propertyKey);
		if (renderer == null) {
			return null;
		}
		final Document document = renderer.getDocument();
		return document instanceof HTMLDocument ? ((HTMLDocument) document).getBase() : null;
	}

	private File fileFromUrl(final URL url) {
		if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
			return null;
		}
		try {
			return new File(url.toURI());
		}
		catch (final Exception e) {
			return null;
		}
	}

	private void openImage(final URL imageUrl) {
		if (imageUrl == null) {
			return;
		}
		try {
			Controller.getCurrentController().getViewController().openDocument(imageUrl);
		}
		catch (final Exception e) {
			LogUtils.warn(e);
			UITools.errorMessage(e.getMessage());
		}
	}

	private void revealImageFile(final Component parent, final File imageFile) {
		if (imageFile == null) {
			return;
		}
		try {
			final File target = imageFile.exists() ? imageFile : imageFile.getParentFile();
			if (target == null) {
				return;
			}
			if (Compat.isMacOsX()) {
				Controller.exec(new String[] {"open", "-R", target.getAbsolutePath()});
			}
			else if (Compat.isWindowsOS()) {
				Controller.exec(new String[] {"explorer.exe", "/select," + target.getAbsolutePath()});
			}
			else {
				final File folder = target.isDirectory() ? target : target.getParentFile();
				if (folder != null) {
					new Browser().openDocument(new Hyperlink(folder.toURI()));
				}
			}
		}
		catch (final Exception e) {
			LogUtils.warn(e);
			UITools.errorMessage(e.getMessage());
		}
	}

	private void deleteReference(final Component parent, final NodeModel node, final Target target,
			final int imageIndex, final String confirmationMessage) {
		final String oldContent = content(node, target);
		final String newContent = htmlInserter.removeImage(oldContent, imageIndex);
		if (Objects.equals(oldContent, newContent)) {
			return;
		}
		final int choice = JOptionPane.showConfirmDialog(parent, confirmationMessage,
				TextUtils.getText("delete", "Delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (choice != JOptionPane.YES_OPTION) {
			return;
		}
		setContent(node, target, newContent);
	}

	private void deleteFileAndReference(final Component parent, final NodeModel node, final Target target,
			final int imageIndex, final File imageFile) {
		final String oldContent = content(node, target);
		final String newContent = htmlInserter.removeImage(oldContent, imageIndex);
		if (Objects.equals(oldContent, newContent)) {
			return;
		}
		final int choice = JOptionPane.showConfirmDialog(parent,
				TextUtils.getText("html_image.delete_file_and_reference.confirm",
						"Delete this image file and remove its reference?"),
				TextUtils.getText("delete", "Delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (choice != JOptionPane.YES_OPTION) {
			return;
		}
		setContent(node, target, newContent);
		try {
			Files.delete(imageFile.toPath());
		}
		catch (final IOException e) {
			LogUtils.warn(e);
			UITools.errorMessage(e.getMessage());
		}
	}

	private void setContent(final NodeModel node, final Target target, final String newContent) {
		if (target == Target.DETAILS) {
			MTextController.getController().setDetails(node, newContent);
		}
		else {
			MTextController.getController().setNodeText(node, newContent);
		}
	}

	private String content(final NodeModel node, final Target target) {
		return target == Target.DETAILS ? DetailModel.getDetailText(node) : node.getText();
	}
}

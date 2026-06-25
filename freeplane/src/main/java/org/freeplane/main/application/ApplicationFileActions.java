package org.freeplane.main.application;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.IMapViewManager;

class ApplicationFileActions {
	private ApplicationFileActions() {
	}

	static void addMenuItems(JPopupMenu menu, Component parent, File file) {
		JMenuItem openItem = new JMenuItem(TextUtils.getText("breadcrumb.open", "Open"));
		openItem.addActionListener(e -> open(parent, file));
		menu.add(openItem);

		JMenuItem locateItem = new JMenuItem(TextUtils.getText("breadcrumb.locate", "Locate"));
		locateItem.addActionListener(e -> reveal(parent, file));
		menu.add(locateItem);

		JMenuItem copyPathItem = new JMenuItem(TextUtils.getText("breadcrumb.copy_path", "Copy Path"));
		copyPathItem.addActionListener(e -> TextUtils.copyToClipboard(file.getAbsolutePath()));
		menu.add(copyPathItem);

		File parentFile = file.getParentFile();
		JMenuItem copyParentItem = new JMenuItem(TextUtils.getText("breadcrumb.copy_parent_path", "Copy Parent Path"));
		copyParentItem.setEnabled(parentFile != null);
		copyParentItem.addActionListener(e -> {
			if (parentFile != null) {
				TextUtils.copyToClipboard(parentFile.getAbsolutePath());
			}
		});
		menu.add(copyParentItem);

		menu.addSeparator();

		JMenuItem deleteItem = new JMenuItem(TextUtils.getText("breadcrumb.delete", "Delete"));
		deleteItem.setEnabled(file.getParentFile() != null);
		deleteItem.addActionListener(e -> delete(parent, file));
		menu.add(deleteItem);
	}

	static void open(Component parent, File file) {
		if (file == null || !file.exists()) {
			showError(TextUtils.getText("breadcrumb.file_not_found", "File not found"));
			return;
		}
		try {
			if (file.isFile() && isMindMapFile(file)) {
				openMindMap(file);
			}
			else {
				new Browser().openDocument(new Hyperlink(file.toURI()));
			}
		}
		catch (Exception e) {
			LogUtils.warn(e);
			showError(e.getMessage());
		}
	}

	static void reveal(Component parent, File file) {
		if (file == null) {
			return;
		}
		try {
			File target = file.exists() ? file : file.getParentFile();
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
				File folder = target.isDirectory() ? target : target.getParentFile();
				if (folder != null) {
					new Browser().openDocument(new Hyperlink(folder.toURI()));
				}
			}
		}
		catch (Exception e) {
			LogUtils.warn(e);
			showError(e.getMessage());
		}
	}

	static boolean isMindMapFile(File file) {
		return file != null && file.getName().toLowerCase(Locale.ROOT).endsWith(".mm");
	}

	private static void openMindMap(File file) throws Exception {
		URL url = Compat.fileToUrl(file);
		Controller controller = Controller.getCurrentController();
		controller.selectMode(MModeController.MODENAME);
		if (!controller.getMapViewManager().tryToChangeToMapView(url)) {
			ModeController modeController = Controller.getCurrentModeController();
			modeController.getExtension(MapIO.class).openMap(url);
		}
	}

	private static void delete(Component parent, File file) {
		if (file == null || !file.exists()) {
			showError(TextUtils.getText("breadcrumb.file_not_found", "File not found"));
			return;
		}
		String message = TextUtils.format("breadcrumb.delete.confirm", file.getAbsolutePath());
		if (message == null || "breadcrumb.delete.confirm".equals(message)) {
			message = "Delete " + file.getAbsolutePath() + "?";
		}
		int choice = JOptionPane.showConfirmDialog(parent, message,
				TextUtils.getText("breadcrumb.delete", "Delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (choice != JOptionPane.YES_OPTION || !closeLoadedMindMaps(file)) {
			return;
		}
		try {
			Files.delete(file.toPath());
		}
		catch (IOException e) {
			LogUtils.warn(e);
			showError(e.getMessage());
		}
	}

	private static boolean closeLoadedMindMaps(File file) {
		if (!isMindMapFile(file)) {
			return true;
		}
		IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
		for (MapModel map : new ArrayList<MapModel>(mapViewManager.getMaps().values())) {
			if (!sameFile(file, map.getFile())) {
				continue;
			}
			List<Component> views = new ArrayList<Component>(mapViewManager.getViews(map));
			for (Component view : views) {
				if (!mapViewManager.close(view)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean sameFile(File first, File second) {
		if (first == null || second == null) {
			return false;
		}
		try {
			return first.getCanonicalFile().equals(second.getCanonicalFile());
		}
		catch (IOException e) {
			return first.getAbsoluteFile().equals(second.getAbsoluteFile());
		}
	}

	private static void showError(String message) {
		UITools.errorMessage(message != null ? message : TextUtils.getText("error", "Error"));
	}
}

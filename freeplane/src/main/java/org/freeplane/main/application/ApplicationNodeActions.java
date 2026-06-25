package org.freeplane.main.application;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.IMapSelection.NodePosition;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

class ApplicationNodeActions {
	private ApplicationNodeActions() {
	}

	static void addMenuItems(JPopupMenu menu, Component parent, NodeModel node) {
		JMenuItem jumpItem = new JMenuItem(TextUtils.getText("breadcrumb.node.jump", "Jump"));
		jumpItem.addActionListener(e -> jumpTo(node));
		menu.add(jumpItem);

		JMenuItem copyItem = new JMenuItem(TextUtils.getText("breadcrumb.node.copy", "Copy"));
		copyItem.addActionListener(e -> copy(node));
		menu.add(copyItem);

		JMenuItem cutItem = new JMenuItem(TextUtils.getText("breadcrumb.node.cut", "Cut"));
		cutItem.setEnabled(!node.isRoot());
		cutItem.addActionListener(e -> cut(node));
		menu.add(cutItem);

		menu.addSeparator();

		JMenuItem deleteItem = new JMenuItem(TextUtils.getText("breadcrumb.node.delete", "Delete"));
		deleteItem.setEnabled(!node.isRoot());
		deleteItem.addActionListener(e -> delete(parent, node));
		menu.add(deleteItem);
	}

	static void jumpTo(NodeModel node) {
		select(node);
		Controller.getCurrentController().getSelection().moveNodeTo(node, NodePosition.CENTER);
	}

	private static void copy(NodeModel node) {
		select(node);
		runAction("CopyAction");
	}

	private static void cut(NodeModel node) {
		select(node);
		runAction("CutAction");
	}

	private static void delete(Component parent, NodeModel node) {
		if (node.isRoot()) {
			UITools.errorMessage(TextUtils.getText("cannot_delete_root"));
			return;
		}
		String message = TextUtils.format("breadcrumb.node.delete.confirm", node.toString());
		if (message == null || "breadcrumb.node.delete.confirm".equals(message)) {
			message = "Delete " + node + "?";
		}
		int choice = JOptionPane.showConfirmDialog(parent, message,
				TextUtils.getText("breadcrumb.node.delete", "Delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (choice != JOptionPane.YES_OPTION) {
			return;
		}
		select(node);
		ModeController modeController = Controller.getCurrentModeController();
		if (modeController.getMapController() instanceof MMapController) {
			((MMapController) modeController.getMapController()).deleteNode(node);
		}
	}

	private static void select(NodeModel node) {
		Controller controller = Controller.getCurrentController();
		controller.getMapViewManager().changeToMap(node.getMap());
		controller.getModeController().getMapController().displayNode(node);
		IMapSelection selection = controller.getSelection();
		selection.selectAsTheOnlyOneSelectedWithoutScrolling(node);
	}

	private static void runAction(String actionName) {
		AFreeplaneAction action = Controller.getCurrentModeController().getAction(actionName);
		if (action != null) {
			action.actionPerformed(new ActionEvent(action, ActionEvent.ACTION_PERFORMED, actionName));
		}
	}
}

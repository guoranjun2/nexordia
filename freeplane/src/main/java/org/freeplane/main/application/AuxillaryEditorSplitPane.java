/*
 * Created on 19 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.main.application;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

class AuxillaryEditorSplitPane extends JSplitPane {
	private static final String SPLIT_PANE_LAST_LEFT_POSITION = "split_pane_last_left_position";
	private static final String SPLIT_PANE_LAST_POSITION = "split_pane_last_position";
	private static final String SPLIT_PANE_LAST_RIGHT_POSITION = "split_pane_last_right_position";
	private static final String SPLIT_PANE_LAST_TOP_POSITION = "split_pane_last_top_position";


	private static final long serialVersionUID = 1L;
	private JComponent auxillaryComponent;
	private Component mainComponent;
	/** Contains the value where the Note Window should be displayed (right, left, top, bottom) */
	private String mLocationPreferenceValue;
	final private ApplicationResourceController resourceController;


	public AuxillaryEditorSplitPane(Component mainComponent) {
		resourceController = (ApplicationResourceController) ResourceController.getResourceController();
		mLocationPreferenceValue = resourceController.getProperty("note_location", "bottom");
		this.mainComponent = mainComponent;
		setLeftComponent(mainComponent);
		setRightComponent(null);
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed){
		return false;
	}

	@Override
	public void setLayout(LayoutManager layout) {
		if(layout == null || layout instanceof SplitPaneLayoutManagerDecorator)
			super.setLayout(layout);
		else if(layout instanceof LayoutManager2)
			super.setLayout(new SplitPaneLayoutManager2Decorator((LayoutManager2) layout));
		else
			super.setLayout(new SplitPaneLayoutManagerDecorator(layout));
	}
	public void insertComponentIntoSplitPane(final JComponent pMindMapComponent) {
		// --- Save the Component --
		auxillaryComponent = pMindMapComponent;
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		setLeftComponent(null);
		setRightComponent(null);
		if ("right".equals(mLocationPreferenceValue)) {
			setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			setLeftComponent(mainComponent);
			setRightComponent(pMindMapComponent);
		}
		else if ("left".equals(mLocationPreferenceValue)) {
			setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			setLeftComponent(pMindMapComponent);
			setRightComponent(mainComponent);
		}
		else if ("top".equals(mLocationPreferenceValue)) {
			setOrientation(JSplitPane.VERTICAL_SPLIT);
			setLeftComponent(pMindMapComponent);
			setRightComponent(mainComponent);
		}
		else {
			setOrientation(JSplitPane.VERTICAL_SPLIT);
			setLeftComponent(mainComponent);
			setRightComponent(pMindMapComponent);
		}
		if(focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, this)) {
		    focusOwner.requestFocusInWindow();
		}
		setContinuousLayout(true);
		setOneTouchExpandable(false);
		SwingUtilities.invokeLater(this::resetDividerLocation);

	}
	private void resetDividerLocation() {
		int lastSplitPanePosition = -1;
		if ("right".equals(mLocationPreferenceValue)) {
			lastSplitPanePosition = resourceController.getIntProperty(SPLIT_PANE_LAST_RIGHT_POSITION, -1);
		}
		else if ("left".equals(mLocationPreferenceValue)) {
			lastSplitPanePosition = resourceController.getIntProperty(SPLIT_PANE_LAST_LEFT_POSITION, -1);
		}
		else if ("top".equals(mLocationPreferenceValue)) {
			lastSplitPanePosition = resourceController.getIntProperty(SPLIT_PANE_LAST_TOP_POSITION, -1);
		}
		else if ("bottom".equals(mLocationPreferenceValue)) {
			lastSplitPanePosition = resourceController.getIntProperty(SPLIT_PANE_LAST_POSITION, -1);
		}

		if (lastSplitPanePosition != -1) {
			setDividerLocation(lastSplitPanePosition);
			setDividerLocation(lastSplitPanePosition);
		}
		else {
			setDividerLocation(0.5);
			setDividerLocation(0.5);
		}
	}

	void saveSplitPanePosition() {
		if ("right".equals(mLocationPreferenceValue)) {
			resourceController.setProperty(SPLIT_PANE_LAST_RIGHT_POSITION, "" + getLastDividerLocation());
		}
		else if ("left".equals(mLocationPreferenceValue)) {
			resourceController.setProperty(SPLIT_PANE_LAST_LEFT_POSITION, "" + getLastDividerLocation());
		}
		else if ("top".equals(mLocationPreferenceValue)) {
			resourceController.setProperty(SPLIT_PANE_LAST_TOP_POSITION, "" + getLastDividerLocation());
		}
		else { // "bottom".equals(mLocationPreferenceValue) also covered
			resourceController.setProperty(SPLIT_PANE_LAST_POSITION, "" + getLastDividerLocation());
		}
	}

	public void changeNoteWindowLocation() {
		saveSplitPanePosition();
		mLocationPreferenceValue = resourceController.getProperty("note_location");
		if(auxillaryComponent != null){
			insertComponentIntoSplitPane(auxillaryComponent);
		}
	}
	public void removeSplitPane() {
		saveSplitPanePosition();
		auxillaryComponent = null;
		setLeftComponent(null);
		setRightComponent(null);
		setLeftComponent(mainComponent);
		final Controller controller = Controller.getCurrentModeController().getController();
		final IMapSelection selection = controller.getSelection();
		if(selection == null){
			return;
		}
		final NodeModel node = selection.getSelected();
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				final Component component = controller.getMapViewManager().getComponent(node);
				if (component != null) {
					component.requestFocus();
				}
			}
		});
	}
}
/*
 * Created on 19 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.main.application;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;

class AuxillaryEditorSplitPane extends JSplitPane {
	private static final String AUX_SPLIT_PANE_LAST_LEFT_POSITION = "aux_split_pane_last_left_position";
	private static final String AUX_SPLIT_PANE_LAST_POSITION = "aux_split_pane_last_position";
	private static final String AUX_SPLIT_PANE_LAST_RIGHT_POSITION = "aux_split_pane_last_right_position";
	private static final String AUX_SPLIT_PANE_LAST_TOP_POSITION = "aux_split_pane_last_top_position";


	private static final long serialVersionUID = 1L;
	private JComponent auxillaryComponent;
	private Component mainComponent;
	/** Contains the value where the Note Window should be displayed (right, left, top, bottom) */
	private String auxillaryComponentLocation;
	final private ApplicationResourceController resourceController;


	public AuxillaryEditorSplitPane(Component mainComponent) {
		resourceController = (ApplicationResourceController) ResourceController.getResourceController();
		auxillaryComponentLocation = resourceController.getProperty("note_location", "bottom");
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
		auxillaryComponent = pMindMapComponent;
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		setLeftComponent(null);
		setRightComponent(null);
		if ("right".equals(auxillaryComponentLocation)) {
			setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			setLeftComponent(mainComponent);
			setRightComponent(pMindMapComponent);
		}
		else if ("left".equals(auxillaryComponentLocation)) {
			setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			setLeftComponent(pMindMapComponent);
			setRightComponent(mainComponent);
		}
		else if ("top".equals(auxillaryComponentLocation)) {
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
		double lastSplitPanePosition = Double.NaN;
		if ("right".equals(auxillaryComponentLocation)) {
			lastSplitPanePosition = resourceController.getDoubleProperty(AUX_SPLIT_PANE_LAST_RIGHT_POSITION, Double.NaN);
		}
		else if ("left".equals(auxillaryComponentLocation)) {
			lastSplitPanePosition = resourceController.getDoubleProperty(AUX_SPLIT_PANE_LAST_LEFT_POSITION, Double.NaN);
		}
		else if ("top".equals(auxillaryComponentLocation)) {
			lastSplitPanePosition = resourceController.getDoubleProperty(AUX_SPLIT_PANE_LAST_TOP_POSITION, Double.NaN);
		}
		else if ("bottom".equals(auxillaryComponentLocation)) {
			lastSplitPanePosition = resourceController.getDoubleProperty(AUX_SPLIT_PANE_LAST_POSITION, Double.NaN);
		}

		if (!Double.isNaN(lastSplitPanePosition)) {
			setDividerLocation(lastSplitPanePosition);
			setDividerLocation(lastSplitPanePosition);
		}
		else {
			setDividerLocation(0.5);
			setDividerLocation(0.5);
		}
	}

	void saveSplitPanePosition() {
		double proportionalLocation = getProportionalDividerLocation();
		if ("right".equals(auxillaryComponentLocation)) {
			resourceController.setProperty(AUX_SPLIT_PANE_LAST_RIGHT_POSITION, String.valueOf(proportionalLocation));
		}
		else if ("left".equals(auxillaryComponentLocation)) {
			resourceController.setProperty(AUX_SPLIT_PANE_LAST_LEFT_POSITION, String.valueOf(proportionalLocation));
		}
		else if ("top".equals(auxillaryComponentLocation)) {
			resourceController.setProperty(AUX_SPLIT_PANE_LAST_TOP_POSITION, String.valueOf(proportionalLocation));
		}
		else {
			resourceController.setProperty(AUX_SPLIT_PANE_LAST_POSITION, String.valueOf(proportionalLocation));
		}
	}

	public double getProportionalDividerLocation() {
		if (getOrientation() == VERTICAL_SPLIT) {
			int height = getHeight() - getDividerSize();
			return height > 0 ? (double) getDividerLocation() / height : 0.0;
		} else {
			int width = getWidth() - getDividerSize();
			return width > 0 ? (double) getDividerLocation() / width : 0.0;
		}
	}

	public void changeNoteWindowLocation(String location) {
		if(location == null || location.equals(auxillaryComponentLocation))
			return;
		saveSplitPanePosition();
		this.auxillaryComponentLocation = resourceController.getProperty("note_location");
		if(getLeftComponent() != null && getRightComponent() != null){
			insertComponentIntoSplitPane(auxillaryComponent);
		}
	}

	public JComponent getAuxiliaryComponent() {
		return auxillaryComponent;
	}

	public void removeAuxiliaryComponent() {
		if (auxillaryComponent != null) {
			saveSplitPanePosition();
			setLeftComponent(null);
			setRightComponent(null);
			setLeftComponent(mainComponent);
			auxillaryComponent = null;
		}
	}
}
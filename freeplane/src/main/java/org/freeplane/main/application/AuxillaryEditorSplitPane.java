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
	private static final String AUX_SPLIT_PANE_LAST_POSITION = "aux_split_pane_last_position";


	private static final long serialVersionUID = 1L;
	private JComponent auxillaryComponent;
	private Component mainComponent;
	private boolean dividerLocationIsRestored;
	/** Contains the value where the Note Window should be displayed (right, left, top, bottom) */
	private static String auxillaryComponentLocation = ResourceController.getResourceController().getProperty("note_location", "bottom");
	final private ApplicationResourceController resourceController;


	private String mode;

	public AuxillaryEditorSplitPane(Component mainComponent) {
		resourceController = (ApplicationResourceController) ResourceController.getResourceController();
		this.mainComponent = mainComponent;
		if (JSplitPane.TOP.equals(auxillaryComponentLocation) || JSplitPane.LEFT.equals(auxillaryComponentLocation)) {
			setLeftComponent(null);
			setRightComponent(mainComponent);
		} else {
			setLeftComponent(mainComponent);
			setRightComponent(null);
		}
		dividerLocationIsRestored = false;
		setResizeWeight(0.5);
		setContinuousLayout(true);
		setOneTouchExpandable(false);
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
	void insertComponentIntoSplitPane(final JComponent pAuxillaryComponent, String mode) {
		this.mode = mode;
		insertComponentIntoSplitPane(pAuxillaryComponent);
	}

	private void insertComponentIntoSplitPane(final JComponent pAuxillaryComponent) {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (JSplitPane.RIGHT.equals(auxillaryComponentLocation) || JSplitPane.LEFT.equals(auxillaryComponentLocation)) {
			if(getOrientation() != JSplitPane.HORIZONTAL_SPLIT) {
				setOrientation(JSplitPane.HORIZONTAL_SPLIT);
				dividerLocationIsRestored = false;
			}
		}
		else {
			if(getOrientation() != JSplitPane.VERTICAL_SPLIT) {
				setOrientation(JSplitPane.VERTICAL_SPLIT);
				dividerLocationIsRestored = false;
			}
		}
		if (JSplitPane.TOP.equals(auxillaryComponentLocation) || JSplitPane.LEFT.equals(auxillaryComponentLocation)) {
			if(getRightComponent() != mainComponent) {
				repositionComponent(mainComponent, JSplitPane.RIGHT);
			}
			if(getLeftComponent() != pAuxillaryComponent) {
				if(auxillaryComponent == pAuxillaryComponent)
					repositionComponent(auxillaryComponent, JSplitPane.LEFT);
				else {
					auxillaryComponent = pAuxillaryComponent;
					setLeftComponent(pAuxillaryComponent);
				}
			}
		}
		else {
			if(getLeftComponent() != mainComponent) {
				repositionComponent(mainComponent, JSplitPane.LEFT);
				dividerLocationIsRestored = false;
			}
			if(getRightComponent() != pAuxillaryComponent) {
				if(auxillaryComponent == pAuxillaryComponent)
					repositionComponent(auxillaryComponent, JSplitPane.RIGHT);
				else {
					auxillaryComponent = pAuxillaryComponent;
					setRightComponent(pAuxillaryComponent);
				}
				dividerLocationIsRestored = false;
			}
		}
		if(focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, this)) {
		    focusOwner.requestFocusInWindow();
		}
		revalidate();
		repaint();
	}

	private void repositionComponent(Component component, String constraints) {
		if(JSplitPane.RIGHT.equals(constraints)) {
			rightComponent = component;
			if(leftComponent == component)
				leftComponent = null;
		} else {
			leftComponent = component;
			if(rightComponent == component)
				rightComponent = null;
		}
		final LayoutManager layoutMgr = getLayout();
		if (layoutMgr != null) {
			layoutMgr.removeLayoutComponent(component);
		    if (layoutMgr instanceof LayoutManager2) {
		        ((LayoutManager2)layoutMgr).addLayoutComponent(component, constraints);
		    } else {
		        layoutMgr.addLayoutComponent(constraints, component);
		    }
		}
	}




	@Override
	protected void validateTree() {
		final boolean dividerLocationWasRestored = dividerLocationIsRestored;
		final int dividerLocation = getDividerLocation();
		dividerLocationIsRestored = false;
		super.validateTree();
		dividerLocationIsRestored = dividerLocationWasRestored && dividerLocation == getDividerLocation();
		restoreDividerLocation();
	}

	private void restoreDividerLocation() {
		if(dividerLocationIsRestored || auxillaryComponent == null)
			return;
		double lastSplitPanePosition = Double.NaN;
		if (JSplitPane.LEFT.equals(auxillaryComponentLocation) || JSplitPane.TOP.equals(auxillaryComponentLocation)) {
			lastSplitPanePosition = 1.0 - resourceController.getDoubleProperty(AUX_SPLIT_PANE_LAST_POSITION, Double.NaN);
		}
		else {
			lastSplitPanePosition = resourceController.getDoubleProperty(AUX_SPLIT_PANE_LAST_POSITION, Double.NaN);
		}

		if (Double.isNaN(lastSplitPanePosition)) {
			setDividerLocation(0.5);
		} else if(getProportionalDividerLocation() != lastSplitPanePosition) {
			setDividerLocation(lastSplitPanePosition);
		}
		dividerLocationIsRestored = true;
		invalidate();
		super.validateTree();
	}

	@Override
	public void setDividerLocation(int location) {
		super.setDividerLocation(location);
		if(dividerLocationIsRestored)
			saveSplitPanePosition();
	}

	private void saveSplitPanePosition() {
		double proportionalLocation = getProportionalDividerLocation();
		if ("left".equals(auxillaryComponentLocation) || "top".equals(auxillaryComponentLocation)) {
			resourceController.setProperty(AUX_SPLIT_PANE_LAST_POSITION, String.valueOf(1.0 - proportionalLocation));
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
		auxillaryComponentLocation = resourceController.getProperty("note_location");
		if(getLeftComponent() != null && getRightComponent() != null){
			insertComponentIntoSplitPane(auxillaryComponent);
		}
	}

	public JComponent getAuxiliaryComponent() {
		return auxillaryComponent;
	}


	@Override
	public void remove(Component component) {
		if(component == auxillaryComponent)
			removeAuxiliaryComponent();
		else
			super.remove(component);
	}

	void removeAuxiliaryComponent() {
		if (auxillaryComponent != null) {
			super.remove(auxillaryComponent);
			auxillaryComponent = null;
			dividerLocationIsRestored = false;
		}
	}

	public void moveAuxillaryComponentTo(AuxillaryEditorSplitPane toSplitPane, String targetMode) {
		if (auxillaryComponent != null) {
			final boolean isModeSame = targetMode.equals(mode);
			if(isModeSame)
				toSplitPane.insertComponentIntoSplitPane(auxillaryComponent, mode);
			else
				removeAuxiliaryComponent();
		}
	}
}
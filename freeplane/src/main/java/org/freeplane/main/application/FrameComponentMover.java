/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2025 Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.main.application;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.freeplane.core.util.Compat;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.IMapViewChangeListener;

class FrameComponentMover implements IMapViewChangeListener, PropertyChangeListener{
	private JFrame lastFocusedFrame = null;

	public FrameComponentMover(JFrame frame) {
		lastFocusedFrame = frame;
	}

	public void install() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addPropertyChangeListener("focusedWindow", this);
	}

	public void uninstall() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.removePropertyChangeListener("focusedWindow", this);
	}


	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Window newFocusedWindow = (Window) evt.getNewValue();
		final Component selectedComponent = Controller.getCurrentController().getMapViewManager().getSelectedComponent();
		if(SwingUtilities.getWindowAncestor(selectedComponent) == newFocusedWindow)
			afterFocusedWindowChange(newFocusedWindow);
	}

	@Override
	public void afterViewChange(Component oldView, Component newView) {
		if(newView == null)
			return;
		Window newFocusedWindow = SwingUtilities.getWindowAncestor(newView);
		afterFocusedWindowChange(newFocusedWindow);
	}

	private void afterFocusedWindowChange(Window newFocusedWindow) {
		if (newFocusedWindow instanceof JFrame) {
			JFrame currentFrame = (JFrame) newFocusedWindow;

			if (lastFocusedFrame != null && lastFocusedFrame != currentFrame) {
				moveNonCenterComponents(lastFocusedFrame, currentFrame);
			}

			lastFocusedFrame = currentFrame;
		}
	}

	private void moveNonCenterComponents(JFrame fromFrame, JFrame toFrame) {
		moveMenuBar(fromFrame, toFrame);

		Container fromContentPane = fromFrame.getContentPane();
		Container toContentPane = toFrame.getContentPane();

		if (!(fromContentPane.getLayout() instanceof BorderLayout) ||
			!(toContentPane.getLayout() instanceof BorderLayout)) {
			return;
		}

		moveComponentIfExists(fromContentPane, toContentPane, BorderLayout.NORTH);
		moveComponentIfExists(fromContentPane, toContentPane, BorderLayout.SOUTH);
		moveComponentIfExists(fromContentPane, toContentPane, BorderLayout.EAST);
		moveComponentIfExists(fromContentPane, toContentPane, BorderLayout.WEST);

		moveAuxiliaryComponents(fromFrame, toFrame);

		fromContentPane.revalidate();
		fromContentPane.repaint();
		toContentPane.revalidate();
		toContentPane.repaint();
	}

	private void moveMenuBar(JFrame fromFrame, JFrame toFrame) {
		javax.swing.JMenuBar menuBar = fromFrame.getJMenuBar();
		if (menuBar != null) {
			if(Compat.isMacOsX()) {
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				fromFrame.setJMenuBar(null);
				toFrame.setJMenuBar(menuBar);
				System.setProperty("apple.laf.useScreenMenuBar", "false");
			} else {
				fromFrame.setJMenuBar(null);
				toFrame.setJMenuBar(menuBar);
			}
		}

	}

	private void moveComponentIfExists(Container fromPane, Container toPane, String position) {
		BorderLayout fromLayout = (BorderLayout) fromPane.getLayout();
		Component component = fromLayout.getLayoutComponent(fromPane, position);
		if (component != null) {
			fromPane.remove(component);
			toPane.add(component, position);
		}
	}

	private void moveAuxiliaryComponents(JFrame fromFrame, JFrame toFrame) {
		AuxillaryEditorSplitPane fromSplitPane = findAuxiliarySplitPane(fromFrame);
		AuxillaryEditorSplitPane toSplitPane = findAuxiliarySplitPane(toFrame);

		if (fromSplitPane != null && toSplitPane != null) {
			EventQueue.invokeLater(() ->
				fromSplitPane.moveAuxillaryComponentTo(toSplitPane, Controller.getCurrentModeController().getModeName()));
		}
	}

	private AuxillaryEditorSplitPane findAuxiliarySplitPane(JFrame frame) {
		Container contentPane = frame.getContentPane();
		if (contentPane.getLayout() instanceof BorderLayout) {
			Component centerComponent = ((BorderLayout) contentPane.getLayout())
				.getLayoutComponent(contentPane, BorderLayout.CENTER);
			if (centerComponent instanceof AuxillaryEditorSplitPane) {
				return (AuxillaryEditorSplitPane) centerComponent;
			}
		}
		return null;
	}
}
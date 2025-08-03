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
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.freeplane.core.util.Compat;

class FrameComponentMover implements PropertyChangeListener {
	private JFrame lastFocusedFrame = null;

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
}
package org.freeplane.features.ui.toolwindow;

import java.util.LinkedHashMap;
import java.util.Map;

public class ToolWindowLayoutModel {
	public static final int DEFAULT_REGION = 1;
	private final Map<String, ToolWindowState> states = new LinkedHashMap<String, ToolWindowState>();

	public ToolWindowState register(String id, ToolWindowAnchor anchor, boolean visible) {
		return register(id, anchor, visible ? ToolWindowMode.DOCKED : ToolWindowMode.HIDDEN, DEFAULT_REGION);
	}

	public ToolWindowState register(String id, ToolWindowAnchor anchor, ToolWindowMode mode) {
		return register(id, anchor, mode, DEFAULT_REGION);
	}

	public ToolWindowState register(String id, ToolWindowAnchor anchor, ToolWindowMode mode, int region) {
		ToolWindowState state = states.get(id);
		if (state == null) {
			state = new ToolWindowState(id, anchor, anchor, mode, region);
		}
		else if (state.mode() != ToolWindowMode.FLOATING) {
			state = new ToolWindowState(id, anchor, anchor, mode, region);
		}
		states.put(id, state);
		if (state.mode() == ToolWindowMode.DOCKED) {
			deactivateOtherWindowsInRegion(state);
		}
		return state;
	}

	public void unregister(String id) {
		states.remove(id);
	}

	public ToolWindowState state(String id) {
		return require(id);
	}

	public ToolWindowState activate(String id) {
		ToolWindowState state = require(id);
		if (state.mode() == ToolWindowMode.HIDDEN || state.mode() == ToolWindowMode.INACTIVE) {
			ToolWindowState dockedState = store(state.withMode(ToolWindowMode.DOCKED));
			deactivateOtherWindowsInRegion(dockedState);
			return dockedState;
		}
		if (state.mode() == ToolWindowMode.DOCKED) {
			return store(state.withMode(ToolWindowMode.INACTIVE));
		}
		return state;
	}

	public ToolWindowState hide(String id) {
		return store(require(id).withMode(ToolWindowMode.HIDDEN));
	}

	public ToolWindowState show(String id) {
		ToolWindowState state = store(require(id).withMode(ToolWindowMode.DOCKED));
		deactivateOtherWindowsInRegion(state);
		return state;
	}

	public ToolWindowState floatWindow(String id) {
		return store(require(id).withMode(ToolWindowMode.FLOATING));
	}

	public ToolWindowState dock(String id) {
		ToolWindowState state = require(id);
		ToolWindowState dockedState = store(state.dockedAt(state.restoreAnchor()));
		deactivateOtherWindowsInRegion(dockedState);
		return dockedState;
	}

	public ToolWindowState dock(String id, ToolWindowAnchor anchor) {
		ToolWindowState state = store(require(id).dockedAt(anchor));
		deactivateOtherWindowsInRegion(state);
		return state;
	}

	public ToolWindowState moveToRegion(String id, int region) {
		ToolWindowState state = store(require(id).inRegion(region).withMode(ToolWindowMode.DOCKED));
		deactivateOtherWindowsInRegion(state);
		return state;
	}

	public ToolWindowState moveHiddenToRegion(String id, int region) {
		ToolWindowState state = require(id);
		if (state.mode() != ToolWindowMode.HIDDEN) {
			return state;
		}
		return store(state.inRegion(region));
	}

	private ToolWindowState require(String id) {
		ToolWindowState state = states.get(id);
		if (state == null) {
			throw new IllegalArgumentException("Unknown tool window " + id);
		}
		return state;
	}

	private ToolWindowState store(ToolWindowState state) {
		states.put(state.id(), state);
		return state;
	}

	private void deactivateOtherWindowsInRegion(ToolWindowState activeState) {
		for (ToolWindowState state : states.values().toArray(new ToolWindowState[0])) {
			if (!state.id().equals(activeState.id()) && state.region() == activeState.region()
					&& state.mode() == ToolWindowMode.DOCKED) {
				store(state.withMode(ToolWindowMode.INACTIVE));
			}
		}
	}
}

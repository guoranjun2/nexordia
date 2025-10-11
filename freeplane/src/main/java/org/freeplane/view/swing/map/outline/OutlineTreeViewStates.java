package org.freeplane.view.swing.map.outline;

import java.util.EnumMap;
import java.util.Map;

class OutlineTreeViewStates {
    private OutlineDisplayMode currentMode;
    private boolean followsJumpIn;
    private final EnumMap<OutlineDisplayMode, OutlineTreeViewState> viewStatesByMode;

    OutlineTreeViewStates() {
        this(OutlineDisplayMode.MAP_VIEW, true, new EnumMap<>(OutlineDisplayMode.class));
    }

    OutlineTreeViewStates(OutlineDisplayMode currentMode,  boolean followsJumpIn, EnumMap<OutlineDisplayMode, OutlineTreeViewState> viewStatesByMode) {
        this.followsJumpIn = followsJumpIn;
		this.currentMode = currentMode != null ? currentMode : OutlineDisplayMode.MAP_VIEW;
        this.viewStatesByMode = viewStatesByMode;

    }

    OutlineTreeViewStates copy() {
        return new OutlineTreeViewStates(currentMode, followsJumpIn, new EnumMap<>(viewStatesByMode));
    }

    boolean followsJumpIn() {
		return followsJumpIn;
	}

	void setFollowsJumpIn(boolean followsJumpIn) {
		this.followsJumpIn = followsJumpIn;
	}

	OutlineDisplayMode getCurrentMode() {
        return currentMode;
    }

    void setCurrentMode(OutlineDisplayMode mode) {
        if (mode != null) {
            currentMode = mode;
        }
    }

    OutlineTreeViewState getViewState() {
        return viewStatesByMode.get(currentMode.baseMode());
    }

    void putViewState(OutlineTreeViewState state) {
        final OutlineDisplayMode stateKey = currentMode.baseMode();
		if (state == null) {
            viewStatesByMode.remove(stateKey);
        } else {
            viewStatesByMode.put(stateKey, state);
        }
    }

    void loadFrom(OutlineTreeViewStates other) {
        if (other == null) {
            return;
        }
        currentMode = other.currentMode;
        viewStatesByMode.clear();
        viewStatesByMode.putAll(other.viewStatesByMode);
    }

    void loadFromMap(Map<?, ?> storedStates) {
        viewStatesByMode.clear();
        currentMode = OutlineDisplayMode.MAP_VIEW;
        if (storedStates == null) {
            return;
        }
        for (Map.Entry<?, ?> entry : storedStates.entrySet()) {
            if (entry.getKey() instanceof OutlineDisplayMode && entry.getValue() instanceof OutlineTreeViewState) {
                viewStatesByMode.put((OutlineDisplayMode) entry.getKey(), (OutlineTreeViewState) entry.getValue());
            }
        }
    }

    void restoreFrom(Object storedState) {
        viewStatesByMode.clear();
        currentMode = OutlineDisplayMode.MAP_VIEW;
        if (storedState instanceof OutlineTreeViewStates) {
            loadFrom((OutlineTreeViewStates) storedState);
            return;
        }
        if (storedState instanceof Map<?, ?>) {
            loadFromMap((Map<?, ?>) storedState);
            return;
        }
        if (storedState instanceof OutlineTreeViewState) {
            viewStatesByMode.put(currentMode, (OutlineTreeViewState) storedState);
        }
    }
}

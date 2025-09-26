package org.freeplane.view.swing.map.outline;

import java.util.EnumMap;
import java.util.Map;

class OutlineDisplayState {
    private OutlineDisplayMode currentMode;
    private final EnumMap<OutlineDisplayMode, OutlineViewState> viewStatesByMode;

    OutlineDisplayState() {
        this(OutlineDisplayMode.MAP_VIEW, new EnumMap<>(OutlineDisplayMode.class));
    }

    OutlineDisplayState(OutlineDisplayMode currentMode, EnumMap<OutlineDisplayMode, OutlineViewState> viewStatesByMode) {
        this.currentMode = currentMode != null ? currentMode : OutlineDisplayMode.MAP_VIEW;
        this.viewStatesByMode = viewStatesByMode;
    }

    OutlineDisplayState copy() {
        return new OutlineDisplayState(currentMode, new EnumMap<>(viewStatesByMode));
    }

    OutlineDisplayMode getCurrentMode() {
        return currentMode;
    }

    void setCurrentMode(OutlineDisplayMode mode) {
        if (mode != null) {
            currentMode = mode;
        }
    }

    OutlineViewState getViewState() {
        return viewStatesByMode.get(currentMode);
    }

    void putViewState(OutlineViewState state) {
        if (state == null) {
            viewStatesByMode.remove(currentMode);
        } else {
            viewStatesByMode.put(currentMode, state);
        }
    }

    void loadFrom(OutlineDisplayState other) {
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
            if (entry.getKey() instanceof OutlineDisplayMode && entry.getValue() instanceof OutlineViewState) {
                viewStatesByMode.put((OutlineDisplayMode) entry.getKey(), (OutlineViewState) entry.getValue());
            }
        }
    }

    void restoreFrom(Object storedState) {
        viewStatesByMode.clear();
        currentMode = OutlineDisplayMode.MAP_VIEW;
        if (storedState instanceof OutlineDisplayState) {
            loadFrom((OutlineDisplayState) storedState);
            return;
        }
        if (storedState instanceof Map<?, ?>) {
            loadFromMap((Map<?, ?>) storedState);
            return;
        }
        if (storedState instanceof OutlineViewState) {
            viewStatesByMode.put(currentMode, (OutlineViewState) storedState);
        }
    }
}

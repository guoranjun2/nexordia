package org.freeplane.features.ui.toolwindow;

public class ToolWindowState {
	private final String id;
	private final ToolWindowAnchor anchor;
	private final ToolWindowAnchor restoreAnchor;
	private final ToolWindowMode mode;
	private final int region;

	ToolWindowState(String id, ToolWindowAnchor anchor, ToolWindowAnchor restoreAnchor, ToolWindowMode mode, int region) {
		this.id = id;
		this.anchor = anchor;
		this.restoreAnchor = restoreAnchor;
		this.mode = mode;
		this.region = region;
	}

	public String id() {
		return id;
	}

	public ToolWindowAnchor anchor() {
		return anchor;
	}

	public ToolWindowAnchor restoreAnchor() {
		return restoreAnchor;
	}

	public ToolWindowMode mode() {
		return mode;
	}

	public int region() {
		return region;
	}

	public boolean isVisible() {
		return mode == ToolWindowMode.DOCKED || mode == ToolWindowMode.FLOATING;
	}

	ToolWindowState withMode(ToolWindowMode mode) {
		return new ToolWindowState(id, anchor, mode == ToolWindowMode.FLOATING ? anchor : restoreAnchor, mode, region);
	}

	ToolWindowState dockedAt(ToolWindowAnchor anchor) {
		return new ToolWindowState(id, anchor, anchor, ToolWindowMode.DOCKED, region);
	}

	ToolWindowState inRegion(int region) {
		return new ToolWindowState(id, anchor, restoreAnchor, mode, region);
	}
}

package org.freeplane.plugin.ai.edits;

public class AiEditsStateIconDecision {
    public boolean shouldShowIcon(boolean isStateIconVisible, boolean hasAiEditsMarker) {
        return isStateIconVisible && hasAiEditsMarker;
    }
}

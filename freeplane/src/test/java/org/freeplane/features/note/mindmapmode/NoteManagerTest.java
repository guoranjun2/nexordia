package org.freeplane.features.note.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.net.URL;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;

public class NoteManagerTest {

    @Test
    public void resolveStartupNoteTargetReturnsSavedFrozenNoteWhenMapAndNodeAreAvailable() throws Exception {
        MapModel savedMap = mock(MapModel.class);
        NodeModel savedNode = mock(NodeModel.class);
        NodeModel selectedNode = mock(NodeModel.class);
        when(savedMap.getURL()).thenReturn(new URL("file:/saved-map.mm"));
        when(savedMap.getNodeForID("saved-node")).thenReturn(savedNode);

        NodeModel uut = NoteManager.resolveStartupNoteTarget("file:/saved-map.mm", "saved-node",
            Collections.singletonList(savedMap), selectedNode);

        assertThat(uut).isSameAs(savedNode);
    }

    @Test
    public void resolveStartupNoteTargetFallsBackToSelectionWhenSavedMapIsNotOpen() throws Exception {
        MapModel openMap = mock(MapModel.class);
        NodeModel selectedNode = mock(NodeModel.class);
        when(openMap.getURL()).thenReturn(new URL("file:/other-map.mm"));

        NodeModel uut = NoteManager.resolveStartupNoteTarget("file:/saved-map.mm", "saved-node",
            Collections.singletonList(openMap), selectedNode);

        assertThat(uut).isSameAs(selectedNode);
    }

    @Test
    public void resolveStartupNoteTargetFallsBackToSelectionWhenSavedNodeIsMissing() throws Exception {
        MapModel savedMap = mock(MapModel.class);
        NodeModel selectedNode = mock(NodeModel.class);
        when(savedMap.getURL()).thenReturn(new URL("file:/saved-map.mm"));
        when(savedMap.getNodeForID("missing-node")).thenReturn(null);

        NodeModel uut = NoteManager.resolveStartupNoteTarget("file:/saved-map.mm", "missing-node",
            Collections.singletonList(savedMap), selectedNode);

        assertThat(uut).isSameAs(selectedNode);
    }

    @Test
    public void resolveSavedFrozenNoteTargetReturnsNullForEmptySavedProperties() {
        MapModel openMap = mock(MapModel.class);

        NodeModel uut = NoteManager.resolveSavedFrozenNoteTarget("", "", Arrays.asList(openMap));

        assertThat(uut).isNull();
    }

    @Test
    public void resolveShutdownNoteTargetReturnsLastShownNoteNodeWhenFollowSelectionIsDisabled() {
        NodeModel lastShownNoteNode = mock(NodeModel.class);

        NodeModel uut = NoteManager.resolveShutdownNoteTarget(false, new WeakReference<>(lastShownNoteNode));

        assertThat(uut).isSameAs(lastShownNoteNode);
    }
}

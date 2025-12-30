package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

public class SystemMessageBuilderTest {
    @Test
    public void buildForChat_returnsIdentifiersWhenAvailable() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        MapModel mapModel = mock(MapModel.class);
        NodeModel rootNode = mock(NodeModel.class);
        UUID mapIdentifier = UUID.fromString("9b07af48-6f0e-4f19-a75c-58c80810cf8d");
        when(availableMaps.getCurrentMapIdentifier()).thenReturn(mapIdentifier);
        when(availableMaps.getCurrentMapModel()).thenReturn(mapModel);
        when(availableMaps.getCurrentSelectedNodeModel()).thenReturn(rootNode);
        when(mapModel.getRootNode()).thenReturn(rootNode);
        when(rootNode.getID()).thenReturn("ID_1");
        SystemMessageBuilder uut = new SystemMessageBuilder(availableMaps);

        String message = uut.buildForChat();

        assertThat(message).contains("Current map identifier: " + mapIdentifier);
        assertThat(message).contains("Current root node identifier: ID_1");
        assertThat(message).contains("Current selected node identifier: ID_1");
    }

    @Test
    public void buildForChat_usesNotAvailableWhenMissingMap() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        when(availableMaps.getCurrentMapIdentifier()).thenReturn(null);
        when(availableMaps.getCurrentMapModel()).thenReturn(null);
        when(availableMaps.getCurrentSelectedNodeModel()).thenReturn(null);
        SystemMessageBuilder uut = new SystemMessageBuilder(availableMaps);

        String message = uut.buildForChat();

        assertThat(message).contains("Current map identifier: not available");
        assertThat(message).contains("Current root node identifier: not available");
        assertThat(message).contains("Current selected node identifier: not available");
    }

    @Test
    public void buildForChat_usesNotAvailableWhenRootNodeMissing() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        MapModel mapModel = mock(MapModel.class);
        UUID mapIdentifier = UUID.fromString("31ae4d9b-552e-4e2b-97c4-7fa4793a2de0");
        when(availableMaps.getCurrentMapIdentifier()).thenReturn(mapIdentifier);
        when(availableMaps.getCurrentMapModel()).thenReturn(mapModel);
        when(availableMaps.getCurrentSelectedNodeModel()).thenReturn(null);
        when(mapModel.getRootNode()).thenReturn(null);
        SystemMessageBuilder uut = new SystemMessageBuilder(availableMaps);

        String message = uut.buildForChat();

        assertThat(message).contains("Current map identifier: " + mapIdentifier);
        assertThat(message).contains("Current root node identifier: not available");
        assertThat(message).contains("Current selected node identifier: not available");
    }
}

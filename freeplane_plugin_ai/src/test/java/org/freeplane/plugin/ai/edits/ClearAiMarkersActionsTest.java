package org.freeplane.plugin.ai.edits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.freeplane.core.undo.IActor;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearAiMarkersActionsTest {
    private Controller controllerBackup;

    @Before
    public void setUp() {
        controllerBackup = Controller.getCurrentController();
    }

    @After
    public void tearDown() {
        Controller.setCurrentController(controllerBackup);
    }

    @Test
    public void resetAiEditsForMapAction_removesAndRestoresMarkers() {
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel rootNode = new NodeModel("root", mapModel);
        mapModel.setRoot(rootNode);
        NodeModel childNode = new NodeModel("child", mapModel);
        rootNode.insert(childNode, 0);
        childNode.setMap(mapModel);
        childNode.addExtension(new AIEdits());
        Controller controller = mock(Controller.class);
        ModeController modeController = mock(ModeController.class);
        MapController mapController = mock(MapController.class);
        AtomicReference<IActor> actorReference = new AtomicReference<>();
        when(controller.getModeController()).thenReturn(modeController);
        when(controller.getMap()).thenReturn(mapModel);
        when(modeController.getMapController()).thenReturn(mapController);
        doAnswer(invocation -> {
            IActor actor = invocation.getArgument(0);
            actorReference.set(actor);
            actor.act();
            return null;
        }).when(modeController).execute(any(IActor.class), eq(mapModel));
        Controller.setCurrentController(controller);
        ClearAiMarkersInMapAction uut = new ClearAiMarkersInMapAction();

        uut.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "reset"));

        assertThat(childNode.getExtension(AIEdits.class)).isNull();
        actorReference.get().undo();
        assertThat(childNode.getExtension(AIEdits.class)).isNotNull();
    }

    @Test
    public void resetAiEditsForNodeAction_removesAndRestoresMarkers() {
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel rootNode = new NodeModel("root", mapModel);
        mapModel.setRoot(rootNode);
        NodeModel childNode = new NodeModel("child", mapModel);
        rootNode.insert(childNode, 0);
        childNode.setMap(mapModel);
        childNode.addExtension(new AIEdits());
        Controller controller = mock(Controller.class);
        ModeController modeController = mock(ModeController.class);
        MapController mapController = mock(MapController.class);
        AtomicReference<IActor> actorReference = new AtomicReference<>();
        when(controller.getModeController()).thenReturn(modeController);
        when(controller.getMap()).thenReturn(mapModel);
        when(modeController.getMapController()).thenReturn(mapController);
        when(mapController.getSelectedNodes()).thenReturn(Collections.singletonList(childNode));
        doAnswer(invocation -> {
            IActor actor = invocation.getArgument(0);
            actorReference.set(actor);
            actor.act();
            return null;
        }).when(modeController).execute(any(IActor.class), eq(mapModel));
        Controller.setCurrentController(controller);
        ClearAiMarkersInSelectionAction uut = new ClearAiMarkersInSelectionAction();

        uut.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "reset"));

        assertThat(childNode.getExtension(AIEdits.class)).isNull();
        actorReference.get().undo();
        assertThat(childNode.getExtension(AIEdits.class)).isNotNull();
    }
}

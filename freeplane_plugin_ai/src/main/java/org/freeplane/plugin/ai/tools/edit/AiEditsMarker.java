package org.freeplane.plugin.ai.tools.edit;

import org.freeplane.core.undo.IActor;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.ai.edits.AIEdits;

public class AiEditsMarker {
    public void addAiEditsMarkerWithUndo(NodeModel nodeModel) {
        if (nodeModel == null || nodeModel.isHiddenSummary()) {
            return;
        }
        if (nodeModel.getExtension(AIEdits.class) != null) {
            return;
        }
        Controller controller = Controller.getCurrentController();
        if (controller == null || controller.getModeController() == null) {
            nodeModel.addExtension(new AIEdits());
            return;
        }
        MapController mapController = controller.getModeController().getMapController();
        IActor actor = new IActor() {
            @Override
            public void act() {
                AIEdits aiEdits = new AIEdits();
                nodeModel.addExtension(aiEdits);
                mapController.nodeChanged(nodeModel, AIEdits.class, null, aiEdits);
            }

            @Override
            public void undo() {
                AIEdits aiEdits = nodeModel.getExtension(AIEdits.class);
                if (aiEdits == null) {
                    return;
                }
                nodeModel.removeExtension(AIEdits.class);
                mapController.nodeChanged(nodeModel, AIEdits.class, aiEdits, null);
            }

            @Override
            public String getDescription() {
                return "add ai edits marker";
            }
        };
        controller.getModeController().execute(actor, nodeModel.getMap());
    }
}

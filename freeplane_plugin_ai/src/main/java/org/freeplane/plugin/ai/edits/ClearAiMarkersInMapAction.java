package org.freeplane.plugin.ai.edits;

import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.undo.IActor;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeIterator;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

public class ClearAiMarkersInMapAction extends AFreeplaneAction {
    private static final long serialVersionUID = 1L;
    public static final String ACTION_KEY = "ClearAiMarkersInMapAction";

    public ClearAiMarkersInMapAction() {
        super(ACTION_KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        MapModel mapModel = Controller.getCurrentController().getMap();
        if (mapModel == null) {
            return;
        }
        Map<NodeModel, AIEdits> removedEditsByNode = collectNodesWithAiEdits(mapModel);
        if (removedEditsByNode.isEmpty()) {
            return;
        }
        IActor actor = new IActor() {
            @Override
            public void act() {
                removeAiEdits(removedEditsByNode);
            }

            @Override
            public void undo() {
                restoreAiEdits(removedEditsByNode);
            }

            @Override
            public String getDescription() {
                return getKey();
            }
        };
        Controller.getCurrentModeController().execute(actor, mapModel);
    }

    private Map<NodeModel, AIEdits> collectNodesWithAiEdits(MapModel mapModel) {
        Map<NodeModel, AIEdits> removedEditsByNode = new LinkedHashMap<>();
        NodeModel rootNode = mapModel.getRootNode();
        if (rootNode == null) {
            return removedEditsByNode;
        }
        NodeIterator<NodeModel> iterator = NodeIterator.of(rootNode, NodeModel::getChildren);
        while (iterator.hasNext()) {
            NodeModel node = iterator.next();
            AIEdits aiEdits = node.getExtension(AIEdits.class);
            if (aiEdits != null) {
                removedEditsByNode.put(node, aiEdits);
            }
        }
        return removedEditsByNode;
    }

    private void removeAiEdits(Map<NodeModel, AIEdits> removedEditsByNode) {
        MapController mapController = Controller.getCurrentModeController().getMapController();
        for (Map.Entry<NodeModel, AIEdits> entry : removedEditsByNode.entrySet()) {
            NodeModel node = entry.getKey();
            AIEdits existingEdits = entry.getValue();
            node.removeExtension(AIEdits.class);
            mapController.nodeChanged(node, AIEdits.class, existingEdits, null);
        }
    }

    private void restoreAiEdits(Map<NodeModel, AIEdits> removedEditsByNode) {
        MapController mapController = Controller.getCurrentModeController().getMapController();
        for (Map.Entry<NodeModel, AIEdits> entry : removedEditsByNode.entrySet()) {
            NodeModel node = entry.getKey();
            AIEdits existingEdits = entry.getValue();
            node.addExtension(existingEdits);
            mapController.nodeChanged(node, AIEdits.class, null, existingEdits);
        }
    }
}

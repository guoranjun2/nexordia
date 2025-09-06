package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.ref.WeakReference;

import org.freeplane.features.filter.Filter;

class OutlineViewState {
    final String firstVisibleNodeId;
    private final Map<String, Integer> expansionLevels;
    final String rootNodeId;
    final WeakReference<Filter> savedFilter;

    OutlineViewState(String firstVisibleNodeId, Map<String, Integer> expansionLevels, String rootNodeId, WeakReference<Filter> savedFilter) {
        this.firstVisibleNodeId = firstVisibleNodeId;
        this.expansionLevels = new HashMap<>(expansionLevels);
        this.rootNodeId = rootNodeId;
        this.savedFilter = savedFilter;
    }

    void applyTo(TreeNode root) {
        Map<String, TreeNode> byId = new HashMap<>();
        Map<String, Integer> depthById = new HashMap<>();
        collect(root, 0, byId, depthById);

        List<Map.Entry<String, Integer>> ordered = new ArrayList<>();
        for (Map.Entry<String, Integer> e : expansionLevels.entrySet()) {
            if (byId.containsKey(e.getKey())) ordered.add(e);
        }
        Collections.sort(ordered, Comparator.comparingInt(e -> depthById.get(e.getKey())));
        for (Map.Entry<String, Integer> e : ordered) {
            TreeNode n = byId.get(e.getKey());
            if (n != null) n.applyExpansionLevel(e.getValue());
        }
    }

    private void collect(TreeNode node, int depth, Map<String, TreeNode> byId, Map<String, Integer> depthById) {
        byId.put(node.getId(), node);
        depthById.put(node.getId(), depth);
        for (TreeNode c : node.getChildren()) collect(c, depth + 1, byId, depthById);
    }
}

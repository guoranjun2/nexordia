package org.freeplane.view.swing.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import org.freeplane.features.bookmarks.mindmapmode.MapBookmarks;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeDeletionEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeMoveEvent;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.features.url.UrlManager;
import org.junit.Test;

public class MapDisplayNameRefreshCoordinatorTest {

    @Test
    public void shouldRefreshForRelevantPropertyChangesOnly() {
        MutablePreferences preferences = new MutablePreferences(MapDisplayNameSource.FILE_NAME);
        RecordingRefresher refresher = new RecordingRefresher();
        RecordingBinder binder = new RecordingBinder();

        MapDisplayNameRefreshCoordinator coordinator = newCoordinator(preferences, refresher, binder, null);

        coordinator.propertyChanged(MapDisplayNameResolver.MAP_DISPLAY_NAME_SOURCE_PROPERTY, "a", "b");
        coordinator.propertyChanged(MapDisplayNameResolver.MAP_DISPLAY_NAME_MAX_LENGTH_PROPERTY, "1", "2");
        coordinator.propertyChanged("other", "1", "2");

        assertThat(refresher.callCount).isEqualTo(2);
    }

    @Test
    public void shouldRefreshForRelevantMapChangesOnly() {
        MutablePreferences preferences = new MutablePreferences(MapDisplayNameSource.FILE_NAME);
        RecordingRefresher refresher = new RecordingRefresher();

        MapDisplayNameRefreshCoordinator coordinator = newCoordinator(preferences, refresher, new RecordingBinder(), null);

        coordinator.mapChanged(new MapChangeEvent(this, IMapViewManager.MapChangeEventProperty.MAP_VIEW_ROOT, null, null));
        coordinator.mapChanged(new MapChangeEvent(this, MapBookmarks.class, null, null));
        coordinator.mapChanged(new MapChangeEvent(this, UrlManager.MAP_URL, null, null));
        coordinator.mapChanged(new MapChangeEvent(this, "other", null, null));

        assertThat(refresher.callCount).isEqualTo(3);
    }

    @Test
    public void shouldRefreshNodeTextChangesOnlyInViewRootTextMode() {
        MutablePreferences preferences = new MutablePreferences(MapDisplayNameSource.VIEW_ROOT_TEXT);
        RecordingRefresher refresher = new RecordingRefresher();

        MapDisplayNameRefreshCoordinator coordinator = newCoordinator(preferences, refresher, new RecordingBinder(), null);
        NodeModel node = createNodeModel();

        coordinator.nodeChanged(new NodeChangeEvent(node, NodeModel.NODE_TEXT, null, null, false, false));
        coordinator.nodeChanged(new NodeChangeEvent(node, NodeBookmark.class, null, null, false, false));

        assertThat(refresher.callCount).isEqualTo(1);
    }

    @Test
    public void shouldRefreshBookmarkChangesOnlyInViewRootBookmarkMode() {
        MutablePreferences preferences = new MutablePreferences(MapDisplayNameSource.VIEW_ROOT_BOOKMARK);
        RecordingRefresher refresher = new RecordingRefresher();

        MapDisplayNameRefreshCoordinator coordinator = newCoordinator(preferences, refresher, new RecordingBinder(), null);
        NodeModel node = createNodeModel();

        coordinator.nodeChanged(new NodeChangeEvent(node, NodeBookmark.class, null, null, false, false));
        coordinator.nodeChanged(new NodeChangeEvent(node, NodeModel.NODE_TEXT, null, null, false, false));

        assertThat(refresher.callCount).isEqualTo(1);
    }

    @Test
    public void shouldRefreshStructuralNodeEventsOnlyForAncestorDependentSources() {
        NodeModel node = createNodeModel();
        NodeDeletionEvent deletionEvent = new NodeDeletionEvent(node, node, 0);
        NodeMoveEvent moveEvent = new NodeMoveEvent(node, 0, node, node, 0);

        MutablePreferences fileNamePreferences = new MutablePreferences(MapDisplayNameSource.FILE_NAME);
        RecordingRefresher fileNameRefresher = new RecordingRefresher();
        MapDisplayNameRefreshCoordinator fileNameCoordinator = newCoordinator(fileNamePreferences, fileNameRefresher,
                new RecordingBinder(), null);

        fileNameCoordinator.onNodeInserted(node, node, 0);
        fileNameCoordinator.onNodeMoved(moveEvent);
        fileNameCoordinator.onNodeDeleted(deletionEvent);

        MutablePreferences rootPreferences = new MutablePreferences(MapDisplayNameSource.VIEW_ROOT_TEXT);
        RecordingRefresher rootRefresher = new RecordingRefresher();
        MapDisplayNameRefreshCoordinator rootCoordinator = newCoordinator(rootPreferences, rootRefresher,
                new RecordingBinder(), null);

        rootCoordinator.onNodeInserted(node, node, 0);
        rootCoordinator.onNodeMoved(moveEvent);
        rootCoordinator.onNodeDeleted(deletionEvent);

        assertThat(fileNameRefresher.callCount).isZero();
        assertThat(rootRefresher.callCount).isEqualTo(3);
    }

    @Test
    public void shouldRegisterAndUnregisterCustomizedTitleListeners() {
        MutablePreferences preferences = new MutablePreferences(MapDisplayNameSource.FILE_NAME);
        RecordingRefresher refresher = new RecordingRefresher();
        RecordingBinder binder = new RecordingBinder();
        JPanel existingView = new JPanel();

        MapDisplayNameRefreshCoordinator coordinator = newCoordinator(preferences, refresher, binder,
                Arrays.<Component>asList(existingView));

        assertThat(binder.registerCount).isEqualTo(1);

        JPanel newView = new JPanel();
        coordinator.afterViewCreated(newView);
        coordinator.afterViewClose(newView);

        assertThat(binder.registerCount).isEqualTo(2);
        assertThat(binder.unregisterCount).isEqualTo(1);
    }

    @Test
    public void shouldRefreshOnCustomizedTitlePropertyChange() {
        MutablePreferences preferences = new MutablePreferences(MapDisplayNameSource.FILE_NAME);
        RecordingRefresher refresher = new RecordingRefresher();
        RecordingBinder binder = new RecordingBinder();
        JPanel view = new JPanel();

        newCoordinator(preferences, refresher, binder, Arrays.<Component>asList(view));

        PropertyChangeListener listener = binder.listenerFor(view);
        listener.propertyChange(new PropertyChangeEvent(view,
                MapDisplayNameResolver.CUSTOMIZED_TAB_NAME_PROPERTY,
                "old",
                "new"));
        listener.propertyChange(new PropertyChangeEvent(view,
                MapDisplayNameResolver.CUSTOMIZED_TAB_NAME_PROPERTY,
                "same",
                "same"));
        listener.propertyChange(new PropertyChangeEvent(view,
                "otherProperty",
                "old",
                "new"));

        assertThat(refresher.callCount).isEqualTo(1);
    }

    private MapDisplayNameRefreshCoordinator newCoordinator(MutablePreferences preferences,
                                                            RecordingRefresher refresher,
                                                            RecordingBinder binder,
                                                            Iterable<? extends Component> existingViews) {
        return new MapDisplayNameRefreshCoordinator(preferences,
                refresher,
                listener -> {
                },
                existingViews,
                binder);
    }

    private NodeModel createNodeModel() {
        return new NodeModel(new MapModel(null, null, null));
    }

    private static class MutablePreferences implements MapDisplayNameRefreshCoordinator.MapNameChoicePreferences {
        private final MapDisplayNameSource source;

        private MutablePreferences(MapDisplayNameSource source) {
            this.source = source;
        }

        @Override
        public MapDisplayNameSource getDisplayNameSource() {
            return source;
        }
    }

    private static class RecordingRefresher implements MapDisplayNameRefreshCoordinator.MapTitlesRefresher {
        private int callCount;

        @Override
        public void setMapTitles() {
            callCount++;
        }
    }

    private static class RecordingBinder implements MapDisplayNameRefreshCoordinator.CustomizedTitleListenerBinder {
        private int registerCount;
        private int unregisterCount;
        private final Map<Component, PropertyChangeListener> listeners = new HashMap<Component, PropertyChangeListener>();

        @Override
        public void register(Component view, PropertyChangeListener listener) {
            registerCount++;
            listeners.put(view, listener);
        }

        @Override
        public void unregister(Component view, PropertyChangeListener listener) {
            unregisterCount++;
            listeners.remove(view);
        }

        private PropertyChangeListener listenerFor(Component view) {
            return listeners.get(view);
        }
    }
}

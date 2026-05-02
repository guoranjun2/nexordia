package org.freeplane.view.swing.ui;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.bookmarks.mindmapmode.MapBookmarks;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.INodeChangeListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeDeletionEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeMoveEvent;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.features.url.UrlManager;
import org.freeplane.view.swing.map.MapView;

public class MapDisplayNameRefreshCoordinator implements IMapViewChangeListener,
        IMapChangeListener, INodeChangeListener, IFreeplanePropertyListener {

    interface MapNameChoicePreferences {
        MapDisplayNameSource getDisplayNameSource();
    }

    interface MapTitlesRefresher {
        void setMapTitles();
    }

    interface PropertyListenerRegistrar {
        void addPropertyChangeListener(IFreeplanePropertyListener listener);
    }

    interface CustomizedTitleListenerBinder {
        void register(Component view, PropertyChangeListener listener);

        void unregister(Component view, PropertyChangeListener listener);
    }

    private static final class ResourceMapNameChoicePreferences implements MapNameChoicePreferences {
        @Override
        public MapDisplayNameSource getDisplayNameSource() {
            return ResourceController.getResourceController().getEnumProperty(
                    MapDisplayNameResolver.MAP_DISPLAY_NAME_SOURCE_PROPERTY,
                    MapDisplayNameSource.FILE_NAME);
        }
    }

    private static final class ControllerMapTitlesRefresher implements MapTitlesRefresher {
        @Override
        public void setMapTitles() {
            Controller.getCurrentController().getMapViewManager().setMapTitles();
        }
    }

    private static final class ResourcePropertyListenerRegistrar implements PropertyListenerRegistrar {
        @Override
        public void addPropertyChangeListener(IFreeplanePropertyListener listener) {
            ResourceController.getResourceController().addPropertyChangeListener(listener);
        }
    }

    private static final class MapViewCustomizedTitleListenerBinder implements CustomizedTitleListenerBinder {
        @Override
        public void register(Component view, PropertyChangeListener listener) {
            if (view instanceof MapView) {
                ((MapView) view).addPropertyChangeListener(MapDisplayNameResolver.CUSTOMIZED_TAB_NAME_PROPERTY,
                        listener);
            }
        }

        @Override
        public void unregister(Component view, PropertyChangeListener listener) {
            if (view instanceof MapView) {
                ((MapView) view).removePropertyChangeListener(MapDisplayNameResolver.CUSTOMIZED_TAB_NAME_PROPERTY,
                        listener);
            }
        }
    }

    private final PropertyChangeListener customizedTitleListener = this::onCustomizedTitleChanged;
    private final MapNameChoicePreferences mapNameChoicePreferences;
    private final MapTitlesRefresher mapTitlesRefresher;
    private final CustomizedTitleListenerBinder customizedTitleListenerBinder;

    public MapDisplayNameRefreshCoordinator() {
        this(new ResourceMapNameChoicePreferences(),
                new ControllerMapTitlesRefresher(),
                new ResourcePropertyListenerRegistrar(),
                new ArrayList<Component>(Controller.getCurrentController().getMapViewManager().getMapViews()),
                new MapViewCustomizedTitleListenerBinder());
    }

    MapDisplayNameRefreshCoordinator(MapNameChoicePreferences mapNameChoicePreferences,
                                     MapTitlesRefresher mapTitlesRefresher,
                                     PropertyListenerRegistrar propertyListenerRegistrar,
                                     Iterable<? extends Component> existingViews,
                                     CustomizedTitleListenerBinder customizedTitleListenerBinder) {
        this.mapNameChoicePreferences = mapNameChoicePreferences;
        this.mapTitlesRefresher = mapTitlesRefresher;
        this.customizedTitleListenerBinder = customizedTitleListenerBinder;
        if (propertyListenerRegistrar != null) {
            propertyListenerRegistrar.addPropertyChangeListener(this);
        }
        if (existingViews != null) {
            for (Component view : existingViews) {
                registerCustomizedTitleListener(view);
            }
        }
    }

    @Override
    public void afterViewCreated(Component newView) {
        registerCustomizedTitleListener(newView);
    }

    @Override
    public void afterViewClose(Component oldView) {
        unregisterCustomizedTitleListener(oldView);
    }

    @Override
    public void propertyChanged(String propertyName, String newValue, String oldValue) {
        if (MapDisplayNameResolver.MAP_DISPLAY_NAME_SOURCE_PROPERTY.equals(propertyName)
                || MapDisplayNameResolver.MAP_DISPLAY_NAME_MAX_LENGTH_PROPERTY.equals(propertyName)) {
            refreshMapTitles();
        }
    }

    @Override
    public void mapChanged(MapChangeEvent event) {
        Object property = event.getProperty();
        if (IMapViewManager.MapChangeEventProperty.MAP_VIEW_ROOT.equals(property)
                || MapBookmarks.class.equals(property)
                || UrlManager.MAP_URL.equals(property)) {
            refreshMapTitles();
        }
    }

    @Override
    public void onNodeDeleted(NodeDeletionEvent nodeDeletionEvent) {
        if (isAncestorDependentSource()) {
            refreshMapTitles();
        }
    }

    @Override
    public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
        if (isAncestorDependentSource()) {
            refreshMapTitles();
        }
    }

    @Override
    public void onNodeMoved(NodeMoveEvent nodeMoveEvent) {
        if (isAncestorDependentSource()) {
            refreshMapTitles();
        }
    }

    @Override
    public void nodeChanged(NodeChangeEvent event) {
        Object property = event.getProperty();
        MapDisplayNameSource source = getDisplayNameSource();
        if (source == MapDisplayNameSource.VIEW_ROOT_TEXT
                && NodeModel.NODE_TEXT.equals(property)) {
            refreshMapTitles();
        }
        else if (source == MapDisplayNameSource.VIEW_ROOT_BOOKMARK
                && NodeBookmark.class.equals(property)) {
            refreshMapTitles();
        }
    }

    private void onCustomizedTitleChanged(PropertyChangeEvent event) {
        if (!MapDisplayNameResolver.CUSTOMIZED_TAB_NAME_PROPERTY.equals(event.getPropertyName())) {
            return;
        }
        if (event.getOldValue() == event.getNewValue()
                || (event.getOldValue() != null && event.getOldValue().equals(event.getNewValue()))) {
            return;
        }
        refreshMapTitles();
    }

    private void registerCustomizedTitleListener(Component view) {
        customizedTitleListenerBinder.register(view, customizedTitleListener);
    }

    private void unregisterCustomizedTitleListener(Component view) {
        customizedTitleListenerBinder.unregister(view, customizedTitleListener);
    }

    private MapDisplayNameSource getDisplayNameSource() {
        return mapNameChoicePreferences.getDisplayNameSource();
    }

    private boolean isAncestorDependentSource() {
        return getDisplayNameSource() != MapDisplayNameSource.FILE_NAME;
    }

    private void refreshMapTitles() {
        mapTitlesRefresher.setMapTitles();
    }
}

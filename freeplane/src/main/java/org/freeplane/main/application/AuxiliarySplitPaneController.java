/*
 * Created on 23 Aug 2025
 *
 * author dimitry
 */
package org.freeplane.main.application;

import org.freeplane.core.resources.ResourceController;

/**
 * Controller for auxiliary split pane that uses configurable
 * property keys to support multiple split pane instances.
 */
class AuxiliarySplitPaneController {
    
    private final String positionPropertyKey;
    private final String locationPropertyKey;
    private final String defaultLocation;
    private final ApplicationResourceController resourceController;
    
    /**
     * Creates a controller with custom property keys.
     * 
     * @param positionPropertyKey key for storing divider position (e.g., "aux_split_pane_last_position")
     * @param locationPropertyKey key for storing location preference (e.g., "note_location")
     * @param defaultLocation default location if no preference is set (e.g., "bottom")
     */
    public AuxiliarySplitPaneController(String positionPropertyKey, String locationPropertyKey, String defaultLocation) {
        this.positionPropertyKey = positionPropertyKey;
        this.locationPropertyKey = locationPropertyKey;
        this.defaultLocation = defaultLocation;
        this.resourceController = (ApplicationResourceController) ResourceController.getResourceController();
    }
    
    
    public String getLocation() {
        return resourceController.getProperty(locationPropertyKey, defaultLocation);
    }
    
    public void setLocation(String location) {
        resourceController.setProperty(locationPropertyKey, location);
    }
    
    public double getPosition(double defaultValue) {
        return resourceController.getDoubleProperty(positionPropertyKey, defaultValue);
    }
    
    public void setPosition(double position) {
        resourceController.setProperty(positionPropertyKey, String.valueOf(position));
    }
    
    public String getDefaultLocation() {
        return defaultLocation;
    }
}
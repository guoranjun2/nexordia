package org.freeplane.view.swing.map.outline;

interface OutlineActionTarget {
    void navigateUp();
    void navigateDown();
    void navigatePageUp();
    void navigatePageDown();
    void goToParent();
    void goToChild();
    void expandSelectedMore();
    void reduceSelectedExpansion();
    void toggleExpandSelected();
}

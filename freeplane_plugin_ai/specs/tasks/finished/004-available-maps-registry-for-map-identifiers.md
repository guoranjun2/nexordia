# Task: AvailableMaps registry for map identifiers
- **Scope:** Introduce AvailableMaps to provide session map identifiers backed by weak references and allow lookup by identifier.
- **Modified production files:**
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/AvailableMaps.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/ControllerMapModelProvider.java
  - freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/maps/MapModelProvider.java
- **Modified test files:**
  - freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/maps/AvailableMapsTest.java
- **Research summary:**
```plantuml
@startuml
note "AvailableMaps can lazily enumerate maps and assign uuid values.\nWeak references avoid retaining closed maps; lifecycle listeners are optional.\nMap identifiers are uuid values in system messages.\nController.getCurrentController().getMapViewManager().getMaps().values() provides open maps; getMap provides current map.\nControllerProxy.getOpenMindMaps de-duplicates map views." as Research
@enduml
```
- **Design:**
```plantuml
@startuml
note "AvailableMaps maintains weak map from MapModel to uuid values and reverse map from uuid values to WeakReference<MapModel>.\nMapModelProvider provides current and open maps; ControllerMapModelProvider uses Controller.getCurrentController().getMapViewManager().\nExpose methods for current identifier, available identifiers, and map lookup." as Design
@enduml
```
- **Test specification:**
  - Verify uuid stability for a map model.
  - Verify identifier list for open maps.
  - Verify lookup from identifier to map model.

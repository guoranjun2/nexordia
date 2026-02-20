package org.freeplane.features.styles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.freeplane.core.resources.TranslatedObject;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;

public final class StyleNameMappingHelper {
    private static final String STYLE_NAME_MUST_NOT_BE_NULL = "styleName mustn't be null";

    private StyleNameMappingHelper() {
    }

    public static IStyle findStyleByName(MapModel map, String styleName) {
        Objects.requireNonNull(map, "map");
        String requiredStyleName = Objects.requireNonNull(styleName, STYLE_NAME_MUST_NOT_BE_NULL);
        MapStyleModel styleModel = MapStyleModel.getExtension(map);
        Set<IStyle> styles = styleModel.getStyles();
        IStyle styleByKey = StyleFactory.create(requiredStyleName);
        if (styles.contains(styleByKey)) {
            return styleByKey;
        }
        IStyle styleByTranslatedKey = StyleFactory.create(new TranslatedObject(requiredStyleName));
        if (styles.contains(styleByTranslatedKey)) {
            return styleByTranslatedKey;
        }
        for (IStyle style : styles) {
            if (style != null && requiredStyleName.equals(style.toString())) {
                return style;
            }
        }
        return null;
    }

    public static IStyle findStyleByNameOrThrow(MapModel map, String styleName) {
        IStyle style = findStyleByName(map, styleName);
        if (style == null) {
            throw new IllegalArgumentException("style '" + styleName + "' not found");
        }
        return style;
    }

    public static String toStyleName(IStyle style) {
        return style == null ? null : StyleTranslatedObject.toKeyString(style);
    }

    public static String toDisplayStyleName(IStyle style) {
        return style == null ? null : style.toString();
    }

    public static String readMainStyleName(NodeModel nodeModel) {
        if (nodeModel == null) {
            return null;
        }
        return toDisplayStyleName(LogicalStyleModel.getStyle(nodeModel));
    }

    public static List<String> readActiveStyleNames(NodeModel nodeModel) {
        if (nodeModel == null) {
            return Collections.emptyList();
        }
        Collection<IStyle> styles = LogicalStyleController.getController().getStyles(
            nodeModel, LogicalStyleController.StyleOption.STYLES_ONLY);
        List<String> activeStyleNames = new ArrayList<>(styles.size());
        for (IStyle style : styles) {
            String styleName = toDisplayStyleName(style);
            if (styleName != null && !styleName.trim().isEmpty()) {
                activeStyleNames.add(styleName);
            }
        }
        return Collections.unmodifiableList(activeStyleNames);
    }

    public static List<String> listAvailableStyleNames(MapModel mapModel) {
        Objects.requireNonNull(mapModel, "mapModel");
        MapStyleModel styleModel = MapStyleModel.getExtension(mapModel);
        List<IStyle> styles = styleModel.getNodeStyles();
        LinkedHashSet<String> styleNames = new LinkedHashSet<>();
        for (IStyle style : styles) {
            String styleName = toDisplayStyleName(style);
            if (styleName != null && !styleName.trim().isEmpty()) {
                styleNames.add(styleName);
            }
        }
        return new ArrayList<>(styleNames);
    }
}

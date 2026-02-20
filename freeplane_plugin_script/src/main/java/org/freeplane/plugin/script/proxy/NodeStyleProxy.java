/**
 *
 */
package org.freeplane.plugin.script.proxy;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

import org.freeplane.api.HorizontalTextAlignment;
import org.freeplane.api.LengthUnit;
import org.freeplane.api.MindMap;
import org.freeplane.api.Quantity;
import org.freeplane.core.util.ColorUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.nodestyle.NodeCss;
import org.freeplane.features.nodestyle.NodeSizeModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.NodeStyleModel;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.styles.IStyle;
import org.freeplane.features.styles.LogicalStyleController;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;
import org.freeplane.features.styles.LogicalStyleModel;
import org.freeplane.features.styles.MapStyleModel;
import org.freeplane.features.styles.StyleNameMappingHelper;
import org.freeplane.features.styles.StyleTranslatedObject;
import org.freeplane.features.styles.mindmapmode.MLogicalStyleController;
import org.freeplane.plugin.script.ScriptContext;
import org.freeplane.plugin.script.proxy.Proxy.Node;

class NodeStyleProxy extends AbstractProxy<NodeModel> implements Proxy.NodeStyle {
	static final String STYLE_NAME_MUST_NOT_BE_NULL = "styleName mustn't be null";

    static IStyle styleByName(MindMap map, String styleName) {
        return styleByName(((MapProxy)map).getDelegate(), styleName);
    }

    static IStyle styleByName(MapModel map, String styleName) {
        return styleName == null ? null : StyleNameMappingHelper.findStyleByName(map, styleName);
    }

	static IStyle styleByNameOrThrowException(MapModel map, String styleName) {
		return StyleNameMappingHelper.findStyleByNameOrThrow(
		    map, Objects.requireNonNull(styleName, STYLE_NAME_MUST_NOT_BE_NULL));
	}

	NodeStyleProxy(final NodeModel delegate, final ScriptContext scriptContext) {
		super(delegate, scriptContext);
	}

	public IStyle getStyle() {
		return LogicalStyleModel.getStyle(getDelegate());
	}

	public List<String> getAllActiveStyles() {
        return StyleNameMappingHelper.readActiveStyleNames(getDelegate());
    }

    public String getName() {
        return StyleNameMappingHelper.readMainStyleName(getDelegate());
    }

	public Node getStyleNode() {
		final NodeModel styleNode = MapStyleModel.getExtension(getDelegate().getMap()).getStyleNode(getStyle());
		return new NodeProxy(styleNode, getScriptContext());
	}

	public Color getBackgroundColor() {
		return getStyleController().getBackgroundColor(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	public String getBackgroundColorCode() {
		return ColorUtils.colorToString(getBackgroundColor());
	}

	public Proxy.Edge getEdge() {
		return new EdgeProxy(getDelegate(), getScriptContext());
	}

	public Proxy.Border getBorder(){
		return new BorderProxy(getDelegate(), getScriptContext());
	}

	public Proxy.Font getFont() {
		return new FontProxy(getDelegate(), getScriptContext());
	}

	public Color getTextColor() {
		return getStyleController().getColor(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	@Deprecated
	public Color getNodeTextColor() {
		return getTextColor();
	}

	public String getTextColorCode() {
		return ColorUtils.colorToString(getTextColor());
	}

    public boolean isFloating() {
        return hasStyle(getDelegate(), StyleTranslatedObject.toKeyString(MapStyleModel.FLOATING_STYLE));
    }

    public int getMinNodeWidth() {
        return getMinNodeWidthQuantity().toBaseUnitsRounded();
    }

	public Quantity<LengthUnit> getMinNodeWidthQuantity() {
		return getStyleController().getMinWidth(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

    public int getMaxNodeWidth() {
        return getMaxNodeWidthQuantity().toBaseUnitsRounded();
    }

	public Quantity<LengthUnit> getMaxNodeWidthQuantity() {
		return getStyleController().getMaxWidth(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
	}

	private MLogicalStyleController getLogicalStyleController() {
		return (MLogicalStyleController) LogicalStyleController.getController();
	}

	private MNodeStyleController getStyleController() {
		return (MNodeStyleController) NodeStyleController.getController();
	}

	public void setStyle(final IStyle key) {
		getLogicalStyleController().setStyle(getDelegate(), key);
	}

	public void setName(String styleName) {
		if (styleName == null) {
			setStyle(null);
		}
		else {
		    IStyle style = styleByName(getDelegate().getMap(), styleName);
		    if(style == null)
                throw new IllegalArgumentException("style '" + styleName + "' not found");
            else
                setStyle(style);
		}
	}

	public void setBackgroundColor(final Color color) {
		getStyleController().setBackgroundColor(getDelegate(), color);
	}

	public void setBackgroundColorCode(final String rgbString) {
		setBackgroundColor(ColorUtils.stringToColor(rgbString));
	}

	public void setTextColor(final Color color) {
		getStyleController().setColor(getDelegate(), color);
	}

	@Deprecated
	public void setNodeTextColor(final Color color) {
		setTextColor(color);
	}

	public void setTextColorCode(final String rgbString) {
		setTextColor(ColorUtils.stringToColor(rgbString));
	}

    public void setFloating(boolean floating) {
        if (floating) {
            setStyle(MapStyleModel.FLOATING_STYLE);
        }
        else if (MapStyleModel.FLOATING_STYLE.equals(getStyle())) {
            setStyle(null);
        }
    }

	public static boolean hasStyle(NodeModel nodeModel, String styleName) {
	    if (styleName == null) {
	        return false;
	    }
	    return StyleNameMappingHelper.readActiveStyleNames(nodeModel).contains(styleName);
    }

    public void setMinNodeWidth(int width) {
        Quantity<LengthUnit> quantity = inPixels(width);
		setMinNodeWidth(quantity);
    }

	public Quantity<LengthUnit> inPixels(int width) {
		Quantity<LengthUnit> quantity = width != -1 ? new Quantity<LengthUnit>(width, LengthUnit.px) : null;
		return quantity;
	}

	public void setMinNodeWidth(Quantity<LengthUnit> width) {
		getStyleController().setMinNodeWidth(getDelegate(), width);
	}

	public void setMinNodeWidth(String width) {
		getStyleController().setMinNodeWidth(getDelegate(), Quantity.fromString(width, LengthUnit.px));
	}

    public void setMaxNodeWidth(int width) {
        Quantity<LengthUnit> quantity = inPixels(width);
		setMaxNodeWidth(quantity);
    }

	public void setMaxNodeWidth(Quantity<LengthUnit> width) {
		getStyleController().setMaxNodeWidth(getDelegate(), width);
	}

	public void setMaxNodeWidth(String width) {
		getStyleController().setMaxNodeWidth(getDelegate(), Quantity.fromString(width, LengthUnit.px));
	}

    public boolean isNumberingEnabled() {
        return NodeStyleModel.getNodeNumbering(getDelegate());
    }

    public void setNumberingEnabled(boolean enabled) {
        getStyleController().setNodeNumbering(getDelegate(), enabled);
    }

	@Override
	public boolean isNumbering() {
		return Boolean.TRUE.equals(NodeStyleModel.getNodeNumbering(getDelegate()));
	}

	@Override
	public void setNumbering(Boolean enabled) {
		getStyleController().setNodeNumbering(getDelegate(), enabled);
	}

	@Override
	public boolean isNumberingSet() {
		return NodeStyleModel.getNodeNumbering(getDelegate()) != null;
	}

	@Override
	public void setCss(String css) {
		getStyleController().setStyleSheet(getDelegate(), css);
	}

	@Override
	public String getCss() {
		String css = getStyleController().getStyleSheet(getDelegate(), StyleOption.FOR_UNSELECTED_NODE).css;
		return css;
	}

	@Override
	public boolean isCssSet() {
		return getDelegate().getExtension(NodeCss.class) != null;
	}

	@Override
	public boolean isBackgroundColorSet() {
		return NodeStyleModel.getBackgroundColor(getDelegate()) != null;
	}

	@Override
	public boolean isTextColorSet() {
		return NodeStyleModel.getColor(getDelegate()) != null;
	}

	@Override
	public boolean isMinNodeWidthSet() {
		return NodeSizeModel.getMinNodeWidth(getDelegate()) != null;
	}

    @Override
    public boolean isMaxNodeWidthSet() {
        return NodeSizeModel.getMaxNodeWidth(getDelegate()) != null;
    }

    @Override
    public boolean isHorizontalTextAlignmentSet() {
        return NodeStyleModel.getHorizontalTextAlignment(getDelegate()) != null;
    }

    @Override
    public HorizontalTextAlignment getHorizontalTextAlignment() {
        return getStyleController().getHorizontalTextAlignment(getDelegate(), StyleOption.FOR_UNSELECTED_NODE);
    }

    @Override
    public void setHorizontalTextAlignment(HorizontalTextAlignment alignment) {
        getStyleController().setHorizontalTextAlignment(getDelegate(), alignment);
    }
}

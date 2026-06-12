package org.freeplane.view.swing.map.cloud;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.freeplane.features.cloud.CloudModel;
import org.freeplane.view.swing.map.NodeView;

public class RectangleCloudView extends CloudView {

	private final boolean isRound;
	RectangleCloudView(CloudModel cloudModel, NodeView source, boolean isRound) {
	    super(cloudModel, source);
	    this.isRound = isRound;
	}

	@Override
    protected void fillPolygon(Polygon p, Graphics2D g) {
    }

	@Override
    protected void paintDecoration(Graphics2D g, Graphics2D gstroke) {
	    final Rectangle bounds = getRectangle();
	    final int distanceToConvexHull = (int) getDistanceToConvexHull();
	    if(isRound){
	        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height,
	        		distanceToConvexHull, distanceToConvexHull);
			gstroke.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height,
					distanceToConvexHull, distanceToConvexHull);
		}
		else{
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			gstroke.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}
    }

	@Override
	protected Rectangle getPaintingBounds() {
		return getRectangle();
	}

	private Rectangle getRectangle() {
	    final int distanceToConvexHull = (int) getDistanceToConvexHull();
	    NodeView parentView = source.getParentView();
	    final boolean expandHorizontally = parentView == null
	    		|| ! parentView.usesHorizontalLayout()
	    		|| ! source.usesHorizontalLayout();
	    final boolean expandVertically = parentView == null
	    		|| parentView.usesHorizontalLayout()
	    		|| source.usesHorizontalLayout();
	    return RectangleCloudBounds.fromContents(getVisibleContentBounds(), distanceToConvexHull,
	    		expandHorizontally, expandVertically);
	}

	@Override
    protected void paintDecoration(Graphics2D g, Graphics2D gstroke, double x0, double y0, double x1, double y1,
                                   double dx, double dy, double dxn, double dyn) {	    
    }

    @Override
    protected double getDistanceToConvexHull() {
        return 0.5 * super.getDistanceToConvexHull();
    }

	private List<Rectangle> getVisibleContentBounds() {
		final ArrayList<Rectangle> contents = new ArrayList<Rectangle>();
		addVisibleContentBounds(source, 0, 0, contents);
		return contents;
	}

	private void addVisibleContentBounds(NodeView nodeView, int x, int y, List<Rectangle> contents) {
		if (!nodeView.isVisible()) {
			return;
		}
		if (nodeView.isContentVisible()) {
			final JComponent content = nodeView.getContent();
			contents.add(new Rectangle(x + content.getX(), y + content.getY(),
					content.getWidth(), content.getHeight()));
		}
		for (NodeView child : nodeView.getChildrenViews()) {
			addVisibleContentBounds(child, x + child.getX(), y + child.getY(), contents);
		}
	}
}

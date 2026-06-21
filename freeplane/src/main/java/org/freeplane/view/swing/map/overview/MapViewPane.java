package org.freeplane.view.swing.map.overview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.bookmarks.mindmapmode.FocusBookmarkToolbarAction;

import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.styles.MapStyle;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.ui.ViewController;
import org.freeplane.view.swing.map.MapBackgroundVideo;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.MapViewScrollPane;
import org.freeplane.view.swing.map.overview.resizable.ResizablePanelBorder;
import org.freeplane.view.swing.map.overview.resizable.ResizePanelMouseHandler;

public class MapViewPane extends JPanel implements IFreeplanePropertyListener, IMapChangeListener, IMapViewChangeListener {
    private static final long serialVersionUID = 8664710783654626093L;

    private final static String MAP_OVERVIEW_VISIBLE_PROPERTY = "mapOverviewVisible";
    private final static String MAP_OVERVIEW_VISIBLE_FS_PROPERTY = "mapOverviewVisible.fullscreen";

    private final static String BOOKMARKS_TOOLBAR_VISIBLE_PROPERTY = "bookmarksToolbarVisible";
    private final static String BOOKMARKS_TOOLBAR_VISIBLE_FS_PROPERTY = "bookmarksToolbarVisible.fullscreen";
    private final static String BACKGROUND_VIDEO_FRONT_TEST_PROPERTY = "org.freeplane.backgroundVideo.frontTest";
    private final static String BACKGROUND_VIDEO_SNAPSHOT_TEST_PROPERTY = "org.freeplane.backgroundVideo.snapshotTest";

    private final static String MAP_OVERVIEW_PROPERTY_PREFIX = "map_overview_";
    private final static String MAP_OVERVIEW_ATTACH_POINT_PROPERTY = MAP_OVERVIEW_PROPERTY_PREFIX + "attach_point";
    private final static String MAP_OVERVIEW_BOUNDS_PROPERTY = MAP_OVERVIEW_PROPERTY_PREFIX + "bounds";

    public final static int MAP_OVERVIEW_DEFAULT_SIZE = new Quantity<LengthUnit>(240, LengthUnit.pt)
            .toBaseUnitsRounded();
    public final static int MAP_OVERVIEW_MIN_SIZE = new Quantity<LengthUnit>(60, LengthUnit.pt).toBaseUnitsRounded();
    public final static int MAP_OVERVIEW_MAX_SIZE = new Quantity<LengthUnit>(480, LengthUnit.pt).toBaseUnitsRounded();

    public static final int MAP_OVERVIEW_BORDER_SIZE = new Quantity<LengthUnit>(3, LengthUnit.pt).toBaseUnitsRounded();
    public static final int MAP_OVERVIEW_BORDER_EXTENDED_SIZE = MAP_OVERVIEW_BORDER_SIZE
            + new Quantity<LengthUnit>(24, LengthUnit.pt).toBaseUnitsRounded();

    private final JScrollPane mapViewScrollPane;
    private final JPanel backgroundVideoPanel;
    private final JPanel mapOverviewPanel;
    private final boolean backgroundVideoFrontTest;
    private final boolean backgroundVideoSnapshotTest;
    private final Map<JComponent, Boolean> backgroundVideoAncestorOpacity = new IdentityHashMap<>();
    private boolean isMapOverviewVisible;
    private final MapOverviewImage mapOverviewImage;
    private MapBackgroundVideoPlayer backgroundVideo;
    private URI backgroundVideoUri;
    private Window backgroundVideoTransparentWindow;
    private Color backgroundVideoWindowBackground;
    private Object backgroundVideoWindowPeer;
    private Object backgroundVideoPlatformWindow;
    private Object backgroundVideoContentView;

	private final MapView mapView;

    public MapViewPane(JScrollPane mapViewScrollPane) {
        this.mapViewScrollPane = mapViewScrollPane;
        this.mapView = (MapView) mapViewScrollPane.getViewport().getView();
        backgroundVideoFrontTest = Boolean.getBoolean(BACKGROUND_VIDEO_FRONT_TEST_PROPERTY);
        backgroundVideoSnapshotTest = Boolean.getBoolean(BACKGROUND_VIDEO_SNAPSHOT_TEST_PROPERTY);
        setLayout(new BorderLayout(0, 0) {
            private static final long serialVersionUID = 3702408082745761647L;

            @Override
			public void addLayoutComponent(Component comp, Object constraints) {
            	if(constraints != null)
            		super.addLayoutComponent(comp, constraints);
			}

			@Override
			public void layoutContainer(Container parent) {
            	super.layoutContainer(parent);
                mapViewScrollPane.validate();
                updateBackgroundVideoBounds();
                mapOverviewPanel.setBounds(calculateMapOverviewBounds());
            }
        });
        backgroundVideoPanel = new JPanel(new BorderLayout(0, 0));
        backgroundVideoPanel.setOpaque(false);
        backgroundVideoPanel.setVisible(false);
        mapOverviewImage = new MapOverviewImage(mapView, mapViewScrollPane);
        mapOverviewPanel = new JPanel(new BorderLayout(0, 0)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void updateUI() {
                super.updateUI();
                MouseInputListener handler = new ResizePanelMouseHandler();
                addMouseListener(handler);
                addMouseMotionListener(handler);
                setBorder(new ResizablePanelBorder(MAP_OVERVIEW_BORDER_SIZE, MAP_OVERVIEW_BORDER_EXTENDED_SIZE));
            }
        };
        mapOverviewPanel.add(mapOverviewImage);
        final ViewController viewController = Controller.getCurrentController().getViewController();
        isMapOverviewVisible = viewController.isMapOverviewVisible();
        mapOverviewPanel.setVisible(isMapOverviewVisible);
        add(backgroundVideoPanel);
        add(mapOverviewPanel);
        add(mapViewScrollPane, BorderLayout.CENTER);
        setBackgroundVideoZOrder();

        mapView.addComponentListener(new ComponentAdapter() {
            @Override
			public void componentResized(ComponentEvent e) {
                updateMapOverview();
                updateBackgroundSnapshot();
            }
        });
        addHierarchyListener(e -> {
            if((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
                updateBackgroundVideo();
        });

        mapView.addPropertyChangeListener(FocusBookmarkToolbarAction.BOOKMARK_TOOLBAR_FOCUS_PROPERTY, ev -> {
        	if(ev.getNewValue() != null) {
        		mapView.putClientProperty(FocusBookmarkToolbarAction.BOOKMARK_TOOLBAR_FOCUS_PROPERTY, null);
        		final ViewController controller = Controller.getCurrentController().getViewController();
        		if(! controller.isBookmarksToolbarVisible()) {
        			controller.setBookmarksToolbarVisible(true);
        		}
        		
        		BookmarkToolbarPane bookmarkToolbarPane = (BookmarkToolbarPane) SwingUtilities.getAncestorOfClass(BookmarkToolbarPane.class, mapView);
        		if (bookmarkToolbarPane != null) {
        			bookmarkToolbarPane.requestFocusForBookmarkToolbar();
        		}
        	}
        });
    }

	@Override
    public void mapChanged(MapChangeEvent event) {
		final Object property = event.getProperty();
		boolean updatesBackgroundVideo = property.equals(MapView.class);
		if(property.equals(MapView.class)) {
			if (event.getOldValue() == mapView) {
				event.getMap().removeMapChangeListener(this);
				final MapModel map = mapView.getMap();
				map.addMapChangeListener(this);
			}
			else
				return;
		}
		updatesBackgroundVideo = updatesBackgroundVideo
				|| property.equals(MapStyle.RESOURCES_BACKGROUND_IMAGE)
				|| property.equals(MapStyle.BACKGROUND_IMAGE_ENABLED);
		if(updatesBackgroundVideo)
			updateBackgroundVideo();
		updateMapOverview();
	}

	private void updateMapOverview() {
        if (mapOverviewPanel.isVisible()) {
            mapOverviewImage.resetImage();
            SwingUtilities.invokeLater(mapOverviewPanel::repaint);
        }
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false; // enable overlap
    }

    @Override
    public void addNotify() {
        super.addNotify();
        mapView.getMap().addMapChangeListener(this);
        ResourceController.getResourceController().addPropertyChangeListener(this);
        Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(this);
        updateBackgroundVideo();
    }

    @Override
    public void removeNotify() {
        mapView.getMap().removeMapChangeListener(this);
        ResourceController.getResourceController().removePropertyChangeListener(this);
        Controller.getCurrentController().getMapViewManager().removeMapViewChangeListener(this);
        clearBackgroundVideo();
        super.removeNotify();
    }

    @Override
    public void propertyChanged(String propertyName, String newValue, String oldValue) {
        if (ViewController.FULLSCREEN_ENABLED_PROPERTY.equals(propertyName)
                || MAP_OVERVIEW_VISIBLE_PROPERTY.equals(propertyName)
                || MAP_OVERVIEW_VISIBLE_FS_PROPERTY.equals(propertyName)) {
            final ViewController viewController = Controller.getCurrentController().getViewController();
            if (isMapOverviewVisible != viewController.isMapOverviewVisible()) {
                isMapOverviewVisible = ! isMapOverviewVisible;
                mapOverviewPanel.setVisible(isMapOverviewVisible);
                updateMapOverview();
            }
        }
        if (propertyName.startsWith(MAP_OVERVIEW_PROPERTY_PREFIX)) {
            if (MAP_OVERVIEW_ATTACH_POINT_PROPERTY.equals(propertyName)) {
                Rectangle mapOverviewBounds = mapOverviewPanel.getBounds();
                convertOriginByAttachPoint(mapOverviewBounds);
                mapOverviewBounds.setLocation(0, 0);
                setMapOverviewBounds(mapOverviewBounds, true);
            }
            revalidate();
            updateMapOverview();
        }
        if (MapBackgroundVideo.FOREGROUND_OPACITY_PROPERTY.equals(propertyName)) {
            if(backgroundVideo != null)
                backgroundVideo.updateForegroundOpacity();
        }
    }

    @Override
    public void afterViewChange(final Component oldView, final Component newView) {
        if(oldView == mapView || newView == mapView)
            updateBackgroundVideo();
    }

    @Override
    public void afterViewDisplayed(final Component oldView, final Component newView) {
        afterViewChange(oldView, newView);
    }

    public MapOverviewAttachPoint getMapOverviewAttachPoint() {
        ResourceController resourceController = ResourceController.getResourceController();
        String rawAttachPoint = resourceController.getProperty(MAP_OVERVIEW_ATTACH_POINT_PROPERTY);
        if (rawAttachPoint == null) {
            return MapOverviewAttachPoint.SOUTH_EAST;
        }
        try {
            return MapOverviewAttachPoint.valueOf(rawAttachPoint);
        } catch (IllegalArgumentException e) {
            return MapOverviewAttachPoint.SOUTH_EAST;
        }
    }

    public Color getMapViewBackground() {
        return mapView.getBackground();
    }

    public void updateMapOverviewAttachPoint(MapOverviewAttachPoint attachPoint) {
        Rectangle oldBounds = mapOverviewPanel.getBounds();
        Rectangle oldAttachBounds = new Rectangle(oldBounds);
        convertOriginByAttachPoint(oldAttachBounds);

        MapOverviewAttachPoint oldAttachPoint = getMapOverviewAttachPoint();
        if (attachPoint != oldAttachPoint) {
            ResourceController.getResourceController().setProperty(MAP_OVERVIEW_ATTACH_POINT_PROPERTY,
                    attachPoint.name());
        } else if (oldAttachBounds.x == 0 && oldAttachBounds.y == 0) {
            oldAttachBounds.setSize(MAP_OVERVIEW_DEFAULT_SIZE, MAP_OVERVIEW_DEFAULT_SIZE);
        }
        oldAttachBounds.setLocation(0, 0);
        setMapOverviewBounds(oldAttachBounds, true);
    }

    private Rectangle calculateMapOverviewBounds() {
        ResourceController resourceController = ResourceController.getResourceController();
        String rawBoundsValue = resourceController.getProperty(MAP_OVERVIEW_BOUNDS_PROPERTY);
        int[] elements = null;
        if (rawBoundsValue != null) {
            String[] rawElements = rawBoundsValue.split(",");
            if (rawElements.length == 4) {
                elements = new int[4];
                for (int i = 0; i < 4; i++) {
                    try {
                        elements[i] = Integer.parseInt(rawElements[i]);
                    } catch (NumberFormatException e) {
                        elements = null;
                        break;
                    }
                }
            }
        }

        if (elements == null) {
            elements = new int[] { 0, 0, MAP_OVERVIEW_DEFAULT_SIZE, MAP_OVERVIEW_DEFAULT_SIZE };
        }
        int x = elements[0];
        int y = elements[1];
        int width = getBoundedValue(elements[2], MAP_OVERVIEW_MIN_SIZE, MAP_OVERVIEW_MAX_SIZE);
        int height = getBoundedValue(elements[3], MAP_OVERVIEW_MIN_SIZE, MAP_OVERVIEW_MAX_SIZE);
        Rectangle bounds = new Rectangle(x, y, width, height);
        convertOriginByAttachPoint(bounds);
        return bounds;
    }

    private int getBoundedValue(int value, int min, int max) {
        return Math.max(Math.min(value, max), min);
    }

    public void setMapOverviewBounds(Rectangle bounds) {
        setMapOverviewBounds(bounds, false);
    }

    private void setMapOverviewBounds(Rectangle bounds, boolean isConverted) {
        if (! isConverted) {
            convertOriginByAttachPoint(bounds);
        }
        ResourceController.getResourceController().setProperty(MAP_OVERVIEW_BOUNDS_PROPERTY,
                String.format("%s,%s,%s,%s", bounds.x, bounds.y, bounds.width, bounds.height));
    }

    private void convertOriginByAttachPoint(Rectangle bounds) {
        final JViewport viewPort = mapViewScrollPane.getViewport();
        int bottom = viewPort.getHeight();
        int right = viewPort.getWidth();
        Point location;
        switch (getMapOverviewAttachPoint()) {
        case SOUTH_EAST:
        	location = new Point(right - bounds.x - bounds.width, bottom - bounds.y - bounds.height);
            break;
        case SOUTH_WEST:
        	location = new Point(bounds.x, bottom - bounds.y  - bounds.height);
            break;
        case NORTH_EAST:
        	location = new Point(right - bounds.x  - bounds.width, bounds.y);
            break;
        case NORTH_WEST:
        	location = new Point(0, 0);
            break;
        default:
        	throw new RuntimeException("All cases handled above");
        }
        UITools.convertPointToAncestor(viewPort, location, JScrollPane.class);
        bounds.setLocation(location);
    }

	public Rectangle getMapOverviewReservedArea() {
		if (isMapOverviewVisible) {
			return mapOverviewPanel.getBounds();
		} else
			return MapViewScrollPane.EMPTY_RECTANGLE;
	}

	public JScrollPane getMapViewScrollPane() {
		return mapViewScrollPane;
	}

	private void updateBackgroundVideo() {
		final URI uri = isBackgroundVideoActive() ? backgroundVideoUri() : null;
		if(uri == null) {
			clearBackgroundVideo();
			return;
		}
		if(uri.equals(backgroundVideoUri) && backgroundVideo != null) {
			updateBackgroundVideoVisibility();
			return;
		}
		clearBackgroundVideo();
		backgroundVideoUri = uri;
		backgroundVideo = MapBackgroundVideoPlayers.create(backgroundVideoPanel, uri,
				this::updateBackgroundVideoVisibility, () -> useJavaFxBackgroundVideo(uri), () -> clearBackgroundVideo(uri));
		backgroundVideoPanel.add(backgroundVideo.component(), BorderLayout.CENTER);
		updateBackgroundVideoBounds();
		backgroundVideoPanel.revalidate();
	}

	private URI backgroundVideoUri() {
		final MapStyle mapStyle = mapView.getModeController().getExtension(MapStyle.class);
		final MapModel map = mapView.getMap();
		final String backgroundImageEnabledAsString = mapStyle.getPropertySetDefault(map, MapStyle.BACKGROUND_IMAGE_ENABLED);
		if(! Boolean.parseBoolean(backgroundImageEnabledAsString))
			return null;
		final URI uri = mapStyle.getBackgroundImage(map);
		return MapBackgroundVideo.isSupportedVideoUri(uri) ? uri : null;
	}

	private boolean isBackgroundVideoActive() {
		return isShowing() && mapView.isSelected();
	}

	private void updateBackgroundVideoVisibility() {
		final boolean visible = backgroundVideo != null && backgroundVideo.isReady() && isBackgroundVideoActive();
		final boolean videoInFront = visible && (backgroundVideoFrontTest || backgroundVideo.isPaintedInFront());
		final boolean snapshotVisible = visible && ! videoInFront && backgroundVideoSnapshotTest;
		updateStageBehindTransparency(visible && backgroundVideo.requiresHostWindowTransparency());
		setOpaque(! visible || videoInFront);
		backgroundVideoPanel.setOpaque(videoInFront || snapshotVisible);
		backgroundVideoPanel.setVisible(visible);
		mapViewScrollPane.setOpaque(! visible || videoInFront || snapshotVisible);
		mapViewScrollPane.getViewport().setOpaque(! visible || videoInFront || snapshotVisible);
		mapView.setExternalBackgroundPainted(visible && ! videoInFront && ! snapshotVisible);
		if(snapshotVisible)
			updateBackgroundSnapshot();
		else if(backgroundVideo != null)
			backgroundVideo.setOverlay(null);
		if(backgroundVideo != null) {
			if(visible)
				backgroundVideo.play();
			else
				backgroundVideo.pause();
		}
		repaint();
	}

    private void setBackgroundVideoZOrder() {
        setComponentZOrder(mapOverviewPanel, 0);
        if(backgroundVideoFrontTest || backgroundVideoSnapshotTest) {
            setComponentZOrder(backgroundVideoPanel, 1);
            setComponentZOrder(mapViewScrollPane, 2);
        }
        else {
            setComponentZOrder(mapViewScrollPane, 1);
            setComponentZOrder(backgroundVideoPanel, 2);
        }
    }

	private void clearBackgroundVideo(final URI uri) {
		if(uri.equals(backgroundVideoUri))
			clearBackgroundVideo();
	}

	private void updateStageBehindTransparency(final boolean transparent) {
		if(transparent)
			installStageBehindTransparency();
		else
			restoreStageBehindTransparency();
	}

	private void installStageBehindTransparency() {
		final Window window = SwingUtilities.getWindowAncestor(this);
		if(window == null)
			return;
		if(backgroundVideoTransparentWindow != window) {
			restoreStageBehindTransparency();
			backgroundVideoTransparentWindow = window;
			backgroundVideoWindowBackground = window.getBackground();
		}
		if(! makeWindowTransparent(window))
			return;
		Component component = this;
		while((component = component.getParent()) != null && ! (component instanceof Window)) {
			if(component instanceof JComponent) {
				final JComponent parent = (JComponent) component;
				if(! backgroundVideoAncestorOpacity.containsKey(parent))
					backgroundVideoAncestorOpacity.put(parent, parent.isOpaque());
				parent.setOpaque(false);
			}
		}
		window.repaint();
	}

	private void restoreStageBehindTransparency() {
		for(final Map.Entry<JComponent, Boolean> entry : backgroundVideoAncestorOpacity.entrySet())
			entry.getKey().setOpaque(entry.getValue());
		backgroundVideoAncestorOpacity.clear();
		restoreMacWindowOpacity();
		if(backgroundVideoTransparentWindow != null && backgroundVideoWindowBackground != null) {
			try {
				backgroundVideoTransparentWindow.setBackground(backgroundVideoWindowBackground);
			}
			catch (final RuntimeException e) {
				LogUtils.warn(e);
			}
			backgroundVideoTransparentWindow.repaint();
		}
		backgroundVideoTransparentWindow = null;
		backgroundVideoWindowBackground = null;
	}

	private boolean makeWindowTransparent(final Window window) {
		if(System.getProperty("os.name", "").startsWith("Mac") && makeMacWindowTransparent(window))
			return true;
		try {
			window.setBackground(new Color(0, 0, 0, 0));
			return true;
		}
		catch (final RuntimeException e) {
			LogUtils.warn(e);
			return false;
		}
	}

	private boolean makeMacWindowTransparent(final Window window) {
		try {
			final Object[] macWindow = macWindow(window);
			backgroundVideoWindowPeer = macWindow[0];
			backgroundVideoPlatformWindow = macWindow[1];
			backgroundVideoContentView = macWindow[2];
			invoke(backgroundVideoWindowPeer.getClass(), backgroundVideoWindowPeer, "setOpaque",
					boolean.class, false);
			invoke(backgroundVideoWindowPeer.getClass(), backgroundVideoWindowPeer, "setBackground",
					Color.class, new Color(0, 0, 0, 0));
			invoke(backgroundVideoPlatformWindow.getClass(), backgroundVideoPlatformWindow, "setOpaque",
					boolean.class, false);
			invoke(backgroundVideoContentView.getClass(), backgroundVideoContentView, "setWindowLayerOpaque",
					boolean.class, false);
			setTransparentWindowBackground(window);
			return true;
		}
		catch (final Exception e) {
			LogUtils.warn(e);
			return false;
		}
	}

	private void setTransparentWindowBackground(final Window window) {
		if(window instanceof Frame && ! ((Frame) window).isUndecorated())
			return;
		if(window instanceof Dialog && ! ((Dialog) window).isUndecorated())
			return;
		try {
			window.setBackground(new Color(0, 0, 0, 0));
		}
		catch (final RuntimeException e) {
			LogUtils.warn(e);
		}
	}

	private void restoreMacWindowOpacity() {
		try {
			if(backgroundVideoContentView != null)
				invoke(backgroundVideoContentView.getClass(), backgroundVideoContentView, "setWindowLayerOpaque",
						boolean.class, true);
			if(backgroundVideoPlatformWindow != null)
				invoke(backgroundVideoPlatformWindow.getClass(), backgroundVideoPlatformWindow, "setOpaque",
						boolean.class, true);
			if(backgroundVideoWindowPeer != null) {
				if(backgroundVideoWindowBackground != null)
					invoke(backgroundVideoWindowPeer.getClass(), backgroundVideoWindowPeer, "setBackground",
							Color.class, backgroundVideoWindowBackground);
				invoke(backgroundVideoWindowPeer.getClass(), backgroundVideoWindowPeer, "setOpaque",
						boolean.class, true);
			}
		}
		catch (final Exception e) {
			LogUtils.warn(e);
		}
		finally {
			backgroundVideoWindowPeer = null;
			backgroundVideoContentView = null;
			backgroundVideoPlatformWindow = null;
		}
	}

	private Object[] macWindow(final Window window) throws Exception {
		final Class<?> awtAccessor = Class.forName("sun.awt.AWTAccessor");
		final Object componentAccessor = invoke(awtAccessor, null, "getComponentAccessor");
		final Class<?> componentAccessorInterface = Class.forName("sun.awt.AWTAccessor$ComponentAccessor");
		final Object peer = invoke(componentAccessorInterface, componentAccessor, "getPeer",
				java.awt.Component.class, window);
		if(peer == null)
			throw new IllegalStateException("no AWT peer");
		final Object platformWindow = invoke(peer.getClass(), peer, "getPlatformWindow");
		final Object contentView = invoke(platformWindow.getClass(), platformWindow, "getContentView");
		return new Object[] { peer, platformWindow, contentView };
	}

	private Object invoke(final Class<?> type, final Object target, final String name) throws Exception {
		final Method method = type.getMethod(name);
		method.setAccessible(true);
		return method.invoke(target);
	}

	private Object invoke(final Class<?> type, final Object target, final String name,
						  final Class<?> argumentType, final Object argument) throws Exception {
		final Method method = type.getMethod(name, argumentType);
		method.setAccessible(true);
		return method.invoke(target, argument);
	}

	private void useJavaFxBackgroundVideo(final URI uri) {
		if(! uri.equals(backgroundVideoUri))
			return;
		final MapBackgroundVideoPlayer video = backgroundVideo;
		backgroundVideo = MapBackgroundVideoPlayers.createJavaFx(uri,
				this::updateBackgroundVideoVisibility, () -> clearBackgroundVideo(uri));
		backgroundVideoPanel.removeAll();
		backgroundVideoPanel.add(backgroundVideo.component(), BorderLayout.CENTER);
		if(video != null)
			video.dispose();
		updateBackgroundVideoBounds();
		backgroundVideoPanel.revalidate();
	}

	private void clearBackgroundVideo() {
		final MapBackgroundVideoPlayer video = backgroundVideo;
		backgroundVideo = null;
		backgroundVideoUri = null;
		backgroundVideoPanel.removeAll();
		if(video != null)
			video.dispose();
		updateBackgroundVideoVisibility();
		backgroundVideoPanel.revalidate();
		backgroundVideoPanel.repaint();
	}

	private void updateBackgroundVideoBounds() {
		final Rectangle bounds = SwingUtilities.convertRectangle(mapViewScrollPane,
				mapViewScrollPane.getViewportBorderBounds(), this);
		backgroundVideoPanel.setBounds(bounds);
		backgroundVideoPanel.doLayout();
		updateBackgroundSnapshot();
	}

	private void updateBackgroundSnapshot() {
		if(backgroundVideo == null || ! backgroundVideo.isReady() || ! isShowing()
				|| backgroundVideoFrontTest || ! backgroundVideoSnapshotTest)
			return;
		final JViewport viewport = mapViewScrollPane.getViewport();
		final Rectangle viewRect = viewport.getViewRect();
		if(viewRect.width <= 0 || viewRect.height <= 0)
			return;
		final BufferedImage snapshot = new BufferedImage(viewRect.width, viewRect.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = snapshot.createGraphics();
		try {
			graphics.translate(-viewRect.x, -viewRect.y);
			mapView.paintWithoutBackground(graphics);
		}
		finally {
			graphics.dispose();
		}
		backgroundVideo.setOverlay(snapshot);
	}

}

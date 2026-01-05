package org.freeplane.plugin.ai;

import java.net.URL;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Properties;

import javax.swing.JTabbedPane;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.application.CommandLineOptions;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.freeplane.plugin.ai.chat.AIChatPanel;
import org.freeplane.plugin.ai.chat.SystemMessageBuilder;
import org.freeplane.plugin.ai.mcpserver.ModelContextProtocolServer;
import org.freeplane.plugin.ai.tools.AIToolSet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static final String PREFERENCES_RESOURCE = "preferences.xml";
	private static final String SYSTEM_MESSAGE_PROPERTY = SystemMessageBuilder.SYSTEM_MESSAGE_PROPERTY;
	private static final String SYSTEM_MESSAGE_DEFAULT_TEXT_KEY = "ai_system_message_default";
	private ModelContextProtocolServer modelContextProtocolServer;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		registerMindMapModeExtension(context);
	}

	private void registerMindMapModeExtension(final BundleContext context) {
		final Hashtable<String, String[]> properties = new Hashtable<String, String[]>();
		properties.put("mode", new String[] { MModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(),
		    new IModeControllerExtensionProvider() {
			    @Override
				public void installExtension(final ModeController modeController, CommandLineOptions options) {
				    final JTabbedPane tabs = UITools.getFreeplaneTabbedPanel();
				    tabs.addTab("AI", new AIChatPanel());
				    addPluginDefaults();
				    startModelContextProtocolServer();
				    addPreferencesToOptionPanel();
				}

				private void addPreferencesToOptionPanel() {
					final URL preferences = this.getClass().getResource(PREFERENCES_RESOURCE);
					if (preferences == null)
						throw new RuntimeException("cannot open preferences");
					final Controller controller = Controller.getCurrentController();
					MModeController modeController = (MModeController) controller.getModeController();
					modeController.getOptionPanelBuilder().load(preferences);
				}

				private void addPluginDefaults() {
					final URL defaults = this.getClass().getResource("defaults.properties");
					Objects.requireNonNull(defaults, "cannot open defaults");
					Properties properties = new Properties();
					ResourceController.loadProperties(properties, defaults);
					ResourceController resourceController = ResourceController.getResourceController();
					resourceController.addDefaults(properties);
					properties.keySet().forEach(key -> resourceController.securePropertyForReadingAndModification((String) key));
					setSystemMessageDefault(resourceController);
				}

				private void setSystemMessageDefault(ResourceController resourceController) {
					String defaultMessage = TextUtils.getRawText(SYSTEM_MESSAGE_DEFAULT_TEXT_KEY, "");
					defaultMessage = TextUtils.removeTranslateComment(defaultMessage);
					resourceController.setDefaultProperty(SYSTEM_MESSAGE_PROPERTY, defaultMessage);
					resourceController.securePropertyForReadingAndModification(SYSTEM_MESSAGE_PROPERTY);
				}

				private void startModelContextProtocolServer() {
					if (modelContextProtocolServer == null) {
						modelContextProtocolServer = new ModelContextProtocolServer(new AIToolSet());
						ResourceController.getResourceController()
							.addPropertyChangeListenerAndPropagate(modelContextProtocolServer);
					}
				}
		    }, properties);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		if (modelContextProtocolServer != null) {
			modelContextProtocolServer.stop();
		}
	}
}

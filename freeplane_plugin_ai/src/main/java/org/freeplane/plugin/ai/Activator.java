package org.freeplane.plugin.ai;

import java.net.URL;
import java.util.Hashtable;

import org.freeplane.features.format.ContentTypeFormat;
import org.freeplane.features.format.FormatController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.note.NoteController;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.ConditionalContentTransformer;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.main.application.CommandLineOptions;
import org.freeplane.main.mindmapmode.stylemode.SModeController;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static final String PREFERENCES_RESOURCE = "preferences.xml";
	static final String TOGGLE_PARSE_LATEX = "parse_latex";

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		registerMindMapModeExtension(context);
	}

	private void registerMindMapModeExtension(final BundleContext context) {
		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
		props.put("mode", new String[] { MModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(),
		    new IModeControllerExtensionProvider() {
			    @Override
				public void installExtension(final ModeController modeController, CommandLineOptions options) {

		    }, props);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
	}
}

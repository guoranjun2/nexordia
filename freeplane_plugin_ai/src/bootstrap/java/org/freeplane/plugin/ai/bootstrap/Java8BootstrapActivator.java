package org.freeplane.plugin.ai.bootstrap;

import java.awt.BorderLayout;
import java.text.MessageFormat;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.application.CommandLineOptions;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Java8BootstrapActivator implements BundleActivator {

    static final int MINIMUM_SUPPORTED_JAVA_MAJOR_VERSION = 17;
    static final String RUNTIME_ACTIVATOR_CLASS_NAME = "org.freeplane.plugin.ai.Activator";
    private static final String RUNTIME_VERSION_TOO_LOW_MESSAGE_KEY =
        "ai_bootstrap_runtime_version_too_low";
    private static final String RUNTIME_ACTIVATION_FAILED_MESSAGE_KEY =
        "ai_bootstrap_runtime_activation_failed";

    private BundleActivator runtimeActivator;
    private ServiceRegistration<?> incompatibleModeExtensionRegistration;
    private BootstrapMessageProvider bootstrapMessageProvider;

    @Override
    public void start(BundleContext context) throws Exception {
        bootstrapMessageProvider = createBootstrapMessageProvider();
        int runtimeJavaMajorVersion = resolveRuntimeJavaMajorVersion();
        if (runtimeJavaMajorVersion < MINIMUM_SUPPORTED_JAVA_MAJOR_VERSION) {
            String message = bootstrapMessageProvider.runtimeVersionTooLow(
                runtimeJavaMajorVersionString(runtimeJavaMajorVersion));
            LogUtils.info(message);
            registerIncompatibleModeExtension(context, message);
            return;
        }
        try {
            BundleActivator delegate = instantiateRuntimeActivator();
            delegate.start(context);
            runtimeActivator = delegate;
        }
        catch (Exception failure) {
            LogUtils.severe("AI runtime activator failed to start", failure);
            String message = bootstrapMessageProvider.runtimeActivationFailed(
                runtimeJavaMajorVersionString(runtimeJavaMajorVersion));
            registerIncompatibleModeExtension(context, message);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (incompatibleModeExtensionRegistration != null) {
            incompatibleModeExtensionRegistration.unregister();
            incompatibleModeExtensionRegistration = null;
        }
        if (runtimeActivator != null) {
            runtimeActivator.stop(context);
            runtimeActivator = null;
        }
    }

    int resolveRuntimeJavaMajorVersion() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null || javaVersion.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        String normalizedJavaVersion = javaVersion.trim();
        if (normalizedJavaVersion.startsWith("1.")) {
            String[] segments = normalizedJavaVersion.split("\\.");
            if (segments.length > 1) {
                return parseJavaMajorVersionSegment(segments[1]);
            }
            return Integer.MAX_VALUE;
        }
        String[] segments = normalizedJavaVersion.split("\\.");
        return parseJavaMajorVersionSegment(segments[0]);
    }

    BundleActivator instantiateRuntimeActivator() throws Exception {
        Class<?> runtimeActivatorClass = Class.forName(RUNTIME_ACTIVATOR_CLASS_NAME);
        if (!BundleActivator.class.isAssignableFrom(runtimeActivatorClass)) {
            throw new IllegalStateException(
                "Runtime activator class does not implement BundleActivator: "
                    + RUNTIME_ACTIVATOR_CLASS_NAME);
        }
        return (BundleActivator) runtimeActivatorClass.getDeclaredConstructor().newInstance();
    }

    void registerIncompatibleModeExtension(BundleContext context, final String message) {
        Hashtable<String, String[]> properties = new Hashtable<String, String[]>();
        properties.put("mode", new String[] { MModeController.MODENAME });
        incompatibleModeExtensionRegistration = context.registerService(
            IModeControllerExtensionProvider.class.getName(),
            new IModeControllerExtensionProvider() {
                @Override
                public void installExtension(ModeController modeController, CommandLineOptions options) {
                    addIncompatibleAiTab(message);
                }
            },
            properties);
    }

    void addIncompatibleAiTab(String message) {
        JTabbedPane tabs = UITools.getFreeplaneTabbedPanel();
        JPanel messagePanel = new JPanel(new BorderLayout());
        JLabel messageLabel = new JLabel("<html><div style='text-align:center;padding:16px;'>"
            + escapeForHtml(message) + "</div></html>", SwingConstants.CENTER);
        messagePanel.add(messageLabel, BorderLayout.CENTER);
        tabs.addTab(
            "",
            ResourceController.getResourceController().getIcon("/images/panelTabs/aiTab.svg?useAccentColor=true"),
            messagePanel,
            TextUtils.getText("ai_panel"));
    }

    private int parseJavaMajorVersionSegment(String segment) {
        try {
            return Integer.parseInt(segment.replaceAll("[^0-9].*", ""));
        }
        catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private String runtimeJavaMajorVersionString(int runtimeJavaMajorVersion) {
        return runtimeJavaMajorVersion == Integer.MAX_VALUE ? "unknown" : String.valueOf(runtimeJavaMajorVersion);
    }

    private String escapeForHtml(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    BootstrapMessageProvider createBootstrapMessageProvider() {
        return new TextUtilsBootstrapMessageProvider();
    }

    interface BootstrapMessageProvider {
        String runtimeVersionTooLow(String runtimeJavaMajorVersion);
        String runtimeActivationFailed(String runtimeJavaMajorVersion);
    }

    private static class TextUtilsBootstrapMessageProvider implements BootstrapMessageProvider {
        @Override
        public String runtimeVersionTooLow(String runtimeJavaMajorVersion) {
            return formatTranslatedMessage(RUNTIME_VERSION_TOO_LOW_MESSAGE_KEY, runtimeJavaMajorVersion);
        }

        @Override
        public String runtimeActivationFailed(String runtimeJavaMajorVersion) {
            return formatTranslatedMessage(RUNTIME_ACTIVATION_FAILED_MESSAGE_KEY, runtimeJavaMajorVersion);
        }

        private String formatTranslatedMessage(String key, String runtimeJavaMajorVersion) {
            String pattern = TextUtils.getText(key);
            if (pattern == null) {
                return "-";
            }
            return new MessageFormat(pattern).format(new Object[] { runtimeJavaMajorVersion });
        }
    }
}

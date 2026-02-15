/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.main.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceBundles;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.FreeplaneVersion;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.mode.mindmapmode.MModeController;

/**
 * @author Dimitry Polivaev
 */
public class ApplicationResourceController extends ResourceController {
    private static final String USE_SYSTEM_LOCALE_PROPERTY = "useSystemLocale";

    public static File getUserPreferencesFile() {
        final String freeplaneDirectory = Compat.getApplicationUserDirectory();
        final File userPropertiesFolder = new File(freeplaneDirectory);
        final File autoPropertiesFile = new File(userPropertiesFolder, "auto.properties");
        return autoPropertiesFile;
    }

    private static File getUserSecretsFile() {
        final String freeplaneDirectory = Compat.getApplicationUserDirectory();
        final File userPropertiesFolder = new File(freeplaneDirectory);
        final File secretsPropertiesFile = new File(userPropertiesFolder, "secrets.properties");
        return secretsPropertiesFile;
    }

	private static final AllPermission ALL_PERMISSION = new AllPermission();
    final private File autoPropertiesFile;
    final private File secretsPropertiesFile;
	final private Properties defProps;
	private LastOpenedList lastOpened;
	final private Properties props;
	final private Properties secretsProps;
	final private Properties securedProps;
	final private Set<String> securedForReadingPropertyKeys;
	final private Set<String> persistedInSecretsFilePropertyKeys;
	public static final String FREEPLANE_BASEDIRECTORY_PROPERTY = "org.freeplane.basedirectory";
	public static final String FREEPLANE_GLOBALRESOURCEDIR_PROPERTY = "org.freeplane.globalresourcedir";
	public static final String DEFAULT_FREEPLANE_GLOBALRESOURCEDIR = "resources";
    private final ArrayList<File> resourceDirectories;
    private final Set<ClassLoader> resourceLoaders;

	public static void showSysInfo() {
		final StringBuilder info = new StringBuilder();
		info.append("freeplane_version = ");
		final FreeplaneVersion freeplaneVersion = FreeplaneVersion.getVersion();
		info.append(freeplaneVersion);
		String revision = freeplaneVersion.getRevision();

		info.append("; freeplane_xml_version = ");
		info.append(FreeplaneVersion.XML_VERSION);
		if(! revision.equals("")){
			info.append("\ngit revision = ");
			info.append(revision);
		}
		info.append("\njava_version = ");
		info.append(System.getProperty("java.version"));
		info.append("; os_name = ");
		info.append(System.getProperty("os.name"));
		info.append("; os_version = ");
		info.append(System.getProperty("os.version"));
		LogUtils.info(info.toString());
	}


	public static String RESOURCE_BASE_DIRECTORY;
	public static String INSTALLATION_BASE_DIRECTORY;
	static {
		try {
			RESOURCE_BASE_DIRECTORY = new File(System.getProperty(ApplicationResourceController.FREEPLANE_GLOBALRESOURCEDIR_PROPERTY,
			ApplicationResourceController.DEFAULT_FREEPLANE_GLOBALRESOURCEDIR)).getCanonicalPath();
			INSTALLATION_BASE_DIRECTORY = new File(System.getProperty(ApplicationResourceController.FREEPLANE_BASEDIRECTORY_PROPERTY, RESOURCE_BASE_DIRECTORY + "/..")).getCanonicalPath();
		} catch (IOException e) {
		}
	}

	/**
	 * @param controller
	 */
	public ApplicationResourceController() {
		super();
		resourceDirectories = new ArrayList<File>(2);
		defProps = readDefaultPreferences();
		props = new SortedProperties(defProps);
		boolean hasLoadedAutoProperties = loadUserProperties(props, getUserPreferencesFile());
		secretsProps = new SortedProperties(props);
		boolean hasLoadedSecretsProperties = loadUserProperties(secretsProps, getUserSecretsFile());
		if (!hasLoadedAutoProperties && !hasLoadedSecretsProperties) {
			System.err.println("User properties not found, new file created");
		}
		securedForReadingPropertyKeys = new HashSet<>();
		persistedInSecretsFilePropertyKeys = new HashSet<>();
		replacePropertyKey("keepSelectedNodeVisibleAfterZoom", "keepSelectedNodeVisible");
		replacePropertyKey("foldingsymbolwidth", "foldingsymbolsize");
		securedProps = new Properties(secretsProps);
		final File userDir = createUserDirectory();
		final String resourceBaseDir = getResourceBaseDir();
		if (resourceBaseDir != null) {
			try {
				final File userResourceDir = new File(userDir, "resources");
				userResourceDir.mkdirs();
				resourceDirectories.add(userResourceDir);
				final File resourceDir = new File(resourceBaseDir);
				resourceDirectories.add(resourceDir);
			}
			catch (final Exception e) {
				e.printStackTrace();
			}
		}
		resourceLoaders = new LinkedHashSet<>();
		if(! getBooleanProperty(USE_SYSTEM_LOCALE_PROPERTY))
		    setDefaultLocale(props.getProperty(ResourceBundles.RESOURCE_LANGUAGE));
		autoPropertiesFile = getUserPreferencesFile();
		secretsPropertiesFile = getUserSecretsFile();
		addPropertyChangeListener(new IFreeplanePropertyListener() {
			@Override
			public void propertyChanged(final String propertyName, final String newValue, final String oldValue) {
				if (propertyName.equals(ResourceBundles.RESOURCE_LANGUAGE)) {
					loadAnotherLanguage();
				}
			}
		});
	}

    private void replacePropertyKey(String oldKey, String newKey) {
		replacePropertyKey(props, oldKey, newKey);
		replacePropertyKey(secretsProps, oldKey, newKey);
    }

	private void replacePropertyKey(Properties sourceProperties, String oldKey, String newKey) {
		if(sourceProperties.containsKey(oldKey) && ! sourceProperties.containsKey(newKey))
			sourceProperties.put(newKey, sourceProperties.getProperty(oldKey));
	}

	private File createUserDirectory() {
		final File userPropertiesFolder = new File(getFreeplaneUserDirectory());
		try {
			if (!userPropertiesFolder.exists()) {
				userPropertiesFolder.mkdirs();
			}
			return userPropertiesFolder;
		}
		catch (final Exception e) {
			e.printStackTrace();
			System.err.println("Cannot create folder for user properties and logging: '"
			        + userPropertiesFolder.getAbsolutePath() + "'");
			return null;
		}
	}

	@Override
	public String getDefaultProperty(final String key) {
		return defProps.getProperty(key);
	}

	@Override
	public String getFreeplaneUserDirectory() {
		return Compat.getApplicationUserDirectory();
	}

	public LastOpenedList getLastOpenedList() {
		return lastOpened;
	}

	@Override
	public Properties getProperties() {
		return props;
	}

	@Override
	public String getProperty(final String key) {
		if(securedForReadingPropertyKeys.contains(key))
			checkSecurityPermission();
	    return securedProps.getProperty(key);
	}

	@Override
	public URL getResource(final String resourcePath) {
		return AccessController.doPrivileged(new PrivilegedAction<URL>() {

			@Override
			public URL run() {
				final String relName = removeSlashAtStart(resourcePath);
				for(File directory : resourceDirectories) {
					File fileResource = new File(directory, relName);
					if (fileResource.exists()) {
						try {
							return Compat.fileToUrl(fileResource);
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				}
				{
				    URL resource = ApplicationResourceController.super.getResource(resourcePath);
				    if (resource != null) {
				        return resource;
				    }
				}
				if ("/lib/freeplaneviewer.jar".equals(resourcePath)) {
				    final String rootDir = new File(getResourceBaseDir()).getAbsoluteFile().getParent();
					try {
						final File try1 = new File(rootDir + "/plugins/org.freeplane.core/lib/freeplaneviewer.jar");
						if (try1.exists()) {
							return try1.toURL();
						}
						final File try2 = new File(rootDir + "/lib/freeplaneviewer.jar");
						if (try2.exists()) {
							return try2.toURL();
						}
					}
					catch (final MalformedURLException e) {
						e.printStackTrace();
					}
				}
				for(ClassLoader loader : resourceLoaders) {
				    URL resource = loader.getResource(resourcePath);
				    if(resource  != null)
				        return resource;
				}

				return null;
			}
		});
	}

	private String removeSlashAtStart(final String name) {
		final String relName;
		if (name.startsWith("/")) {
			relName = name.substring(1);
		}
		else {
			relName = name;
		}
		return relName;
	}

	@Override
	public String getResourceBaseDir() {
		return RESOURCE_BASE_DIRECTORY;
	}

	@Override
	public String getInstallationBaseDir() {
		return INSTALLATION_BASE_DIRECTORY;
    }

	public void registerResourceLoader(ClassLoader loader) {
	    resourceLoaders.add(loader);
	}

	@Override
	public void init() {
		lastOpened = new LastOpenedList();
		super.init();
	}

	private Properties readDefaultPreferences() {
		final Properties props = new Properties();
		readDefaultPreferences(props, ResourceController.FREEPLANE_PROPERTIES);
		final String propsLocs = props.getProperty("load_next_properties", "");
		readDefaultPreferences(props, propsLocs.split(";"));
		return props;
	}

	private void readDefaultPreferences(final Properties props, final String[] locArray) {
		for (final String loc : locArray) {
			readDefaultPreferences(props, loc);
		}
	}

	private void readDefaultPreferences(final Properties props, final String propsLoc) {
		final URL defaultPropsURL = getResource(propsLoc);
		loadProperties(props, defaultPropsURL);
	}

	private boolean loadUserProperties(Properties target, File file) {
		try (InputStream in = new FileInputStream(file)){
			target.load(in);
			return true;
		}
		catch (final Exception ex) {
			return false;
		}
	}

	@Override
	public void saveProperties() {
		MModeController modeController = MModeController.getMModeController();
		if(modeController != null) {
			MIconController iconController = (MIconController)modeController.getExtension(IconController.class);
			if(iconController != null)
				iconController.saveRecentlyUsedActions();
		}
		try (OutputStream out = new FileOutputStream(autoPropertiesFile)){
			final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(out, "8859_1");
			outputStreamWriter.write("#Freeplane ");
			outputStreamWriter.write(FreeplaneVersion.getVersion().toString());
			outputStreamWriter.write('\n');
			outputStreamWriter.flush();
			props.store(out, null);
		}
		catch (final Exception ex) {
		}
		try (OutputStream out = new FileOutputStream(secretsPropertiesFile)){
			final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(out, "8859_1");
			outputStreamWriter.write("#Freeplane ");
			outputStreamWriter.write(FreeplaneVersion.getVersion().toString());
			outputStreamWriter.write('\n');
			outputStreamWriter.flush();
			secretsProps.store(out, null);
		}
		catch (final Exception ex) {
		}
		try {
			((ResourceBundles)getResources()).saveUserResources();
		}
		catch (final Exception ex) {
		}
		FilterController filterController = FilterController.getCurrentFilterController();
		if(filterController != null)
			filterController.saveConditions();
	}

	/**
	 * @param pProperties
	 */
	private void setDefaultLocale(final String lang ) {
		if (lang == null) {
			return;
		}
		Locale localeDef = null;
		switch (lang.length()) {
			case 2:
				localeDef = new Locale(lang);
				break;
			case 5:
				localeDef = new Locale(lang.substring(0, 2), lang.substring(3, 5));
				break;
			default:
				return;
		}
		Locale.setDefault(localeDef);
	}

	@Override
	public void setDefaultProperty(final String key, final String value) {
		defProps.setProperty(key, value);
	}

	@Override
	public void setProperty(final String key, final String value) {
		final String oldValue = getProperty(key);
		if (oldValue == value) {
			return;
		}
		if (oldValue != null && oldValue.equals(value)) {
			return;
		}
		if(securedProps.containsKey(key)) {
		    checkSecurityPermission();
            securedProps.setProperty(key, value);
        }
		if (persistedInSecretsFilePropertyKeys.contains(key)) {
			secretsProps.setProperty(key, value);
			props.remove(key);
		}
		else {
			props.setProperty(key, value);
		}
		firePropertyChanged(key, value, oldValue);
	}

    static private void checkSecurityPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ALL_PERMISSION);
        }
    }

    @Override
    public Properties getSecuredProperties() {
        checkSecurityPermission();
        return securedProps;
    }

    @Override
    public void securePropertyForModification(String key) {
        checkSecurityPermission();
        if(! securedProps.containsKey(key)) {
			String propertyValue = getProperty(key);
			if (propertyValue != null) {
				securedProps.setProperty(key, propertyValue);
			}
		}
    }
    @Override
    public void securePropertyForReadingAndModification(String key) {
    	securePropertyForModification(key);
    	securedForReadingPropertyKeys.add(key);
    }

    @Override
    public void persistPropertyInSecretsFile(String key) {
		if (props.containsKey(key)) {
	    	checkSecurityPermission();
			String value = props.getProperty(key);
			if (value != null) {
				secretsProps.setProperty(key, value);
			}
			props.remove(key);
		}
    	persistedInSecretsFilePropertyKeys.add(key);
    }

    @Override
    public boolean isPropertySetByUser(String key) {
        return props.containsKey(key) || secretsProps.containsKey(key);
    }
}

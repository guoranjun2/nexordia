package org.freeplane.core.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import org.freeplane.api.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.WindowConfigurationStorage;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.ui.FrameController;

public class JFreeplaneCustomizableFileChooser extends JFileChooser{
    private static final String FILE_CHOOSER_SPECIAL_FOLDERS_PROPERTY = "file_chooser_shows_special_folders";

	private static final String USE_SHELL_FOLDER_JAVA_PROPERTY = "FileChooser.useShellFolder";

	private static final long serialVersionUID = 1;

	private final List<JComponent> optionComponents = new ArrayList<>();

    private boolean areSpecialFoldersShown;

	private boolean isFileHidingDisabledForCurrentDirectory = false;

    public JFreeplaneCustomizableFileChooser() {
        super();
        initializeSpecialFolderShownFlag();
        showHiddenFilesOnMac();
    }

	private void initializeSpecialFolderShownFlag() {
		boolean areSpecialFoldersShown = ResourceController.getResourceController().getBooleanProperty(FILE_CHOOSER_SPECIAL_FOLDERS_PROPERTY);
		if(this.areSpecialFoldersShown != areSpecialFoldersShown)
			this.areSpecialFoldersShown = areSpecialFoldersShown;
	}

    public JFreeplaneCustomizableFileChooser(File currentDirectory) {
        super(currentDirectory);
        initializeSpecialFolderShownFlag();
        showHiddenFilesOnMac();
    }

	private void showHiddenFilesOnMac() {
		if(Compat.isMacOsX())
			setFileHidingEnabled(false);
	}

    @Override
	protected void setup(FileSystemView view) {
    	initializeSpecialFolderShownFlag();
        putClientProperty(USE_SHELL_FOLDER_JAVA_PROPERTY, areSpecialFoldersShown);
    	super.setup(view);
	}

	@Override
	public Dimension getPreferredSize() {
		return fixAquaFileChooserUIPreferredSize();
	}

	@Override
	public int showOpenDialog(Component parent) throws HeadlessException {
		setDialogType(OPEN_DIALOG);
		if(usesNativeFileDialog())
			return showNativeFileDialog(parent, FileDialog.LOAD);
		return super.showOpenDialog(parent);
	}

	@Override
	public int showSaveDialog(Component parent) throws HeadlessException {
		setDialogType(SAVE_DIALOG);
		if(usesNativeFileDialog())
			return showNativeFileDialog(parent, FileDialog.SAVE);
		return super.showSaveDialog(parent);
	}

	private boolean usesNativeFileDialog() {
		return Compat.isMacOsX() && getFileSelectionMode() != FILES_AND_DIRECTORIES
				&& optionComponents.isEmpty() && ! accessoryContainsOptions();
	}

	private boolean accessoryContainsOptions() {
		return containsCheckBox(getAccessory());
	}

	private boolean containsCheckBox(Component component) {
		if(component == null)
			return false;
		if(component instanceof JCheckBox)
			return true;
		if(component instanceof java.awt.Container) {
			for(Component child : ((java.awt.Container) component).getComponents()) {
				if(containsCheckBox(child))
					return true;
			}
		}
		return false;
	}

	private int showNativeFileDialog(Component parent, int mode) {
		final FileDialog dialog = createNativeFileDialog(parent, mode);
		final File selectedFile = getSelectedFile();
		final File currentDirectory = selectedFile != null && selectedFile.getParentFile() != null
				? selectedFile.getParentFile() : getCurrentDirectory();
		if(currentDirectory != null)
			dialog.setDirectory(currentDirectory.getPath());
		if(selectedFile != null)
			dialog.setFile(selectedFile.getName());
		dialog.setMultipleMode(isMultiSelectionEnabled());
		dialog.setFilenameFilter(createNativeFileFilter());
		final String oldDirectorySelectionProperty = System.getProperty("apple.awt.fileDialogForDirectories");
		final String oldHiddenFilesProperty = System.getProperty("apple.awt.fileDialogShowHiddenFiles");
		try {
			if(getFileSelectionMode() == DIRECTORIES_ONLY)
				System.setProperty("apple.awt.fileDialogForDirectories", "true");
			System.setProperty("apple.awt.fileDialogShowHiddenFiles", "true");
			dialog.setVisible(true);
		}
		finally {
			restoreProperty("apple.awt.fileDialogForDirectories", oldDirectorySelectionProperty);
			restoreProperty("apple.awt.fileDialogShowHiddenFiles", oldHiddenFilesProperty);
		}
		File[] files = dialog.getFiles();
		if(files == null || files.length == 0)
			files = selectedFiles(dialog);
		if(files.length == 0)
			return CANCEL_OPTION;
		setSelectedFiles(files);
		setSelectedFile(files[0]);
		final File parentFile = files[0].getParentFile();
		if(parentFile != null)
			setCurrentDirectory(parentFile);
		return APPROVE_OPTION;
	}

	private void restoreProperty(String property, String oldValue) {
		if(oldValue != null)
			System.setProperty(property, oldValue);
		else
			System.clearProperty(property);
	}

	private File[] selectedFiles(FileDialog dialog) {
		final String file = dialog.getFile();
		final String directory = dialog.getDirectory();
		if(file == null || directory == null)
			return new File[0];
		return new File[] { new File(directory, file) };
	}

	private FileDialog createNativeFileDialog(Component parent, int mode) {
		final Window window = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
		final String title = getDialogTitle();
		if(window instanceof Frame)
			return new FileDialog((Frame) window, title, mode);
		if(window instanceof Dialog)
			return new FileDialog((Dialog) window, title, mode);
		return new FileDialog((Frame) null, title, mode);
	}

	private FilenameFilter createNativeFileFilter() {
		final FileFilter fileFilter = getFileFilter();
		if(fileFilter == null || isAcceptAllFileFilterUsed() && fileFilter == getAcceptAllFileFilter())
			return null;
		return (dir, name) -> fileFilter.accept(new File(dir, name));
	}

	@Override
	public boolean isFileHidingEnabled() {
		if(isFileHidingDisabledForCurrentDirectory)
			return false;
		return super.isFileHidingEnabled();
	}

	private Dimension fixAquaFileChooserUIPreferredSize() {
		Dimension preferredSize = super.getPreferredSize();
		if(isPreferredSizeSet() || ! getUI().getClass().getSimpleName().equals("AquaFileChooserUI")) {
			return preferredSize;
		}
		LayoutManager layout = getLayout();
		Dimension layoutPrefSize = layout.preferredLayoutSize(this);
		Dimension maximumSize = super.getMaximumSize();
		int width = Math.min(maximumSize.width, Math.max(preferredSize.width, layoutPrefSize.width));
		int height = Math.min(maximumSize.height, Math.max(preferredSize.height, layoutPrefSize.height));
		return new Dimension(width, height);
	}

	@Override
	public void setCurrentDirectory(File dir) {
		if(dir != null && ! areSpecialFoldersShown && Compat.isWindowsOS() && dir.getClass().equals(File.class)) {
			try {
				setDirectoryBehavingLikeShellFolder(dir);
				return;
			}
			catch (IOException | InvalidPathException e) {
			}
			catch(IllegalAccessError e) {
				LogUtils.severe(e);
			}
		}
		if(UIManager.getLookAndFeel().getName().equals(FrameController.VAQUA_LAF_NAME)) {
			final boolean wasFileHidingEnabled = isFileHidingEnabled();
			isFileHidingDisabledForCurrentDirectory = isDirectoryHiddenInVAqua(dir);
			final boolean isFileHidingEnabled = isFileHidingEnabled();
			if(wasFileHidingEnabled != isFileHidingEnabled) {
				SwingUtilities.invokeLater(() ->
					firePropertyChange(FILE_HIDING_CHANGED_PROPERTY, wasFileHidingEnabled, isFileHidingEnabled));
			}
		}
		super.setCurrentDirectory(dir);
	}

	private boolean isDirectoryHiddenInVAqua(File directory) {
		for (; directory != null; directory = directory.getParentFile()) {
			if (getFileSystemView().isHiddenFile(directory))
				return true;
		}
		return false;
	}

	private void setDirectoryBehavingLikeShellFolder(File dir) throws IOException {
		File shellFolder = sun.awt.shell.ShellFolder.getShellFolder(dir);
		super.setCurrentDirectory(shellFolder);
	}


	@Override
	public boolean accept(File f) {
		if(! areSpecialFoldersShown && Compat.isWindowsOS() && f.getName().endsWith(".lnk")) {
			return false;
		}
		return super.accept(f);
	}



	@FunctionalInterface
    public interface Customizer extends Consumer<JDialog>{
        Customizer DEFAULT = d -> {};
    }

    private Consumer<JDialog> customizer = Customizer.DEFAULT;

    public void addCustomizer(Customizer newCustomizer) {
        customizer = customizer.andThen(newCustomizer);
    }

    public Consumer<JDialog> getCustomizer() {
        return customizer;
    }
    private static final String WINDOW_CONFIG_PROPERTY = "file_chooser_window_configuration";
    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException {
        final JDialog dialog = super.createDialog(parent);
        customizer.accept(dialog);
        if(optionComponents.size() == 1) {
           dialog.getContentPane().add(optionComponents.get(0), BorderLayout.SOUTH);
        }
        else if(optionComponents.size() > 1) {
            Box optionBox = Box.createVerticalBox();
            optionComponents.forEach(c -> c.setAlignmentX(LEFT_ALIGNMENT));
            optionComponents.forEach(optionBox::add);
            dialog.getContentPane().add(optionBox, BorderLayout.SOUTH);
        }
        String windowConfigurationPropertyName = windowConfigurationPropertyName();
		final WindowConfigurationStorage windowConfigurationStorage = new WindowConfigurationStorage(windowConfigurationPropertyName);
        windowConfigurationStorage.setBounds(dialog);
        if(Compat.isMacOsX()) {
            ActionMap am = getActionMap();
            InputMap globalInputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            KeyStroke ks  = KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, InputEvent.META_MASK | InputEvent.SHIFT_MASK);
                    globalInputMap.put(ks, ks);
			am.put(ks, new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					boolean isHiding = isFileHidingEnabled();
                    setFileHidingEnabled(!isHiding);
				}
			});
        }

        return dialog;
    }

	private String windowConfigurationPropertyName() {
		JComponent accessory = getAccessory();
		if (accessory == null)
			return WINDOW_CONFIG_PROPERTY;
		else {
			Dimension preferredSize = accessory.getPreferredSize();
			return WINDOW_CONFIG_PROPERTY + "_" + preferredSize.width + "." +preferredSize.height;
		}
	}

    public void addOptionComponent(final JComponent component) {
        optionComponents.add(component);
    }


}

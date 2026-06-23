package org.freeplane.main.application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.freeplane.core.util.Compat;

public class UserReadmeWriter {
	private static final String USER_README_RESOURCE = "/userReadme.txt";
	private static final String USER_README_FILENAME = "README.txt";

	public void ensureReadmeExists() {
		File readmeDirectory = new File(Compat.getApplicationUserDirectory());
		File readmeFile = new File(readmeDirectory, USER_README_FILENAME);
		if (readmeFile.exists()) {
			return;
		}
		if (!readmeDirectory.exists() && !readmeDirectory.mkdirs()) {
			return;
		}
		if (!readmeDirectory.isDirectory()) {
			return;
		}
		try (InputStream inputStream = UserReadmeWriter.class.getResourceAsStream(USER_README_RESOURCE)) {
			if (inputStream == null) {
				return;
			}
			try (OutputStream outputStream = new FileOutputStream(readmeFile)) {
				copy(inputStream, outputStream);
			}
		}
		catch (IOException e) {
		}
	}

	private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[8192];
		int count;
		while ((count = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, count);
		}
	}
}

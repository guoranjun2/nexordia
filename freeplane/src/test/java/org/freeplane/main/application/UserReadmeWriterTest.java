package org.freeplane.main.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.freeplane.core.util.Compat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UserReadmeWriterTest {
	private static final String USER_README_RESOURCE = "/userReadme_1.13.txt";
	private static final String USER_README_FILENAME = "README.txt";
	private static final String USER_README_DIRECTORY = "1.13.x";
	private static final String CURRENT_VERSION_DIRECTORY = "1.12.x";

	private Path tempDirectory;
	private String previousUserDirProperty;

	@Before
	public void setup() throws Exception {
		previousUserDirProperty = System.getProperty(Compat.FREEPLANE_USERDIR_PROPERTY);
		tempDirectory = Files.createTempDirectory("freeplane-userdir-");
		System.setProperty(Compat.FREEPLANE_USERDIR_PROPERTY, tempDirectory.toString());
		resetUserDirCache();
	}

	@After
	public void cleanup() throws Exception {
		if (previousUserDirProperty == null) {
			System.clearProperty(Compat.FREEPLANE_USERDIR_PROPERTY);
		}
		else {
			System.setProperty(Compat.FREEPLANE_USERDIR_PROPERTY, previousUserDirProperty);
		}
		resetUserDirCache();
		deleteRecursively(tempDirectory);
	}

	@Test
	public void createsReadmeWhenMissing() throws Exception {
		UserReadmeWriter uut = new UserReadmeWriter();

		uut.ensureReadmeExists();

		Path readmeDirectory = tempDirectory.resolve(USER_README_DIRECTORY);
		Path readmeFile = readmeDirectory.resolve(USER_README_FILENAME);
		assertTrue(Files.isDirectory(readmeDirectory));
		assertTrue(Files.isRegularFile(readmeFile));
		try (Stream<Path> files = Files.list(readmeDirectory)) {
			assertEquals(1, files.count());
		}
	}

	@Test
	public void doesNotOverwriteExistingReadme() throws Exception {
		Path readmeDirectory = tempDirectory.resolve(USER_README_DIRECTORY);
		Path readmeFile = readmeDirectory.resolve(USER_README_FILENAME);
		Files.createDirectories(readmeDirectory);
		Files.write(readmeFile, "custom".getBytes(StandardCharsets.US_ASCII));

		UserReadmeWriter uut = new UserReadmeWriter();

		uut.ensureReadmeExists();

		byte[] content = Files.readAllBytes(readmeFile);
		assertArrayEquals("custom".getBytes(StandardCharsets.US_ASCII), content);
	}

	@Test
	public void writesBundledReadmeContent() throws Exception {
		UserReadmeWriter uut = new UserReadmeWriter();

		uut.ensureReadmeExists();

		Path readmeFile = tempDirectory.resolve(USER_README_DIRECTORY).resolve(USER_README_FILENAME);
		byte[] fileContent = Files.readAllBytes(readmeFile);
		byte[] resourceContent = readResourceContent();
		assertArrayEquals(resourceContent, fileContent);
	}

	@Test
	public void usesCurrentVersionDirectoryForPreferences() throws Exception {
		Path expected = tempDirectory.toRealPath().resolve(CURRENT_VERSION_DIRECTORY).resolve("auto.properties");
		assertEquals(expected.toFile().getCanonicalPath(),
				ApplicationResourceController.getUserPreferencesFile().getCanonicalPath());
	}

	private byte[] readResourceContent() throws IOException {
		try (InputStream inputStream = UserReadmeWriter.class.getResourceAsStream(USER_README_RESOURCE)) {
			if (inputStream == null) {
				throw new IOException("Missing resource " + USER_README_RESOURCE);
			}
			return readAllBytes(inputStream);
		}
	}

	private byte[] readAllBytes(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[8192];
		int count;
		int offset = 0;
		byte[] result = new byte[0];
		while ((count = inputStream.read(buffer)) != -1) {
			byte[] expanded = new byte[offset + count];
			System.arraycopy(result, 0, expanded, 0, offset);
			System.arraycopy(buffer, 0, expanded, offset, count);
			result = expanded;
			offset += count;
		}
		return result;
	}

	private void resetUserDirCache() throws Exception {
		Field userDirField = Compat.class.getDeclaredField("userFpDir");
		userDirField.setAccessible(true);
		userDirField.set(null, null);
	}

	private void deleteRecursively(Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				}
				catch (IOException e) {
				}
			});
		}
	}
}

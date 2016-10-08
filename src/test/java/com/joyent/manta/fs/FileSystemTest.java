package com.joyent.manta.fs;

import com.joyent.manta.fs.config.ConfigContext;
import com.joyent.manta.fs.config.MapConfigContext;
import com.joyent.manta.fs.config.SystemSettingsConfigContext;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.exception.MantaException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
@Test(groups = "file_system")
public class FileSystemTest {
    private static final String TEST_DATA = "I AM A MANTA FILE";

    private final ConfigContext config = new SystemSettingsConfigContext();
    private final String testDirectory = String.format("/%s/stor/%s",
            config.getMantaUser(), UUID.randomUUID());
    private final MantaClient mantaClient;

    {
        try {
            mantaClient = new MantaClient(config);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeClass
    public void setup() throws IOException, MantaException {
        mantaClient.putDirectory(testDirectory);
    }

    @AfterClass
    public void cleanUp() throws IOException, MantaException {
        mantaClient.deleteRecursive(testDirectory);
    }

    @Test
    public void canOpenMantaPathWithFullURLAndUsername() throws IOException {
        String testPath = String.format("%s/%s", testDirectory, UUID.randomUUID());
        mantaClient.put(testPath, TEST_DATA);

        URI mantaURL = URI.create(config.getMantaURL());
        String mantaHost = mantaURL.getHost();
        int mantaPort = mantaURL.getPort() >= -1 ? mantaURL.getPort() : 443;

        String fullUrl = String.format("manta://%s@%s:%d%s",
                config.getMantaUser(), mantaHost, mantaPort, testPath);
        URI uri = URI.create(fullUrl);

        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            Path path = Paths.get(uri);
            List<String> lines = Files.readAllLines(path);
            Assert.assertEquals(lines.get(0), TEST_DATA, "File data should be identical");
        }
    }

    @Test
    public void canOpenMantaPathWithFullURL() throws IOException {
        String testPath = String.format("%s/%s", testDirectory, UUID.randomUUID());
        mantaClient.put(testPath, TEST_DATA);

        URI mantaURL = URI.create(config.getMantaURL());
        String mantaHost = mantaURL.getHost();
        int mantaPort = mantaURL.getPort() >= -1 ? mantaURL.getPort() : 443;

        String fullUrl = String.format("manta://%s:%d%s",
                mantaHost, mantaPort, testPath);
        URI uri = URI.create(fullUrl);

        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            Path path = Paths.get(uri);
            List<String> lines = Files.readAllLines(path);
            Assert.assertEquals(lines.get(0), TEST_DATA, "File data should be identical");
        }
    }

    @Test
    public void canOpenMantaPathWithOnlyScheme() throws IOException {
        String testPath = String.format("%s/%s", testDirectory, UUID.randomUUID());
        mantaClient.put(testPath, TEST_DATA);

        String fullUrl = String.format("manta://%s", testPath);
        URI uri = URI.create(fullUrl);

        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            Path path = Paths.get(uri);
            List<String> lines = Files.readAllLines(path);
            Assert.assertEquals(lines.get(0), TEST_DATA, "File data should be identical");
        }
    }

    @Test
    public void canOpenMantaPathWithExplicitConfig() throws IOException {
        String testPath = String.format("%s/%s", testDirectory, UUID.randomUUID());
        mantaClient.put(testPath, TEST_DATA);

        String fullUrl = String.format("manta://%s", testPath);
        URI uri = URI.create(fullUrl);

        Map<String, Object> env = new HashMap<>();
        env.put(MapConfigContext.MANTA_USER_KEY, config.getMantaUser());
        env.put(MapConfigContext.MANTA_URL_KEY, config.getMantaURL());
        env.put(MapConfigContext.MANTA_KEY_PATH_KEY, config.getMantaKeyPath());
        env.put(MapConfigContext.MANTA_KEY_ID_KEY, config.getMantaKeyId());

        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            Path path = Paths.get(uri);
            List<String> lines = Files.readAllLines(path);
            Assert.assertEquals(lines.get(0), TEST_DATA, "File data should be identical");
        }
    }
}

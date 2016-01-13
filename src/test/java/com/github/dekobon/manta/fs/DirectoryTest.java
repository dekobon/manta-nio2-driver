package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObjectResponse;
import com.joyent.manta.exception.MantaCryptoException;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Test(groups = { "directory" })
public class DirectoryTest {
    private final ConfigContext config = new SystemSettingsConfigContext();
    private FileSystem fileSystem;
    private MantaClient mantaClient;
    private String testPathPrefix;

    private static final String TEST_DATA = "I AM STRING DATA!";

    @BeforeClass
    public void beforeClass()
            throws IOException, MantaCryptoException {
        URI uri = ConfigContext.mantaURIFromContext(config);
        fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
        mantaClient = new MantaClient(config);

        testPathPrefix = String.format("/%s/stor/%s",
                config.getMantaHomeDirectory(), UUID.randomUUID());
        mantaClient.putDirectory(testPathPrefix);
    }

    @AfterClass
    public void afterClass() throws IOException, MantaCryptoException {
        if (mantaClient != null) {
            mantaClient.deleteRecursive(testPathPrefix);
            mantaClient.closeQuietly();
        }

        if (fileSystem != null) {
            fileSystem.close();
        }
    }

    @Test(expectedExceptions = { AccessDeniedException.class })
    public void listRootDirectory() throws IOException {
        Path rootPath = fileSystem.getPath("/");
        List<String> listing = listPath(rootPath);

        listing.forEach(item -> System.out.println(item));
    }

    @Test
    public void listUserDirectory() throws IOException {
        Path rootPath = fileSystem.getPath("~~");
        List<String> listing = listPath(rootPath);

        try {
            String jobsEntry = String.format("/%s/jobs", config.getMantaUser());
            Assert.assertTrue(listing.contains(jobsEntry), "Listing didn't contain jobs");
            String storEntry = String.format("/%s/stor", config.getMantaUser());
            Assert.assertTrue(listing.contains(storEntry), "Listing didn't contain stor");
            String publicEntry = String.format("/%s/public", config.getMantaUser());
            Assert.assertTrue(listing.contains(publicEntry), "Listing didn't contain public");
        } catch (AssertionError e) {
            System.err.println(String.format("Listing contents: %s",
                    StringUtils.join(listing, ",")));
            throw e;
        }
    }

    @Test(expectedExceptions = { AccessDeniedException.class })
    public void listNonexistentDirectoryFromRoot() throws IOException {
        Path rootPath = fileSystem.getPath("/mymoneyisonthisnotbeinghere7");
        List<String> listing = listPath(rootPath);

        listing.forEach(item -> System.out.println(item));
    }

    @Test(expectedExceptions = { NoSuchFileException.class })
    public void listNonexistentDirectoryFromHome() throws IOException {
        Path rootPath = fileSystem.getPath("~~/mymoneyisonthisnotbeinghere2");
        List<String> listing = listPath(rootPath);

        listing.forEach(item -> System.out.println(item));
    }

    @Test
    public void isDirectoryMarkedAsDirectory() {
        Path stor = fileSystem.getPath("~~/stor");
        Assert.assertTrue(Files.isDirectory(stor),
                "This is a directory and it should be marked as such");
    }

    @Test
    public void verifyLastModifiedTimeExists() throws IOException {
        Path file = fileSystem.getPath("~~/stor");
        FileTime time = Files.getLastModifiedTime(file);

        Assert.assertNotNull(time);
    }

    @Test
    public void verifyLastModifiedTimeIsAccurate() throws IOException {
        String filename = UUID.randomUUID().toString();
        String path = String.format("%s/%s", testPathPrefix, filename);

        MantaObjectResponse response = mantaClient.put(path, TEST_DATA);

        Path file = fileSystem.getPath(testPathPrefix, filename);
        Instant mtime = Files.getLastModifiedTime(file).toInstant();

        Instant expectedMTime = response.getLastModifiedTime().toInstant();
        Assert.assertEquals(mtime, expectedMTime,
                "Last modified time wasn't the same");
    }

    public static List<String> listPath(Path directory) throws IOException {
        List<String> fileNames = new ArrayList<>();
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory);
        for (Path path : directoryStream) {
            fileNames.add(path.toString());
        }
        return fileNames;
    }
}

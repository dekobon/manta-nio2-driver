package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import com.github.dekobon.manta.fs.provider.MantaFileSystemProvider;
import com.github.dekobon.manta.fs.provider.MantaFileSystemRepository;
import com.github.fge.filesystem.provider.FileSystemRepository;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.exception.MantaException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Scanner;
import java.util.UUID;

public class FileTest {
    private final FileSystemRepository repository = new MantaFileSystemRepository();
    private final FileSystemProvider provider = new MantaFileSystemProvider(repository);
    private final FileSystem fileSystem;
    private final ConfigContext config = new SystemSettingsConfigContext();
    private final String testDirectory = String.format("/%s/stor/%s",
            config.getMantaUser(), UUID.randomUUID());
    private final MantaClient mantaClient;

    {
        try {
            URI uri = URI.create(String.format("manta://%s", config.getMantaUser()));
            fileSystem = provider.newFileSystem(uri, Collections.emptyMap());
            mantaClient = new MantaClient(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public void setup() throws IOException, MantaException {
        mantaClient.putDirectory(testDirectory, null);
    }

    @AfterClass
    public void cleanUp() throws IOException, MantaException {
        mantaClient.deleteRecursive(testDirectory);
    }

    @Test(groups = { "file" })
    public void canReadAsciiFile() throws IOException, MantaException {
        final String fileContents = "Hello World";
        String testFilePath = uploadTestFile("read_file_test", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);
        try (Scanner scanner = new Scanner(fileToRead)) {

            Assert.assertTrue(scanner.hasNext(), "There should be a first line in the file");
            Assert.assertEquals(scanner.nextLine(), fileContents,
                    String.format("File contents did not equal: %s", fileContents));
        }
    }

    @Test(groups = { "file" })
    public void canReadUTF8AsciiFile() throws IOException, MantaException {
        final String fileContents = "\u3053\u3093\u306B\u3061\u306F";
        String testFilePath = uploadTestFile("UTF8\u30C6\u30B9\u30C8\u30D5\u30A1\u30A4\u30EB", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);
        try (Scanner scanner = new Scanner(fileToRead)) {

            Assert.assertTrue(scanner.hasNext(), "There should be a first line in the file");
            Assert.assertEquals(scanner.nextLine(), fileContents,
                    String.format("File contents did not equal: %s", fileContents));
        }
    }

    @Test(groups = { "file" })
    public void canReadBinaryFile() throws IOException, MantaException {
        final byte[] content = new byte[] {
                (byte) 0x00, (byte) 0xad, (byte) 0xdf, (byte) 0x45,
                (byte) 0x53, (byte) 0x4a, (byte) 0xf8, (byte) 0xff };
        String testFilePath = uploadTestFile("read_file_test", content);

        Path fileToRead = fileSystem.getPath(testFilePath);
        final byte[] actualBytes = Files.readAllBytes(fileToRead);

        Assert.assertEquals(actualBytes, content);
    }

    @Test(groups = { "file" })
    public void verifyLastModifiedTime() throws IOException, MantaException {
        // Adjust time to be before now to account for all kinds of skew
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                .minusSeconds(5);

        String testFilePath = uploadTestFile("read_file_test", "Hello World");

        Path file = fileSystem.getPath(testFilePath);
        FileTime time = Files.getLastModifiedTime(file);

        Assert.assertTrue(time.toInstant().isAfter(now) || time.toInstant().equals(now),
                String.format("File should have been created after %s was created %s",
                        now, time));
    }

    @Test(groups = { "file" })
    public void createTempFile() throws IOException, MantaException {
        Path dir = fileSystem.getPath(testDirectory);
        Path temp = Files.createTempFile(dir, "prefix", "suffix");

        MantaObject head = mantaClient.head(temp.toString());
    }

    @Test(groups = { "file" })
    public void isFileMarkedAsFile() throws IOException, MantaException {
        String testFilePath = uploadTestFile("read_file_test", "Hello World");

        Path file = fileSystem.getPath(testFilePath);
        Assert.assertTrue(Files.isRegularFile(file),
                "This is a file and it should be marked as such");
    }

    @Test(groups = { "file" })
    public void canCopyFile() throws IOException, MantaException {
        String fileContents = "Hello World";
        String testFilePath = uploadTestFile("copy_file_test", fileContents);
        Path source = fileSystem.getPath(testFilePath);
        Path target = fileSystem.getPath(String.format("%s/%s",
                testDirectory, "destination"));

        Files.copy(source, target);

        try (Scanner scanner = new Scanner(target)) {

            Assert.assertTrue(scanner.hasNext(), "There should be a first line in the file");
            Assert.assertEquals(scanner.nextLine(), fileContents,
                    String.format("File contents did not equal: %s", fileContents));
        }
    }

    public void canOpenMantaPath() {
        Paths.get(URI.create("manta://"));
    }

    /**
     * Manually uploads a file to Manta, so that we can test reads.
     */
    protected String uploadTestFile(String testFilename, String contents)
            throws IOException, MantaException {
        // Manually upload a file to Manta, so that we can test read
        String testFilePath = String.format("%s/%s", testDirectory, testFilename);

        mantaClient.put(testFilePath, contents);

        // This will throw an exception if the file isn't on Manta
        MantaObject headObject = mantaClient.head(testFilePath);

        return headObject.getPath();
    }

    /**
     * Manually uploads a file to Manta, so that we can test reads.
     */
    protected String uploadTestFile(String testFilename, byte[] contents)
            throws IOException, MantaException {
        // Manually upload a file to Manta, so that we can test read
        String testFilePath = String.format("%s/%s", testDirectory, testFilename);

        try (InputStream bs = new ByteArrayInputStream(contents)) {
            mantaClient.put(testFilePath, bs);
        }

        // This will throw an exception if the file isn't on Manta
        MantaObject headObject = mantaClient.head(testFilePath);

        return headObject.getPath();
    }
}

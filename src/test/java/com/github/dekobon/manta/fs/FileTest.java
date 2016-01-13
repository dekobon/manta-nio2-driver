package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import com.github.dekobon.manta.fs.driver.MantaTempSeekableByteChannel;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.client.MantaSeekableByteChannel;
import com.joyent.manta.exception.MantaException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Scanner;
import java.util.UUID;

@Test(groups = { "file" })
public class FileTest {
    private final ConfigContext config = new SystemSettingsConfigContext();
    private final String testDirectory = String.format("/%s/stor/%s",
            config.getMantaUser(), UUID.randomUUID());

    private FileSystem fileSystem;
    private MantaClient mantaClient;

    @BeforeClass
    public void setup() throws IOException, MantaException {
        URI uri = ConfigContext.mantaURIFromContext(config);
        fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
        mantaClient = new MantaClient(config);

        mantaClient.putDirectory(testDirectory);
    }

    @AfterClass
    public void cleanUp() throws IOException, MantaException {
        if (mantaClient != null) {
            mantaClient.deleteRecursive(testDirectory);
            mantaClient.closeQuietly();
        }

        if (fileSystem != null) {
            fileSystem.close();
        }    }

    @Test
    public void isReadable() throws IOException {
        final String fileContents = "Hello World";
        String testFilePath = uploadTestFile("read_file_test", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);

        Assert.assertTrue(Files.isReadable(fileToRead),
                "File should be marked as readable");
    }

    @Test
    public void canReadAsciiFile() throws IOException {
        final String fileContents = "Hello World";
        String testFilePath = uploadTestFile("read_file_test", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);
        try (Scanner scanner = new Scanner(fileToRead)) {

            Assert.assertTrue(scanner.hasNext(), "There should be a first line in the file");
            Assert.assertEquals(scanner.nextLine(), fileContents,
                    String.format("File contents did not equal: %s", fileContents));
        }
    }

    @Test
    public void canReadUTF8AsciiFile() throws IOException {
        final String fileContents = "\u3053\u3093\u306B\u3061\u306F";
        String testFilePath = uploadTestFile("UTF8\u30C6\u30B9\u30C8\u30D5\u30A1\u30A4\u30EB", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);
        try (Scanner scanner = new Scanner(fileToRead)) {

            Assert.assertTrue(scanner.hasNext(), "There should be a first line in the file");
            Assert.assertEquals(scanner.nextLine(), fileContents,
                    String.format("File contents did not equal: %s", fileContents));
        }
    }

    @Test
    public void canReadBinaryFile() throws IOException {
        final byte[] content = new byte[] {
                (byte) 0x00, (byte) 0xad, (byte) 0xdf, (byte) 0x45,
                (byte) 0x53, (byte) 0x4a, (byte) 0xf8, (byte) 0xff };
        String testFilePath = uploadTestFile("read_file_test", content);

        Path fileToRead = fileSystem.getPath(testFilePath);
        final byte[] actualBytes = Files.readAllBytes(fileToRead);

        Assert.assertEquals(actualBytes, content);
    }

    @Test
    public void verifyLastModifiedTime() throws IOException {
        // Adjust time to be before now to account for all kinds of skew
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                .minusSeconds(8);

        String testFilePath = uploadTestFile("read_file_test", "Hello World");

        Path file = fileSystem.getPath(testFilePath);
        FileTime time = Files.getLastModifiedTime(file);

        Assert.assertTrue(time.toInstant().isAfter(now) || time.toInstant().equals(now),
                String.format("File should have been created after %s was created %s",
                        now, time));
    }

    @Test
    public void canReadFromInputStream() throws IOException {
        final String fileContents = "Hello World";
        String testFilePath = uploadTestFile("read_file_test", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);

        try (InputStream is = Files.newInputStream(fileToRead)) {
            byte[] bytes = new byte[fileContents.length()];
            is.read(bytes);
            String actual = new String(bytes);
            Assert.assertEquals(actual, fileContents,
                    "Stream data doesn't match data written");
        }
    }

    @Test
    public void canGetAStreamingSeekableChannelWhenReadOnly() throws IOException {
        final String fileContents = "Hello World";
        String testFilePath = uploadTestFile("read_file_test", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);

        try (SeekableByteChannel channel = Files.newByteChannel(fileToRead,
                StandardOpenOption.READ)) {
            Assert.assertEquals(channel.getClass(), MantaSeekableByteChannel.class,
                    "Wrong class returned for channel");
            ByteBuffer buffer = ByteBuffer.allocate(fileContents.length());
            channel.read(buffer);
            String actual = new String(buffer.array());
            Assert.assertEquals(actual, fileContents,
                    "Stream data doesn't match data written");
        }
    }

    @Test
    public void canGetATempFileSeekableChannelWhenWriteEnabled() throws IOException {
        final String fileContents = "Hello World";
        String testFilePath = uploadTestFile("read_file_test", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);

        try (SeekableByteChannel channel = Files.newByteChannel(fileToRead,
                StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            Assert.assertEquals(channel.getClass(), MantaTempSeekableByteChannel.class,
                    "Wrong class returned for channel");
            ByteBuffer buffer = ByteBuffer.allocate(fileContents.length());
            channel.read(buffer);
            String actual = new String(buffer.array());
            Assert.assertEquals(actual, fileContents,
                    "Stream data doesn't match data written");
        }
    }

    @Test
    public void canOverwriteToMantaOverSeekableChannel() throws IOException {
        final String fileContents = "Hello World";
        final String newContents = "Foo Bar";
        String testFilePath = uploadTestFile("read_file_test", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);

        try (SeekableByteChannel channel = Files.newByteChannel(fileToRead,
                StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(newContents.getBytes());
            channel.write(buffer);
        }

        String actual = mantaClient.getAsString(testFilePath);
        Assert.assertEquals(actual, "Foo Barorld",
                "Contents were not overwritten");
    }

    @Test
    public void canAppendToMantaOverSeekableChannel() throws IOException {
        final String fileContents = "Hello World";
        final String newContents = "Foo Bar";
        String testFilePath = uploadTestFile("read_file_test", fileContents);

        Path fileToRead = fileSystem.getPath(testFilePath);

        try (SeekableByteChannel channel = Files.newByteChannel(fileToRead,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(newContents.getBytes());
            channel.write(buffer);
        }

        String actual = mantaClient.getAsString(testFilePath);
        Assert.assertEquals(actual, "Hello WorldFoo Bar",
                "Contents were not appended");
    }

    @Test
    public void createTempFile() throws IOException {
        Path dir = fileSystem.getPath(testDirectory);
        Path temp = Files.createTempFile(dir, "prefix", "suffix");

        // Will throw IOException if the file doesn't exist
        MantaObject head = mantaClient.head(temp.toString());
    }

    @Test
    void canCreateFile() throws IOException {
        String path = String.format("%s/%s", testDirectory, UUID.randomUUID());
        Path file = fileSystem.getPath(path);
        Files.createFile(file);

        MantaObject head = mantaClient.head(path);
    }

    @Test
    public void isFileMarkedAsFile() throws IOException, MantaException {
        String testFilePath = uploadTestFile("read_file_test", "Hello World");

        Path file = fileSystem.getPath(testFilePath);
        Assert.assertTrue(Files.isRegularFile(file),
                "This is a file and it should be marked as such");
    }

    @Test
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

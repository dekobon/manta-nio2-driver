package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import com.github.dekobon.manta.fs.provider.MantaFileSystemProvider;
import com.github.dekobon.manta.fs.provider.MantaFileSystemRepository;
import com.github.fge.filesystem.provider.FileSystemRepository;
import com.joyent.manta.exception.MantaException;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryTest {
    private final FileSystemRepository repository = new MantaFileSystemRepository();
    private final FileSystemProvider provider = new MantaFileSystemProvider(repository);
    private final FileSystem fileSystem;
    private final ConfigContext config = new SystemSettingsConfigContext();

    {
        try {
            URI uri = URI.create(String.format("manta://%s", config.getMantaUser()));
            fileSystem = provider.newFileSystem(uri, Collections.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(groups = { "directory"}, expectedExceptions = { AccessDeniedException.class })
    public void listRootDirectory() throws IOException {
        Path rootPath = fileSystem.getPath("/");
        List<String> listing = listPath(rootPath);

        listing.forEach(item -> System.out.println(item));
    }

    @Test(groups = { "directory"})
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

    @Test(groups = { "directory"}, expectedExceptions = { AccessDeniedException.class })
    public void listNonexistentDirectoryFromRoot() throws IOException {
        Path rootPath = fileSystem.getPath("/mymoneyisonthisnotbeinghere7");
        List<String> listing = listPath(rootPath);

        listing.forEach(item -> System.out.println(item));
    }

    @Test(groups = { "directory"}, expectedExceptions = { FileNotFoundException.class })
    public void listNonexistentDirectoryFromHome() throws IOException {
        Path rootPath = fileSystem.getPath("~~/mymoneyisonthisnotbeinghere2");
        List<String> listing = listPath(rootPath);

        listing.forEach(item -> System.out.println(item));
    }

    @Test(groups = { "directory" })
    public void isDirectoryMarkedAsDirectory() {
        Path stor = fileSystem.getPath("~~/stor");
        Assert.assertTrue(Files.isDirectory(stor),
                "This is a directory and it should be marked as such");
    }

    @Test(groups = { "directory" })
    public void verifyLastModifiedTime() throws IOException, MantaException {
        Path file = fileSystem.getPath("~~/stor");
        FileTime time = Files.getLastModifiedTime(file);

        // TODO: Actually create a directory and check the time
        Assert.assertNotNull(time);
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

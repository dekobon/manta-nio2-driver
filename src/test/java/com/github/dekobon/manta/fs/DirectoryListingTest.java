package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import com.github.dekobon.manta.fs.provider.MantaFileSystemProvider;
import com.github.dekobon.manta.fs.provider.MantaFileSystemRepository;
import com.github.fge.filesystem.provider.FileSystemRepository;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryListingTest {
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

    @Test(expectedExceptions = { FileNotFoundException.class })
    public void listNonexistentDirectoryFromHome() throws IOException {
        Path rootPath = fileSystem.getPath("~~/mymoneyisonthisnotbeinghere2");
        List<String> listing = listPath(rootPath);

        listing.forEach(item -> System.out.println(item));
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

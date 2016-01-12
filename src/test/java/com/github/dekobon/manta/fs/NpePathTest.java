package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import com.github.dekobon.manta.fs.provider.MantaFileSystemProvider;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;

public class NpePathTest {
    private FileSystemProvider provider = new MantaFileSystemProvider();
    private FileSystem fileSystem;

    @BeforeClass
    public void setup() {
        try {
            ConfigContext config = new SystemSettingsConfigContext();
            URI uri = URI.create(String.format("manta://%s", config.getMantaUser()));
            fileSystem = provider.newFileSystem(uri, Collections.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public void cleanUp() {
        try {
            fileSystem.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void cantResolveNullPath() {
        Path path = fileSystem.getPath("foo");
        path.resolve((String)null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void cantRelativizeNullPath() {
        Path path = fileSystem.getPath("foo");
        path.relativize(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void cantCompareToNullPath() {
        Path path = fileSystem.getPath("foo");
        path.compareTo(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void cantStartsWithNullPath() {
        Path path = fileSystem.getPath("foo");
        path.startsWith((Path)null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void cantEndsWithNullPath() {
        Path path = fileSystem.getPath("foo");
        path.endsWith((Path)null);
    }
}

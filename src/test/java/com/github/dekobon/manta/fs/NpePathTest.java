package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;

public class NpePathTest {
    private FileSystem fileSystem;

    @BeforeClass
    public void setup() throws IOException {
        ConfigContext config = new SystemSettingsConfigContext();
        URI uri = ConfigContext.mantaURIFromContext(config);
        fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
    }

    @AfterClass
    public void cleanUp() throws IOException {
        if (fileSystem != null) {
            fileSystem.close();
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

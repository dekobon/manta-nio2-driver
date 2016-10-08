package com.joyent.manta.fs;

import com.joyent.manta.fs.config.ConfigContext;
import com.joyent.manta.fs.config.SystemSettingsConfigContext;
import com.joyent.manta.fs.provider.MantaFileSystemProvider;
import org.testng.Assert;
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
import java.util.Iterator;

public class MantaPathTest {
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

    @Test
    public void canIterateAbsolutePath() {
        Path path = fileSystem.getPath("/user/stor/a/b/c");

        Iterator<Path> iterator = path.iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("/"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("/user"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("/user/stor"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("/user/stor/a"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("/user/stor/a/b"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("/user/stor/a/b/c"));

        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void canIterateRelativePath() {
        Path path = fileSystem.getPath("user/stor/a/b/c");

        Iterator<Path> iterator = path.iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("user"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("user/stor"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("user/stor/a"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("user/stor/a/b"));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(iterator.next(), fileSystem.getPath("user/stor/a/b/c"));

        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void compareToSamePath() {
        Path path1 = fileSystem.getPath("/user/stor/a/b/c");
        Path path2 = fileSystem.getPath("/user/stor/a/b/c");

        Assert.assertEquals(path1.compareTo(path2), 0);
        Assert.assertEquals(path1, path2);
    }

    @Test
    public void compareToDifferentPath() {
        Path path1 = fileSystem.getPath("/user/stor/a/b/c");
        Path path2 = fileSystem.getPath("/user/stor/a/b");

        Assert.assertEquals(path1.compareTo(path2), 2);
        Assert.assertNotEquals(path1, path2);
    }

    @Test
    public void compareToAnAssortmentOfEquivalentPaths() {
        Path original = fileSystem.getPath("/user/stor/a/b/c");

        Path path1 = fileSystem.getPath("/user/stor/a/b/c/");
        Assert.assertEquals(original.compareTo(path1), 0,
                String.format("Original: [%s], Comparison: [%s]", original, path1));

        Path path2 = fileSystem.getPath("user/stor/a/b/c");
        Assert.assertNotEquals(original.compareTo(path2), 0,
                String.format("Original: [%s], Comparison: [%s]", original, path2));

        Path path3 = fileSystem.getPath("//user/stor/a/b/c");
        Assert.assertEquals(original.compareTo(path3), 0,
                String.format("Original: [%s], Comparison: [%s]", original, path3));
    }

    @Test
    public void relativePathDoesntCompareToAbsolutePath() {
        Path original = fileSystem.getPath("/user/stor/a/b/c");

        Path path = fileSystem.getPath("user/stor/a/b/c");
        Assert.assertNotEquals(original.compareTo(path), 0,
                String.format("Original: [%s], Comparison: [%s]", original, path));
    }

    @Test
    public void equalsToAnAssortmentOfEquivalentPaths() {
        Path original = fileSystem.getPath("/user/stor/a/b/c");

        Path path1 = fileSystem.getPath("/user/stor/a/b/c/");
        Assert.assertEquals(path1, original,
                String.format("Original: [%s], Comparison: [%s]", original, path1));

        Path path2 = fileSystem.getPath("//user/stor/a/b/c");
        Assert.assertEquals(path2, original,
                String.format("Original: [%s], Comparison: [%s]", original, path2));
    }

    @Test
    public void relativePathIsNotEqualToAbsolutePath() {
        Path original = fileSystem.getPath("/user/stor/a/b/c");

        Path path = fileSystem.getPath("user/stor/a/b/c");
        Assert.assertNotEquals(path, original,
                String.format("Original: [%s], Comparison: [%s]", original, path));
    }
}

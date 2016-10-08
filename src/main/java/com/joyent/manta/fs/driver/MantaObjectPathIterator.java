package com.joyent.manta.fs.driver;

import com.joyent.manta.client.MantaDirectoryListingIterator;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaObjectPathIterator implements Iterator<Path>, Closeable {
    protected final Path dir;
    protected final String realDirPath;
    protected final MantaFileSystemDriver driver;
    protected final MantaDirectoryListingIterator internalIterator;

    public MantaObjectPathIterator(final Path dir,
                                   final MantaFileSystemDriver driver,
                                   final MantaDirectoryListingIterator iterator)
            throws IOException {
        this.dir = dir;
        this.driver = driver;
        this.realDirPath = driver.findRealPath(dir);
        this.internalIterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return internalIterator.hasNext();
    }

    @Override
    public Path next() {
        Map<String, Object> properties = internalIterator.next();

        if (properties == null) {
            return null;
        }

        final String name = Objects.toString(properties.get("name"));

        if (properties == null) {
            return null;
        }

        final String objPath = String.format("%s/%s", realDirPath, name);

        return dir.resolve(objPath);
    }

    @Override
    public void close() throws IOException {
        internalIterator.close();
    }
}

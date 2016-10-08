package com.joyent.manta.fs.driver;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaDirectoryListingIterator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * {@link DirectoryStream} implementation that wraps a {@link MantaObjectPathIterator}
 * in order to provide a streaming memory efficient interface to Manta directory
 * listings.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaDirectoryStream implements DirectoryStream<Path> {
    private final Path mantaPath;
    private final MantaObjectPathIterator iterator;

    public MantaDirectoryStream(final Path dirPath,
                                final MantaClient mantaClient,
                                final MantaFileSystemDriver driver) throws IOException {
        this.mantaPath = dirPath;
        String realPath = driver.findRealPath(dirPath);
        MantaDirectoryListingIterator mantaIterator = mantaClient.streamingIterator(realPath);
        this.iterator = new MantaObjectPathIterator(mantaPath, driver, mantaIterator);
    }

    @Override
    public Iterator<Path> iterator() {
        return iterator;
    }

    @Override
    public void close() throws IOException {
        iterator.close();
    }
}

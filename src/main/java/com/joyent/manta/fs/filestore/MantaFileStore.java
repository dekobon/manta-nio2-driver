package com.joyent.manta.fs.filestore;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.filestore.FileStoreBase;
import com.joyent.manta.client.MantaClient;

import java.io.IOException;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileStore extends FileStoreBase {
    protected final MantaClient mantaClient;

    public MantaFileStore(final MantaClient mantaClient,
                          final FileAttributesFactory factory) {
        super("manta", factory, false);
        this.mantaClient = mantaClient;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return Long.MAX_VALUE;
    }

    @Override
    public long getUsableSpace() throws IOException {
        return Long.MAX_VALUE;
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        return Long.MAX_VALUE;
    }
}

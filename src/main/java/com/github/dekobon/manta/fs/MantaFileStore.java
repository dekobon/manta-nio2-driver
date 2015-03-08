package com.github.dekobon.manta.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * Mostly hard-coded file store implementation for Manta.
 *
 * Many of the properties defined here aren't applicable to Manta, so we
 * return back our best guesses.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileStore extends FileStore {
    public MantaFileStore() {
    }

    @Override
    public String name() {
        return "manta";
    }

    @Override
    public String type() {
        return "remote";
    }

    @Override
    public boolean isReadOnly() {
        return false;
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
        return -1;
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return false;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        return false;
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

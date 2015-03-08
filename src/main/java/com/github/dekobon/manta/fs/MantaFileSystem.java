package com.github.dekobon.manta.fs;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.exception.MantaCryptoException;
import com.joyent.manta.exception.MantaObjectException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Manta implementation of a NIO.2 {@link java.nio.file.FileSystem}.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystem extends FileSystem {
    public static final MantaFileStore FILE_STORE =
            new MantaFileStore();

    private final URI uri;
    private final MantaClient mantaClient;
    private final MantaFileSystemProvider fileSystemProvider;

    public MantaFileSystem(URI uri,
                           MantaClient mantaClient,
                           MantaFileSystemProvider fileSystemProvider) {
        this.uri = uri;
        this.mantaClient = mantaClient;
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public FileSystemProvider provider() {
        return fileSystemProvider;
    }

    @Override
    public void close() throws IOException {
        fileSystemProvider.fileSystemCache.remove(uri);
    }

    @Override
    public boolean isOpen() {
        return fileSystemProvider.fileSystemCache.containsKey(uri);
    }

    @Override
    public boolean isReadOnly() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        try {
            Collection<MantaObject> rootList = mantaClient.listObjects("/");
            Collection<Path> paths = new ArrayList<>(rootList.size());

            for (MantaObject obj : rootList) {
                MantaPath mantaPath = new MantaPath(obj.getPath(), this,
                        mantaClient);
                mantaPath.setMantaObject(obj);
                paths.add(mantaPath);
            }

            return paths;
        } catch (MantaObjectException | MantaCryptoException |
                 MantaClientHttpResponseException | IOException e) {
            throw new MantaRuntimeException(
                    "Error getting root directory listing from Manta", e);
        }
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        // This is anonymous because I don't have a good immutable library
        return new Iterable<FileStore>() {
            @Override
            public Iterator<FileStore> iterator() {
                return new Iterator<FileStore>() {
                    private volatile boolean first = false;

                    @Override
                    public boolean hasNext() {
                        return !first;
                    }

                    @Override
                    public synchronized FileStore next() {
                        if (!first) {
                            first = true;
                            return FILE_STORE;
                        }

                        return null;
                    }
                };
            }
        };
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Path getPath(String first, String... more) {
        return new MantaPath(first, this, mantaClient);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

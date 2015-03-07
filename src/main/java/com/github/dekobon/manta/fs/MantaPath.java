package com.github.dekobon.manta.fs;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.exception.MantaCryptoException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaPath implements Path {
    private final String objectPath;
    private final MantaFileSystem fileSystem;
    private final MantaClient mantaClient;
    private volatile MantaObject mantaObject;

    public MantaPath(String objectPath,
            MantaFileSystem fileSystem,
            MantaClient mantaClient) {
        if (objectPath == null) throw new IllegalArgumentException(
                "Object path must not be null");
        this.objectPath = objectPath;
        this.fileSystem = fileSystem;
        this.mantaClient = mantaClient;
    }

    @Override
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return false;
    }

    @Override
    public Path getRoot() {
        return null;
    }

    @Override
    public Path getFileName() {
        return null;
    }

    @Override
    public Path getParent() {
        return null;
    }

    @Override
    public int getNameCount() {
        return 0;
    }

    @Override
    public Path getName(int index) {
        return null;
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return null;
    }

    @Override
    public boolean startsWith(Path other) {
        return false;
    }

    @Override
    public boolean startsWith(String other) {
        return false;
    }

    @Override
    public boolean endsWith(Path other) {
        return false;
    }

    @Override
    public boolean endsWith(String other) {
        return false;
    }

    @Override
    public Path normalize() {
        return null;
    }

    @Override
    public Path resolve(Path other) {
        return null;
    }

    @Override
    public Path resolve(String other) {
        return null;
    }

    @Override
    public Path resolveSibling(Path other) {
        return null;
    }

    @Override
    public Path resolveSibling(String other) {
        return null;
    }

    @Override
    public Path relativize(Path other) {
        return null;
    }

    @Override
    public URI toUri() {
        return URI.create(
                String.format("%s://%s",
                        MantaFileSystemProvider.SCHEME,
                        objectPath)
        );
    }

    @Override
    public Path toAbsolutePath() {
        return null;
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public File toFile() {
        return getMantaObject().getDataInputFile();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Iterator<Path> iterator() {
        return null;
    }

    @Override
    public int compareTo(Path other) {
        return 0;
    }

    public synchronized MantaObject getMantaObject() {
        if (mantaObject == null) {
            try {
                return mantaClient.get(objectPath);
            } catch (IOException |
                     MantaCryptoException |
                     MantaClientHttpResponseException e) {
                throw new MantaRuntimeException("Error getting Manta object", e);
            }
        } else {
            return mantaObject;
        }
    }

    public void setMantaObject(MantaObject mantaObject) {
        this.mantaObject = mantaObject;
    }
}

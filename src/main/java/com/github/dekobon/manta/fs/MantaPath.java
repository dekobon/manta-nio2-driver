package com.github.dekobon.manta.fs;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.exception.MantaCryptoException;
import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Objects;

/**
 * Implementation of {@link java.nio.file.Path} that is backed by properties
 * relevant to a Manta object.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaPath implements Path {
    private final char separatorChar;
    private final String separator;
    private final String objectPath;
    private final MantaFileSystem fileSystem;
    private final MantaClient mantaClient;
    private volatile MantaObject mantaObject;

    public MantaPath(String first, MantaFileSystem fileSystem, MantaClient mantaClient, String... more) {
        if (first == null) throw new IllegalArgumentException(
                "Object path must not be null");

        this.fileSystem = fileSystem;
        this.mantaClient = mantaClient;
        this.separatorChar = fileSystem == null ?
                '/' : fileSystem.getSeparator().charAt(0);
        this.separator = new String(new char[] { separatorChar });
        this.objectPath = buildObjectPath(first, more);
    }

    protected String buildObjectPath(String first, String... more) {
        if (more == null || more.length == 0) {
            return first;
        }

        // Don't pre add first if it is the separator, otherwise we will get a path like //foo
        final StringBuilder builder;

        if (first.equals(separator)) {
            builder = new StringBuilder();
        } else {
            builder = new StringBuilder(first);
        }

        for (int i = 0; i < more.length; i++) {
            final String part = more[i];

            if (part == null) continue;
            if (part.isEmpty()) continue;
            if (part.equals(this.separator)) continue;

            String normalized = FilenameUtils.normalizeNoEndSeparator(part, true);

            boolean emptyFirstValue = i == 0 && first.isEmpty();
            boolean hasLeadingSeparator = normalized.charAt(0) == separatorChar;

            if (!hasLeadingSeparator && !emptyFirstValue) {
                builder.append(this.separatorChar);
            }

            builder.append(normalized);
        }

        String builtPath = builder.toString();

        // If everything is empty, then we assume we are getting the root directory
        if (first.isEmpty() && builtPath.isEmpty()) {
            return separator;
        // When we've excluded all of the possible extra parts, we can't append double separators together,
        // so we just return the value of first.
        } else if (builtPath.isEmpty()) {
            return first;
        } else {
            return builtPath;
        }
    }

    @Override
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return !objectPath.isEmpty() && objectPath.charAt(0) == separatorChar;
    }

    @Override
    public Path getRoot() {
        if (!objectPath.isEmpty() && objectPath.charAt(0) == separatorChar) {
            return fileSystem.rootDirectory();
        } else {
            return null;
        }
    }

    @Override
    public Path getFileName() {
        // There is no filename available for the root directory
        if (objectPath.equals(fileSystem.rootDirectory().toString())) return null;

        String normalized = FilenameUtils.normalizeNoEndSeparator(objectPath, true);
        String fileName = FilenameUtils.getBaseName(normalized);
        return new MantaPath(fileName, fileSystem, mantaClient);
    }

    @Override
    public Path getParent() {
        // The root directory doesn't have a parent
        if (objectPath.equals(fileSystem.rootDirectory().toString())) return null;
        // We imitate the behavior of UnixPath in these cases
        if (objectPath.equals(".") || objectPath.equals("..")) return null;

        String normalized = FilenameUtils.normalizeNoEndSeparator(objectPath, true);
        String parent = FilenameUtils.getFullPathNoEndSeparator(normalized);

        return new MantaPath(parent, fileSystem, mantaClient);
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
        final String resolve = resolveObjectPath(objectPath);
        final String normalized = FilenameUtils.normalizeNoEndSeparator(resolve, true);

        return new MantaPath(normalized, fileSystem, mantaClient);
    }

    @Override
    public Path resolve(Path other) {
        return resolve(objectPath);
    }

    @Override
    public Path resolve(String other) {
        return null;
    }

    protected String resolveObjectPath(String objectPath) {
        switch (objectPath) {
            case "/":
                return "/";
            case "..":
                return "/";
            case ".":
                return "/";
        }

        String[] parts = objectPath.split(separator);
        StringBuilder builder = new StringBuilder();

        boolean trailingSeparator = objectPath.endsWith(separator);
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            final String lastPart = i > 0 ? parts[i-1] : "";

            if (part.isEmpty()) continue;
            if (part.equals(".")) continue;
            if (part.equals("..") && i == 0) continue;
            if (part.equals("..") && builder.length() == 0) continue;

            if (part.equals("..")) {
                final int start = builder.length() - lastPart.length() - 1;
                final int end = builder.length();
                builder.delete(start, end);
                continue;
            }

            builder.append(separator);
            builder.append(part);
        }

        if (trailingSeparator) builder.append(separator);
        if (builder.length() == 0) builder.append(separator);

        return builder.toString();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MantaPath paths = (MantaPath) o;
        return Objects.equals(objectPath, paths.objectPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectPath);
    }

    public String toString() {
        return objectPath;
    }
}

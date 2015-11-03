package com.github.dekobon.manta.fs;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.exception.MantaCryptoException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
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
        validatePath(objectPath);
    }

    protected String buildObjectPath(String first, String... more) {
        if (more == null || more.length == 0) {
            return normalizeObjectPath(first);
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
            return normalizeObjectPath(first);
        } else {
            return builtPath;
        }
    }

    /**
     * Checks a given path string to see if it contains forbidden characters.
     * @param aObjectPath path string to check for invalid characters
     * @throws InvalidPathException thrown when an invalid character is detected
     */
    protected static void validatePath(String aObjectPath) {
        final int badCharPos = aObjectPath.indexOf("\u0000");
        if (badCharPos >= 0) {
            throw new InvalidPathException(aObjectPath,
                    "Path included character \\u0000", badCharPos);
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

        String normalized = normalizeObjectPath(objectPath);
        String fileName = FilenameUtils.getBaseName(normalized);
        return new MantaPath(fileName, fileSystem, mantaClient);
    }

    @Override
    public Path getParent() {
        // The root directory doesn't have a parent
        if (objectPath.equals(fileSystem.rootDirectory().toString())) return null;
        // We imitate the behavior of UnixPath in these cases
        if (objectPath.equals(".") || objectPath.equals("..")) return null;
        // There is no parent when we have an empty path
        if (objectPath.isEmpty()) return null;

        String[] subPaths = subPaths();

        if (subPaths.length < 2) return null;

        String normalized = normalizeObjectPath(objectPath);
        String parent = FilenameUtils.getFullPathNoEndSeparator(normalized);

        return new MantaPath(parent, fileSystem, mantaClient);
    }

    @Override
    public int getNameCount() {
        if (this.equals(fileSystem.rootDirectory())) return 0;

        return subPaths().length;
    }

    @Override
    public Path getName(int index) {
        if (index < 0) throw new IllegalArgumentException("index must be above 0");
        String[] subPaths = subPaths();
        if (index > subPaths.length) throw new IllegalArgumentException(
                "index must not be greater than the number of elements available");

        String namePath = subPaths.length > 0 ? subPaths[index] : "";

        return new MantaPath(namePath, fileSystem, mantaClient);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        if (beginIndex < 0) throw new IllegalArgumentException(
                "beginIndex must be above 0");

        if (endIndex < 0) throw new IllegalArgumentException(
                "endIndex must be above 0");

        String[] subPaths = subPaths();

        if (beginIndex > subPaths.length) throw new IllegalArgumentException(
                "beginIndex is greater than the number of elements available");

        if (endIndex <= beginIndex) throw new IllegalArgumentException(
                "endIndex must be less or equal to beginIndex");

        // On the edge case of an subpathing an empty path, return self
        if (objectPath.isEmpty() && beginIndex == 0 && endIndex == 1) {
            return this;
        }

        if (endIndex > subPaths.length) {
            throw new IllegalArgumentException(
                    "endIndex must not be greater than the number of elements available");
        }

        StringBuilder builder = new StringBuilder();

        for (int i = beginIndex; i < endIndex; i++) {
            String part = subPaths[i];

            builder.append(part);

            if (i < endIndex - 1) {
                builder.append(separator);
            }
        }

        return new MantaPath(builder.toString(), fileSystem, mantaClient);
    }

    protected String[] subPaths() {
        if (objectPath == null) return null;

        return StringUtils.split(objectPath, separatorChar);
    }

    @Override
    public boolean startsWith(Path other) {
        if (other instanceof MantaPath) {
            MantaPath otherMantaPath = (MantaPath)other;
            return startsWith(otherMantaPath.objectPath);
        } else {
            return startsWith(other.toString());
        }
    }

    @Override
    public boolean startsWith(String other) {
        if (objectPath.isEmpty() && other.isEmpty()) return true;
        if (objectPath.isEmpty() && other.equals(separator)) return false;
        if (other == null || other.isEmpty()) return false;

        String resolveOther = resolveObjectPath(other);
        String resolveOtherWithSeparator = resolveOther.endsWith(separator) ?
                resolveOther : resolveOther + separator;
        String resolve = resolveObjectPath(objectPath);
        String resolveWithSeparator = resolve.endsWith(separator) ?
                resolve : resolve + separator;

        return resolveWithSeparator.startsWith(resolveOtherWithSeparator);
    }

    @Override
    public boolean endsWith(Path other) {
        return endsWith(other.toString());
    }

    @Override
    public boolean endsWith(String other) {
        if (objectPath.isEmpty() && other.isEmpty()) return true;
        if (objectPath.isEmpty() && other.equals(separator)) return false;
        if (other == null || other.isEmpty()) return false;

        /* Just do a simple equality if we have a fully specified path on other
         * Example:
         *   objectPath: /foo/bar -> other: /bar => false
         *   objectPath: /foo/bar -> other: /foo/bar => true
         */
        if (other.startsWith(separator)) {
            return objectPath.equals(other);
        }

        String[] subPaths = subPaths();
        String[] otherPaths = StringUtils.split(other, separatorChar);

        int sizeDiff = subPaths.length - otherPaths.length;

        // if the other sizes are greater than the subpaths, it's false
        if (sizeDiff < 0) return false;

        // if there are no path separators, just do a normal ends with [foo, foo]
        if (subPaths.length == 0 && otherPaths.length == 0) return objectPath.endsWith(other);

        // If there were no path separators in other, but in objectPath [/foo/bar, bar]
        if (subPaths.length >= 0 && otherPaths.length == 0) {
            return subPaths[subPaths.length-1].endsWith(other);
        }

        int subPathsIndex = subPaths.length - 1;
        int otherPathsIndex = otherPaths.length - 1;

        while (subPathsIndex >= 0 && otherPathsIndex >= 0) {
            final String lastSubPath = subPaths[subPathsIndex];
            final String lastOtherPath = otherPaths[otherPathsIndex];

            if (!lastSubPath.equals(lastOtherPath)) return false;

            subPathsIndex--;
            otherPathsIndex--;
        }

        return true;
    }

    @Override
    public Path normalize() {
        String normalized = normalizeObjectPath(objectPath);
        return new MantaPath(normalized, fileSystem, mantaClient);
    }

    protected String normalizeObjectPath(String aObjectPath) {
        final String resolve;

        if (!aObjectPath.isEmpty() && aObjectPath.charAt(0) == separatorChar) {
            resolve = resolveObjectPath(aObjectPath);
        } else {
            resolve = aObjectPath;
        }

        String normalized = FilenameUtils.normalizeNoEndSeparator(resolve, true);

        if (normalized == null) {
            return FilenameUtils.normalizeNoEndSeparator(resolveObjectPath(aObjectPath), true);
        } else {
            return normalized;
        }

    }

    @Override
    public Path resolve(Path other) {
        if (other instanceof MantaPath) {
            MantaPath otherMantaPath = (MantaPath)other;
            return resolve(otherMantaPath.objectPath);
        } else {
            return resolve(other.toString());
        }
    }

    @Override
    public Path resolve(String other) {
        final String resolved;

        if (objectPath.isEmpty() && other.isEmpty()) {
            resolved = "";
        } else if (objectPath.isEmpty() && other.charAt(0) != separatorChar) {
            resolved = resolveObjectPath(other);
        } else if (objectPath.isEmpty()) {
            resolved = resolveObjectPath(other);
        } else if (other.isEmpty()) {
            resolved = resolveObjectPath(objectPath);
        } else if (other.charAt(0) != separatorChar) {
            resolved = resolveObjectPath(objectPath + separator + other);
        } else {
            resolved = resolveObjectPath(other);
        }

        return new MantaPath(resolved, fileSystem, mantaClient);
    }

    protected String resolveObjectPath(String objectPath) {
        // We don't include this in the switch because separator is not static final
        if (objectPath.equals(separator)) return separator;

        switch (objectPath) {
            case "":
                return "";
            case "..":
                return separator;
            case ".":
                return separator;
        }

        String[] parts = objectPath.split(separator);
        StringBuilder builder = new StringBuilder();

        boolean startingSeparator = objectPath.startsWith(separator);
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

            if (builder.length() == 0 && startingSeparator) {
                builder.append(separator);
            } else if (builder.length() > 0){
                builder.append(separator);
            }

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
        if (objectPath.isEmpty()) return other;

        String otherPath = other.toString();

        String relativePath = null;

        // Two equal paths
        if (objectPath.equals(otherPath)) {
            relativePath = "";
        // Handles two absolute paths
        } else if (objectPath.startsWith(separator) && otherPath.startsWith(separator)) {
            String diff = StringUtils.removeStart(otherPath, objectPath);
            relativePath = StringUtils.removeStart(diff, separator);
        } else {
            String[] subPaths = subPaths();
            //String[] otherPaths =

        }

        return new MantaPath(relativePath, fileSystem, mantaClient);
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
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events,
                             WatchEvent.Modifier... modifiers) throws IOException {
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

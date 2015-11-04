package com.github.dekobon.manta.fs.provider;

import com.github.fge.filesystem.provider.FileSystemProviderBase;
import com.github.fge.filesystem.provider.FileSystemRepository;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystemProvider extends FileSystemProviderBase {
    public MantaFileSystemProvider(FileSystemRepository repository) {
        super(repository);
    }
}

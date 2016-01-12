package com.github.dekobon.manta.fs.provider;

import com.github.dekobon.manta.fs.attributes.MantaFileAttributesFactory;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystemFactoryProvider extends FileSystemFactoryProvider {
    public MantaFileSystemFactoryProvider() {
        setAttributesFactory(new MantaFileAttributesFactory());
    }
}

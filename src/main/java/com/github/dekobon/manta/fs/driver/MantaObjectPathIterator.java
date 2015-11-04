package com.github.dekobon.manta.fs.driver;

import com.joyent.manta.client.MantaObject;

import java.nio.file.Path;
import java.util.Iterator;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaObjectPathIterator implements Iterator<Path> {
    protected final Path dir;
    protected final Iterator<MantaObject> internalIterator;

    public MantaObjectPathIterator(final Path dir,
                                   final Iterable<MantaObject> mantaObjects) {
        this.internalIterator = mantaObjects.iterator();
        this.dir = dir;
    }

    public MantaObjectPathIterator(final Path dir,
                                   final Iterator<MantaObject> iterator) {
        this.internalIterator = iterator;
        this.dir = dir;
    }

    @Override
    public boolean hasNext() {
        return internalIterator.hasNext();
    }

    @Override
    public Path next() {
        MantaObject mantaObject = internalIterator.next();
        if (mantaObject == null) {
            return null;
        }

        return dir.resolve(mantaObject.getPath());
    }
}

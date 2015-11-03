package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import com.joyent.manta.client.MantaClient;
import org.junit.Assert;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Adapted from: http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/tip/test/java/nio/file/Path/PathOps.java
 */

/* Test code in this class is under the following license:
 *
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
public class PathOps {
    static final java.io.PrintStream out = System.out;

    private String input;
    private Path path;
    private Exception exc;

    private PathOps(String first, String... more) {
        out.println();
        input = first;
        try {
            ConfigContext config = new SystemSettingsConfigContext();
            MantaFileSystemProvider provider = new MantaFileSystemProvider(config);
            URI mantaPath = URI.create(String.format("manta://%s/stor", config.getMantaUser()));
            MantaFileSystem fileSystem = (MantaFileSystem) provider.newFileSystem(mantaPath, null);
            MantaClient client = MantaClient.newInstance(config);
            path = new MantaPath(first, fileSystem, null, more);
        } catch (InvalidPathException e) {
            exc = e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        out.format("%s -> %s", first, path);

        out.println();
    }

    Path path() {
        return path;
    }

    void fail() {
        throw new RuntimeException("PathOps failed");
    }

    void checkPath() {
        if (path == null) {
            throw new InternalError("path is null");
        }
    }

    void check(Object result, String expected) {
        if (expected == null) {
            Assert.assertNull(result);
        } else {
            String msg = String.format("Expected: [%s] Actually: [%s]", expected, result);
            Assert.assertNotNull(msg, result);
            Assert.assertEquals(expected, result.toString());
        }
    }

    void check(Object result, boolean expected) {
        check(result, Boolean.toString(expected));
    }

    PathOps root(String expected) {
        out.println("check root");
        checkPath();
        check(path.getRoot(), expected);
        return this;
    }

    PathOps parent(String expected) {
        out.println("check parent");
        checkPath();
        check(path.getParent(), expected);
        return this;
    }

    PathOps name(String expected) {
        out.println("check name");
        checkPath();
        check(path.getFileName(), expected);
        return this;
    }

    PathOps element(int index, String expected) {
        out.format("check element %d\n", index);
        checkPath();
        check(path.getName(index), expected);
        return this;
    }

    PathOps subpath(int startIndex, int endIndex, String expected) {
        out.format("test subpath(%d,%d)\n", startIndex, endIndex);
        checkPath();
        check(path.subpath(startIndex, endIndex), expected);
        return this;
    }

    PathOps starts(String prefix) {
        out.format("test startsWith with %s\n", prefix);
        checkPath();
        Path s = FileSystems.getDefault().getPath(prefix);
        check(path.startsWith(s), true);
        return this;
    }

    PathOps notStarts(String prefix) {
        out.format("test not startsWith with %s\n", prefix);
        checkPath();
        Path s = FileSystems.getDefault().getPath(prefix);
        check(path.startsWith(s), false);
        return this;
    }

    PathOps ends(String suffix) {
        out.format("test endsWith %s\n", suffix);
        checkPath();
        Path s = FileSystems.getDefault().getPath(suffix);
        check(path.endsWith(s), true);
        return this;
    }

    PathOps notEnds(String suffix) {
        out.format("test not endsWith %s\n", suffix);
        checkPath();
        Path s = FileSystems.getDefault().getPath(suffix);
        check(path.endsWith(s), false);
        return this;
    }

    PathOps absolute() {
        out.println("check path is absolute");
        checkPath();
        check(path.isAbsolute(), true);
        return this;
    }

    PathOps notAbsolute() {
        out.println("check path is not absolute");
        checkPath();
        check(path.isAbsolute(), false);
        return this;
    }

    PathOps resolve(String other, String expected) {
        out.format("test resolve %s\n", other);
        checkPath();
        check(path.resolve(other), expected);
        return this;
    }

    PathOps resolveSibling(String other, String expected) {
        out.format("test resolveSibling %s\n", other);
        checkPath();
        check(path.resolveSibling(other), expected);
        return this;
    }

    PathOps relativize(String other, String expected) {
        out.format("test relativize %s\n", other);
        checkPath();
        Path that = FileSystems.getDefault().getPath(other);
        check(path.relativize(that), expected);
        return this;
    }

    PathOps normalize(String expected) {
        out.println("check normalized path");
        checkPath();
        check(path.normalize(), expected);
        return this;
    }

    PathOps string(String expected) {
        out.println("check string representation");
        checkPath();
        check(path, expected);
        return this;
    }

    PathOps invalid() {
        if (!(exc instanceof InvalidPathException)) {
            out.println("InvalidPathException not thrown as expected");
            fail();
        }
        return this;
    }

    static PathOps test(String first, String... more) {
        return new PathOps(first, more);
    }

    // -- PathOpss --

    static void header(String s) {
        out.println();
        out.println();
        out.println("-- " + s + " --");
    }

    static void npes() {
        header("NullPointerException");

        Path path = FileSystems.getDefault().getPath("foo");

        try {
            path.resolve((String)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException npe) {
        }

        try {
            path.relativize(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException npe) {
        }

        try {
            path.compareTo(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException npe) {
        }

        try {
            path.startsWith((Path)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException npe) {
        }

        try {
            path.endsWith((Path)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException npe) {
        }

    }
}

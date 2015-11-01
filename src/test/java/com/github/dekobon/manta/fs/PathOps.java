package com.github.dekobon.manta.fs;

import org.junit.Assert;
import org.junit.Test;

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
        path = new MantaPath(first, null, null, more);
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

    static void doUnixTests() {
        // no root component
        test("a/b")
                .root(null)
                .parent("a")
                .name("b");

        // name component only
        test("foo")
                .root(null)
                .parent(null)
                .name("foo");
        test("")
                .root(null)
                .parent(null)
                .name("");

        // startsWith
        test("/")
                .starts("/")
                .notStarts("")
                .notStarts("/foo");
        test("/foo")
                .starts("/")
                .starts("/foo")
                .notStarts("/f");
        test("/foo/bar")
                .starts("/")
                .starts("/foo")
                .starts("/foo/bar")
                .notStarts("/f")
                .notStarts("foo")
                .notStarts("foo/bar");
        test("foo")
                .starts("foo")
                .notStarts("")
                .notStarts("f");
        test("foo/bar")
                .starts("foo")
                .starts("foo/bar")
                .notStarts("f")
                .notStarts("/foo")
                .notStarts("/foo/bar");
        test("")
                .starts("")
                .notStarts("/");

        // endsWith
        test("/")
                .ends("/")
                .notEnds("")
                .notEnds("foo")
                .notEnds("/foo");
        test("/foo")
                .ends("foo")
                .ends("/foo")
                .notEnds("fool");
        test("/foo/bar")
                .ends("bar")
                .ends("foo/bar")
                .ends("/foo/bar")
                .notEnds("ar")
                .notEnds("barack")
                .notEnds("/bar")
                .notEnds("o/bar");
        test("foo")
                .ends("foo")
                .notEnds("")
                .notEnds("oo")
                .notEnds("oola");
        test("foo/bar")
                .ends("bar")
                .ends("foo/bar")
                .notEnds("r")
                .notEnds("barmaid")
                .notEnds("/bar");
        test("foo/bar/gus")
                .ends("gus")
                .ends("bar/gus")
                .ends("foo/bar/gus")
                .notEnds("g")
                .notEnds("/gus")
                .notEnds("r/gus")
                .notEnds("barack/gus")
                .notEnds("bar/gust");
        test("")
                .ends("")
                .notEnds("/");

        // elements
        test("a/b/c")
                .element(0, "a")
                .element(1, "b")
                .element(2, "c");
        test("")
                .element(0, "");

        // subpath
        test("/foo")
                .subpath(0, 1, "foo");
        test("foo")
                .subpath(0, 1, "foo");
        test("/foo/bar")
                .subpath(0, 1, "foo")
                .subpath(1, 2, "bar")
                .subpath(0, 2, "foo/bar");
        test("foo/bar")
                .subpath(0, 1, "foo")
                .subpath(1, 2, "bar")
                .subpath(0, 2, "foo/bar");
        test("/foo/bar/gus")
                .subpath(0, 1, "foo")
                .subpath(1, 2, "bar")
                .subpath(2, 3, "gus")
                .subpath(0, 2, "foo/bar")
                .subpath(1, 3, "bar/gus")
                .subpath(0, 3, "foo/bar/gus");
        test("foo/bar/gus")
                .subpath(0, 1, "foo")
                .subpath(1, 2, "bar")
                .subpath(2, 3, "gus")
                .subpath(0, 2, "foo/bar")
                .subpath(1, 3, "bar/gus")
                .subpath(0, 3, "foo/bar/gus");
        test("")
                .subpath(0, 1, "");

        // isAbsolute
        test("/")
                .absolute();
        test("/tmp")
                .absolute();
        test("tmp")
                .notAbsolute();
        test("")
                .notAbsolute();


        // resolve
        test("/tmp")
                .resolve("foo", "/tmp/foo")
                .resolve("/foo", "/foo")
                .resolve("", "/tmp");
        test("tmp")
                .resolve("foo", "tmp/foo")
                .resolve("/foo", "/foo")
                .resolve("", "tmp");
        test("")
                .resolve("", "")
                .resolve("foo", "foo")
                .resolve("/foo", "/foo");

        // resolveSibling
        test("foo")
                .resolveSibling("bar", "bar")
                .resolveSibling("/bar", "/bar")
                .resolveSibling("", "");
        test("foo/bar")
                .resolveSibling("gus", "foo/gus")
                .resolveSibling("/gus", "/gus")
                .resolveSibling("", "foo");
        test("/foo")
                .resolveSibling("gus", "/gus")
                .resolveSibling("/gus", "/gus")
                .resolveSibling("", "/");
        test("/foo/bar")
                .resolveSibling("gus", "/foo/gus")
                .resolveSibling("/gus", "/gus")
                .resolveSibling("", "/foo");
        test("")
                .resolveSibling("foo", "foo")
                .resolveSibling("/foo", "/foo")
                .resolve("", "");

        // relativize
        test("/a/b/c")
                .relativize("/a/b/c", "")
                .relativize("/a/b/c/d/e", "d/e")
                .relativize("/a/x", "../../x")
                .relativize("/x", "../../../x");
        test("a/b/c")
                .relativize("a/b/c/d", "d")
                .relativize("a/x", "../../x")
                .relativize("x", "../../../x")
                .relativize("", "../../..");
        test("")
                .relativize("a", "a")
                .relativize("a/b/c", "a/b/c")
                .relativize("", "");

        // normalize
        test("/")
                .normalize("/");
        test("foo")
                .normalize("foo");
        test("/foo")
                .normalize("/foo");
        test(".")
                .normalize("");
        test("..")
                .normalize("..");
        test("/..")
                .normalize("/");
        test("/../..")
                .normalize("/");
        test("foo/.")
                .normalize("foo");
        test("./foo")
                .normalize("foo");
        test("foo/..")
                .normalize("");
        test("../foo")
                .normalize("../foo");
        test("../../foo")
                .normalize("../../foo");
        test("foo/bar/..")
                .normalize("foo");
        test("foo/bar/gus/../..")
                .normalize("foo");
        test("/foo/bar/gus/../..")
                .normalize("/foo");

        // invalid
        test("foo\u0000bar")
                .invalid();
        test("\u0000foo")
                .invalid();
        test("bar\u0000")
                .invalid();
        test("//foo\u0000bar")
                .invalid();
        test("//\u0000foo")
                .invalid();
        test("//bar\u0000")
                .invalid();

        // normalization of input
        test("//foo//bar")
                .string("/foo/bar")
                .root("/")
                .parent("/foo")
                .name("bar");
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

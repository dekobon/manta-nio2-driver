package com.github.dekobon.manta.fs;

import org.junit.Test;
import static com.github.dekobon.manta.fs.PathOps.test;

public class PathOpsTest {
    @Test
    public void pathConstruction() {
        test("/")
                .string("/");
        test("/", "")
                .string("/");
        test("/", "foo")
                .string("/foo");
        test("/", "/foo")
                .string("/foo");
        test("/", "foo/")
                .string("/foo");
        test("foo", "bar", "gus")
                .string("foo/bar/gus");
        test("")
                .string("");
        test("", "/")
                .string("/");
        test("", "foo", "", "bar", "", "/gus")
                .string("foo/bar/gus");
        test("", "/foo", "", "bar", "", "/gus")
                .string("/foo/bar/gus");
    }

    @Test
    public void allComponents() {
        test("/a/b/c")
                .root("/")
                .parent("/a/b")
                .name("c");
    }

    @Test
    public void rootComponentOnly() {
        test("/")
                .root("/")
                .parent(null)
                .name(null);
    }

    @Test
    public void noRootComponent() {
        test("a/b")
                .root(null)
                .parent("a")
                .name("b");
    }

    @Test
    public void nameComponentOnly() {
        test("foo")
                .root(null)
                .parent(null)
                .name("foo");
        test("")
                .root(null)
                .parent(null)
                .name("");
    }

    @Test
    public void startsWith() {
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
    }

    @Test
    public void endsWith() {
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
    }

    @Test
    public void elements() {
        test("a/b/c")
                .element(0, "a")
                .element(1, "b")
                .element(2, "c");
        test("")
                .element(0, "");
    }

    @Test
    public void subPath() {
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
    }

    @Test
    public void isAbsolute() {
        test("/")
                .absolute();
        test("/tmp")
                .absolute();
        test("tmp")
                .notAbsolute();
        test("")
                .notAbsolute();
    }

    @Test
    public void resolve() {
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
    }

    @Test
    public void resolveSibling() {
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
    }

    @Test
    public void relativize() {
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
    }

    @Test
    public void normalizeRoot() {
        test("/").normalize("/");
    }

    @Test
    public void normalizeSingleRootNode() {
        test("/foo")
                .normalize("/foo");
        test("foo")
                .normalize("/foo");
    }

    @Test
    public void normalizeRelativePaths() {
        test(".")
                .normalize("/");
        test("..")
                .normalize("/");
        test("/..")
                .normalize("/");
        test("/../..")
                .normalize("/");
        test("foo/.")
                .normalize("/foo");
        test("./foo")
                .normalize("/foo");
        test("foo/..")
                .normalize("/");
        test("../foo")
                .normalize("/foo");
        test("../../foo")
                .normalize("/foo");
    }

    @Test
    public void normalizeTrailingRelativePaths() {
        test("foo/bar/..")
                .normalize("/foo");
        test("foo/bar/gus/../..")
                .normalize("/foo");
        test("/foo/bar/gus/../..")
                .normalize("/foo");
    }

    @Test
    public void invalid() {
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
    }

    @Test
    public void normalizationOfInput() {
        test("//foo//bar")
                .string("/foo/bar")
                .root("/")
                .parent("/foo")
                .name("bar");
    }
}

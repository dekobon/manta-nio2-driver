package com.github.dekobon.manta.fs;

import org.testng.annotations.Test;

import static com.github.dekobon.manta.fs.PathOps.test;

public class PathTest {
    @Test
    public void pathConstruction() {
        test("/").string("/");
        test("/", "").string("/");
        test("/", "foo").string("/foo");
        test("/", "/foo").string("/foo");
        test("/", "foo/").string("/foo");
        test("foo", "bar", "gus").string("foo/bar/gus");
        test("").string("");
        test("", "/").string("/");
        test("", "foo", "", "bar", "", "/gus").string("foo/bar/gus");
        test("", "/foo", "", "bar", "", "/gus").string("/foo/bar/gus");
    }

    @Test
    public void allComponents() {
        test("/a/b/c").root("/");
        test("/a/b/c").parent("/a/b");
        test("/a/b/c").name("c");
    }

    @Test
    public void rootComponentOnly() {
        test("/").root("/");
        test("/").parent(null);
        test("/").name(null);
    }

    @Test
    public void noRootComponent() {
        test("a/b").root(null);
        test("a/b").parent("a");
        test("a/b").name("b");
    }

    @Test
    public void nameComponentOnly() {
        test("foo").root(null);
        test("foo").parent(null);
        test("foo").name("foo");
    }

    @Test
    public void emptyNameComponentOnly() {
        test("").root(null);
        test("").parent(null);
        test("").name("");
    }

    @Test
    public void startsWithRoot() {
        test("/").starts("/");
        test("/").notStarts("");
        test("/").notStarts("/foo");
    }

    @Test
    public void startsWithRootFile() {
        test("/foo").starts("/");
        test("/foo").starts("/foo");
        test("/foo").notStarts("/f");
    }

    @Test
    public void startsWithRootDepth2() {
        test("/foo/bar").starts("/");
        test("/foo/bar").starts("/foo");
        test("/foo/bar").starts("/foo/bar");
        test("/foo/bar").notStarts("/f");
        test("/foo/bar").notStarts("foo");
        test("/foo/bar").notStarts("foo/bar");
    }

    @Test
    public void startsWith() {
        test("foo").starts("foo");
        test("foo").notStarts("");
        test("foo").notStarts("f");

        test("foo/bar");
        test("foo/bar").starts("foo");
        test("foo/bar").starts("foo/bar");
        test("foo/bar").notStarts("/foo");
        test("foo/bar").notStarts("/foo/bar");
        test("foo/bar").notStarts("f");
    }

    @Test
    public void startsWithEmptiness() {
        test("").starts("");
        test("").notStarts("/");
    }

    @Test
    public void endsWithRoot() {
        test("/").ends("/");
        test("/").notEnds("");
        test("/").notEnds("foo");
        test("/").notEnds("/foo");
    }

    @Test
    public void endsWithStartingRoot() {
        test("/foo").ends("foo");
        test("/foo").ends("/foo");
        test("/foo").notEnds("fool");
    }

    @Test
    public void endsWithStartingRootDepth2() {
        test("/foo/bar").ends("bar");
        test("/foo/bar").ends("foo/bar");
        test("/foo/bar").ends("/foo/bar");
        test("/foo/bar").notEnds("ar");
        test("/foo/bar").notEnds("barack");
        test("/foo/bar").notEnds("/bar");
        test("/foo/bar").notEnds("o/bar");
    }

    @Test
    public void endsWith() {
        test("foo").ends("foo");
        test("foo").notEnds("");
        test("foo").notEnds("oo");
        test("foo").notEnds("oola");

        test("foo/bar").ends("bar");
        test("foo/bar").ends("foo/bar");
        test("foo/bar").notEnds("r");
        test("foo/bar").notEnds("barmaid");
        test("foo/bar").notEnds("/bar");

        test("foo/bar/gus").ends("gus");
        test("foo/bar/gus").ends("bar/gus");
        test("foo/bar/gus").ends("foo/bar/gus");
        test("foo/bar/gus").notEnds("g");
        test("foo/bar/gus").notEnds("/gus");
        test("foo/bar/gus").notEnds("r/gus");
        test("foo/bar/gus").notEnds("barack/gus");
        test("foo/bar/gus").notEnds("bar/gust");
    }

    @Test
    public void endsWithEmptiness() {
        test("").ends("");
        test("").notEnds("/");
    }

    @Test
    public void elements() {
        test("a/b/c").element(0, "a");
        test("a/b/c").element(1, "b");
        test("a/b/c").element(2, "c");
    }

    @Test
    public void emptyElements() {
        test("").element(0, "");
    }

    @Test
    public void subPathRelativeToRoot() {
        test("/foo").subpath(0, 1, "foo");
        test("foo").subpath(0, 1, "foo");
    }

    @Test
    public void subPath() {
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
    }

    @Test
    public void subPathEmpty() {
        test("").subpath(0, 1, "");
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
        test("/tmp").resolve("foo", "/tmp/foo");
        test("/tmp").resolve("/foo", "/foo");
        test("/tmp").resolve("", "/tmp");

        test("tmp").resolve("foo", "tmp/foo");
        test("tmp").resolve("/foo", "/foo");
        test("tmp").resolve("", "tmp");
    }

    @Test
    public void resolveEmpty() {
        test("").resolve("", "");
        test("").resolve("foo", "foo");
        test("").resolve("/foo", "/foo");
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
    public void relativizeFromRoot() {
        test("/a/b/c").relativize("/a/b/c", "");
        test("/a/b/c").relativize("/a/b/c/d/e", "d/e");
        test("/a/b/c").relativize("/a/x", "../../x");
        test("/a/b/c").relativize("/x", "../../../x");
    }

    @Test
    public void relativizeRelativePaths() {
        test("a/b/c").relativize("a/b/c/d", "d");
        test("a/b/c").relativize("a/x", "../../x");
        test("a/b/c").relativize("x", "../../../x");
        test("a/b/c").relativize("", "../../..");
    }

    @Test
    public void relativizeEmptiness() {
        test("").relativize("a", "a");
        test("").relativize("a/b/c", "a/b/c");
        test("").relativize("", "");
    }

    @Test
    public void normalizeRoot() {
        test("/").normalize("/");
    }

    @Test
    public void normalizeSingleRootNode() {
        test("/foo").normalize("/foo");
        test("foo").normalize("foo");
    }

    @Test
    public void normalizeCurrentDirRef() {
        test(".").normalize("");
    }

    @Test
    public void normalizeBackDirRef() {
        test("..").normalize("..");
    }

    @Test
    public void normalizeSeparatorBackDirRef() {
        test("/..").normalize("/");
    }

    @Test
    public void normalizeRelativePaths() {
        test("/../..").normalize("/");
        test("foo/.").normalize("foo");
        test("./foo").normalize("foo");
        test("foo/..").normalize("");
        test("../foo").normalize("foo");
        test("../../foo").normalize("foo");
    }

    @Test
    public void normalizeTrailingRelativePaths() {
        test("foo/bar/..")
                .normalize("foo");
        test("foo/bar/gus/../..")
                .normalize("foo");
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

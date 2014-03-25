package com.jetbrains.lang.dart.ide.runner;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DartPositionInfo {

  private static final String PACKAGE_PREFIX = "package:";

  public enum Type {
    FILE, DART, PACKAGE;

    @Nullable
    public static Type getType(final String path) {
      if (path.startsWith("package:")) return PACKAGE;
      if (path.startsWith("dart:")) return DART;
      if (path.endsWith(".dart")) return FILE;
      return null;
    }
  }

  public final @NotNull Type type;
  public final @NotNull String path;
  public final int highlightingStartIndex;
  public final int highlightingEndIndex;
  public final int line;
  public final int column;

  public DartPositionInfo(final @NotNull Type type,
                          final @NotNull String path,
                          final int highlightingStartIndex,
                          final int highlightingEndIndex,
                          final int line,
                          final int column) {
    this.type = type;
    this.path = path;
    this.highlightingStartIndex = highlightingStartIndex;
    this.highlightingEndIndex = highlightingEndIndex;
    this.line = line;
    this.column = column;
  }

  /*
   * package:unittest/src/expect.dart 75:29  expect
   * baz.dart 17:29                          main.<fn>.<fn>
   * baz.dart 34:19                          runTests.<fn>
   * dart:isolate                            _RawReceivePortImpl._handleMessage
   */
  @Nullable
  public static DartPositionInfo parsePositionInfo(final @NotNull String text) {

    final String[] strings = text.split(" ");
    if (strings.length < 2) return null;

    final String path = strings[0];
    final Type type = Type.getType(path);

    if (type == null) return null;

    final String lineAndColumnString = strings[1];
    final Pair<Integer, Integer> lineAndColumn = parseLineAndColumn(lineAndColumnString);

    // NOTE that column info is elided (with no range, it's not very useful)
    return new DartPositionInfo(type,
                                path,
                                0,
                                path.length(),
                                lineAndColumn.first - 1,
                                0 /* lineAndColumn.second */);
  }

  @NotNull
  private static Pair<Integer, Integer> parseLineAndColumn(final @NotNull String text) {
    final String[] index = text.split(":");
    if (index.length == 2) {
      try {
        return Pair.create(Integer.parseInt(index[0]), Integer.parseInt(index[1]));
      }
      catch (NumberFormatException e) {
        // fall-through
      }
    }

    return Pair.create(-1, -1);
  }

  @NotNull
  private static String relativize(final @NotNull String path) {
    if (path.startsWith(PACKAGE_PREFIX)) {
      return path.substring(PACKAGE_PREFIX.length() + 1);
    }
    return path;
  }

}

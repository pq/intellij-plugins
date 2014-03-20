package com.jetbrains.lang.dart.ide.runner;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DartPositionInfo {

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
  #0      Object.noSuchMethod (dart:core-patch/object_patch.dart:42)
  #1      SplayTreeMap.addAll (dart:collection/splay_tree.dart:373)
  #1      Sphere.copyFrom (package:vector_math/src/vector_math/sphere.dart:46:23)
  #1      StringBuffer.writeAll (dart:core/string_buffer.dart:41)
  #2      main (file:///C:/dart/DartSample2/web/Bar.dart:4:28)
  #3      _startIsolate.isolateStartHandler (dart:isolate-patch/isolate_patch.dart:190)
  #4      _RawReceivePortImpl._handleMessage (dart:isolate-patch/isolate_patch.dart:93)
  */


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


    //final char nextChar = text.charAt(pathEndIndex);
    //if (nextChar != ':' && nextChar != ')') return null;
    //
    //final int leftParenIndex = text.substring(0, dotDartIndex).lastIndexOf("(");
    //final int rightParenIndex = text.indexOf(")", dotDartIndex);
    //if (leftParenIndex < 0 || rightParenIndex < 0) return null;
    //
    //final int colonIndex = text.indexOf(":", leftParenIndex);
    //if (colonIndex < 0) return null;


    return new DartPositionInfo(type,
                                path,
                                0,
                                path.length(),
                                lineAndColumn.first,
                                lineAndColumn.second);
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


    //try {
    //  int index = 1;
    //  final int lineTextStartIndex = index;
    //  while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
    //
    //  if (index == lineTextStartIndex) return Pair.create(-1, -1);
    //  final int line = Integer.parseInt(text.substring(lineTextStartIndex, index));
    //
    //  if (index == text.length() || text.charAt(index) != ':') return Pair.create(line, -1);
    //
    //  index++;
    //  final int columnTextStartIndex = index;
    //  while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
    //
    //  if (index == columnTextStartIndex) return Pair.create(line, -1);
    //  final int column = Integer.parseInt(text.substring(columnTextStartIndex, index));
    //
    //  return Pair.create(line, column);
    //}
    //catch (NumberFormatException e) {
    //  return Pair.create(-1, -1);
    //}
  }

  // trim all leading slashes on windows or all except one on Mac/Linux
  private static int getPathStartIndex(final @NotNull String text) {
    if (text.isEmpty() || text.charAt(0) != '/') return 0;

    int index = 0;
    while (index < text.length() && text.charAt(index) == '/') index++;

    return SystemInfo.isWindows ? index : index - 1;
  }
}

package com.jetbrains.lang.dart.ide.documentation;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.DartCodeInsightFixtureTestCase;
import com.jetbrains.lang.dart.psi.DartComponent;

public class DartDocumentationProviderTest extends DartCodeInsightFixtureTestCase {


  private static final DartDocumentationProvider myProvider = new DartDocumentationProvider();

  private void doTest(String expectedDoc, String fileContents) throws Exception {
    final int caretOffset = fileContents.indexOf("<caret>");
    assertTrue(caretOffset != -1);
    final String realContents = fileContents.substring(0, caretOffset) + fileContents.substring(caretOffset + "<caret>".length());
    final PsiFile psiFile = myFixture.addFileToProject("test.dart", realContents);

    final DartComponent dartComponent = PsiTreeUtil.getParentOfType(psiFile.findElementAt(caretOffset), DartComponent.class);
    assertNotNull("target element not found at offset " + caretOffset, dartComponent);


    assertEquals(expectedDoc, myProvider.getUrlFor(dartComponent, dartComponent));
  }

  public void testObjectClass() throws Exception {
    doTest("http://api.dartlang.org/docs/releases/latest/dart_core/Object.html",
           "<caret>Object o;\n");
  }

}

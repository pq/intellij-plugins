package com.jetbrains.lang.dart.ide.runner.unittest;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PathUtil;
import com.jetbrains.lang.dart.ide.runner.server.DartCommandLineRuntimeConfigurationProducer;
import com.jetbrains.lang.dart.psi.DartArgumentList;
import com.jetbrains.lang.dart.psi.DartCallExpression;
import com.jetbrains.lang.dart.psi.DartExpression;
import com.jetbrains.lang.dart.psi.DartFile;
import com.jetbrains.lang.dart.util.DartResolveUtil;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.jetbrains.lang.dart.ide.runner.unittest.DartUnitRunnerParameters.Scope;

public class DartUnitRunConfigurationProducer extends RunConfigurationProducer<DartUnitRunConfiguration> {
  public DartUnitRunConfigurationProducer() {
    super(DartUnitRunConfigurationType.getInstance());
  }

  @Override
  protected boolean setupConfigurationFromContext(final @NotNull DartUnitRunConfiguration configuration,
                                                  final @NotNull ConfigurationContext context,
                                                  final @NotNull Ref<PsiElement> sourceElement) {
    final VirtualFile dartFile = DartCommandLineRuntimeConfigurationProducer.getRunnableDartFileFromContext(context);
    if (dartFile == null) return false;

    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(context.getProject(), dartFile);
    final VirtualFile dartUnitLib = urlResolver.findFileByDartUrl("package:unittest/unittest.dart");
    if (dartUnitLib == null) return false;

    final VirtualFile yamlFile = urlResolver.getPubspecYamlFile();
    if (yamlFile != null) {
      final VirtualFile parent = yamlFile.getParent();
      final VirtualFile testFolder = parent == null ? null : parent.findChild("test");
      if (testFolder == null || !testFolder.isDirectory() || !VfsUtilCore.isAncestor(testFolder, dartFile, true)) {
        return false;
      }
    }

    final PsiElement element = findTestElement(context.getPsiLocation());
    if (element == null || !setupRunConfiguration(configuration.getRunnerParameters(), element)) {
      return false;
    }

    configuration.setGeneratedName();
    return true;
  }

  @Override
  public boolean isConfigurationFromContext(final @NotNull DartUnitRunConfiguration configuration,
                                            final @NotNull ConfigurationContext context) {
    final PsiElement testElement = findTestElement(context.getPsiLocation());
    if (testElement == null) return false;

    final DartUnitRunnerParameters paramsFromContext = new DartUnitRunnerParameters();
    if (!setupRunConfiguration(paramsFromContext, testElement)) return false;

    final DartUnitRunnerParameters existingParams = configuration.getRunnerParameters();

    return Comparing.equal(existingParams.getFilePath(), paramsFromContext.getFilePath()) &&
           Comparing.equal(existingParams.getScope(), paramsFromContext.getScope()) &&
           (existingParams.getScope() == Scope.ALL ||
            Comparing.equal(existingParams.getTestName(), paramsFromContext.getTestName()));
  }

  private static boolean setupRunConfiguration(final @NotNull DartUnitRunnerParameters runnerParams, final @NotNull PsiElement psiElement) {
    if (psiElement instanceof DartCallExpression) {
      final String testName = findTestName((DartCallExpression)psiElement);
      final List<VirtualFile> virtualFiles = DartResolveUtil.findLibrary(psiElement.getContainingFile());
      if (testName == null || virtualFiles.isEmpty()) {
        return false;
      }

      runnerParams.setTestName(testName);
      runnerParams.setScope(isTest((DartCallExpression)psiElement) ? Scope.METHOD : Scope.GROUP);
      final String dartFilePath = virtualFiles.iterator().next().getPath();
      runnerParams.setFilePath(dartFilePath);
      runnerParams.setWorkingDirectory(PathUtil.getParentPath(dartFilePath));
      return true;
    }
    else {
      final PsiFile psiFile = psiElement.getContainingFile();
      if (psiFile instanceof DartFile) {
        final VirtualFile virtualFile = DartResolveUtil.getRealVirtualFile((DartFile)psiElement);
        if (virtualFile == null || !DartResolveUtil.isLibraryRoot((DartFile)psiElement)) {
          return false;
        }

        runnerParams.setTestName(DartResolveUtil.getLibraryName((DartFile)psiElement));
        runnerParams.setScope(Scope.ALL);
        final String dartFilePath = FileUtil.toSystemIndependentName(virtualFile.getPath());
        runnerParams.setFilePath(dartFilePath);
        runnerParams.setWorkingDirectory(PathUtil.getParentPath(dartFilePath));
        return true;
      }
    }
    return false;
  }

  @Nullable
  private static String findTestName(@Nullable DartCallExpression expression) {
    String testName;
    final DartArgumentList dartArgumentList = expression == null ? null : expression.getArguments().getArgumentList();
    if (dartArgumentList == null || dartArgumentList.getExpressionList().isEmpty()) {
      return null;
    }
    final DartExpression dartExpression = dartArgumentList.getExpressionList().get(0);
    testName = dartExpression == null ? "" : StringUtil.unquoteString(dartExpression.getText());
    return testName;
  }

  @Nullable
  private static PsiElement findTestElement(@Nullable PsiElement element) {
    DartCallExpression callExpression = PsiTreeUtil.getParentOfType(element, DartCallExpression.class);
    while (callExpression != null) {
      if (isGroup(callExpression) || isTest(callExpression)) {
        return callExpression;
      }
      callExpression = PsiTreeUtil.getParentOfType(callExpression, DartCallExpression.class, true);
    }
    return element != null ? element.getContainingFile() : null;
  }

  private static boolean isTest(DartCallExpression expression) {
    return checkLibAndName(expression, "test");
  }

  private static boolean isGroup(DartCallExpression expression) {
    return checkLibAndName(expression, "group");
  }

  private static boolean checkLibAndName(DartCallExpression callExpression, String expectedName) {
    final String name = callExpression.getExpression().getText();
    return expectedName.equalsIgnoreCase(name);
  }
}

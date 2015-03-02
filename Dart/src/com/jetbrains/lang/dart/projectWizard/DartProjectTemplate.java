package com.jetbrains.lang.dart.projectWizard;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.impl.WebBrowserServiceImpl;
import com.intellij.javascript.debugger.execution.JavaScriptDebugConfiguration;
import com.intellij.javascript.debugger.execution.JavascriptDebugConfigurationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Consumer;
import com.intellij.util.Url;
import com.jetbrains.lang.dart.ide.runner.client.DartiumUtil;
import com.jetbrains.lang.dart.ide.runner.server.DartCommandLineRunConfiguration;
import com.jetbrains.lang.dart.ide.runner.server.DartCommandLineRunConfigurationType;
import com.jetbrains.lang.dart.projectWizard.Stagehand.StagehandDescriptor;
import com.jetbrains.lang.dart.util.PubspecYamlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class DartProjectTemplate {

  private static final Stagehand STAGEHAND = new Stagehand();
  private static List<DartProjectTemplate> ourTemplateCache;

  private static final Logger LOG = Logger.getInstance(DartProjectTemplate.class.getName());

  @NotNull private final String myName;
  @NotNull private final String myDescription;

  public DartProjectTemplate(@NotNull final String name, @NotNull final String description) {
    myName = name;
    myDescription = description;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public String getDescription() {
    return myDescription;
  }

  public abstract Collection<VirtualFile> generateProject(@NotNull final String sdkRoot,
                                                          @NotNull final Module module,
                                                          @NotNull final VirtualFile baseDir)
    throws IOException;


  /**
   * Get the Dart project directory for the given file by looking for the first parent that contains a pubspec.
   * In case none can be found, the file's parent is used instead.
   */
  public static String getProjectDirectory(@Nullable final Project project, @NotNull final VirtualFile contextFile) {
    if (project != null) {
      final VirtualFile pubspec = PubspecYamlUtil.findPubspecYamlFile(project, contextFile);
      if (pubspec != null) {
        final VirtualFile parent = pubspec.getParent();
        if (parent != null) {
          return parent.getPath();
        }
      }
    }
    return contextFile.getParent().getPath();
  }

  /**
   * Must be called in pooled thread without read action; <code>templatesConsumer</code> will be invoked in EDT
   */
  public static void loadTemplatesAsync(final String sdkRoot, @NotNull final Consumer<List<DartProjectTemplate>> templatesConsumer) {
    if (ApplicationManager.getApplication().isReadAccessAllowed()) {
      LOG.error("DartProjectTemplate.loadTemplatesAsync() must be called in pooled thread without read action");
    }

    final List<DartProjectTemplate> templates = new ArrayList<DartProjectTemplate>();
    try {
      templates.addAll(getStagehandTemplates(sdkRoot));
    }
    finally {
      if (templates.isEmpty()) {
        templates.add(new WebAppTemplate());
        templates.add(new CmdLineAppTemplate());
      }

      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          templatesConsumer.consume(templates);
        }
      }, ModalityState.any());
    }
  }

  @NotNull
  private static List<DartProjectTemplate> getStagehandTemplates(@NotNull final String sdkRoot) {
    if (ourTemplateCache != null) {
      return ourTemplateCache;
    }

    STAGEHAND.install(sdkRoot);

    final List<StagehandDescriptor> templates = STAGEHAND.getAvailableTemplates(sdkRoot);

    ourTemplateCache = new ArrayList<DartProjectTemplate>();

    for (StagehandDescriptor template : templates) {
      ourTemplateCache.add(new StagehandTemplate(STAGEHAND, template));
    }

    return ourTemplateCache;
  }

  static void createWebRunConfiguration(final @NotNull Module module, final @NotNull VirtualFile htmlFile) {
    DartModuleBuilder.runWhenNonModalIfModuleNotDisposed(new Runnable() {
      public void run() {
        final WebBrowser dartium = DartiumUtil.getDartiumBrowser();
        if (dartium == null) return;

        final Url url = WebBrowserServiceImpl.getDebuggableUrl(PsiManager.getInstance(module.getProject()).findFile(htmlFile));
        if (url == null) return;

        final RunManager runManager = RunManager.getInstance(module.getProject());
        try {
          final RunnerAndConfigurationSettings settings =
            runManager.createRunConfiguration("", JavascriptDebugConfigurationType.getTypeInstance().getFactory());

          ((JavaScriptDebugConfiguration)settings.getConfiguration()).setUri(url.toDecodedForm());
          ((JavaScriptDebugConfiguration)settings.getConfiguration()).setEngineId(dartium.getId().toString());
          settings.setName(((JavaScriptDebugConfiguration)settings.getConfiguration()).suggestedName());

          runManager.addConfiguration(settings, false);
          runManager.setSelectedConfiguration(settings);
        }
        catch (Throwable t) {/* ClassNotFound in IDEA Community or if JS Debugger plugin disabled */}
      }
    }, module);
  }

  static void createCmdLineRunConfiguration(final @NotNull Module module, final @NotNull VirtualFile mainDartFile) {
    DartModuleBuilder.runWhenNonModalIfModuleNotDisposed(new Runnable() {
      @Override
      public void run() {
        final RunManager runManager = RunManager.getInstance(module.getProject());
        final RunnerAndConfigurationSettings settings =
          runManager.createRunConfiguration("", DartCommandLineRunConfigurationType.getInstance().getConfigurationFactories()[0]);

        final DartCommandLineRunConfiguration runConfiguration = (DartCommandLineRunConfiguration)settings.getConfiguration();
        runConfiguration.getRunnerParameters().setFilePath(mainDartFile.getPath());
        final String workingDir = getProjectDirectory(module.getProject(), mainDartFile);
        runConfiguration.getRunnerParameters().setWorkingDirectory(workingDir);
        settings.setName(runConfiguration.suggestedName());

        runManager.addConfiguration(settings, false);
        runManager.setSelectedConfiguration(settings);
      }
    }, module);
  }
}
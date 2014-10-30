package com.jetbrains.lang.dart.ide.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PairProcessor;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class DartComponentIndex extends FileBasedIndexExtension<String, DartComponentInfo> {
  public static final ID<String, DartComponentInfo> DART_COMPONENT_INDEX = ID.create("DartComponentIndex");
  private static final int INDEX_VERSION = 3;
  private final DataIndexer<String, DartComponentInfo, FileContent> myIndexer = new MyDataIndexer();
  private final DataExternalizer<DartComponentInfo> myExternalizer = new DartComponentInfoExternalizer();

  @NotNull
  @Override
  public ID<String, DartComponentInfo> getName() {
    return DART_COMPONENT_INDEX;
  }

  @NotNull
  @Override
  public DataIndexer<String, DartComponentInfo, FileContent> getIndexer() {
    return myIndexer;
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @NotNull
  @Override
  public DataExternalizer<DartComponentInfo> getValueExternalizer() {
    return myExternalizer;
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return DartInputFilter.INSTANCE;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return DartIndexUtil.BASE_VERSION + INDEX_VERSION;
  }

  public static List<VirtualFile> getAllFiles(@NotNull Project project, @Nullable String componentName) {
    if (componentName == null) {
      return Collections.emptyList();
    }
    return new ArrayList<VirtualFile>(
      FileBasedIndex.getInstance().getContainingFiles(DART_COMPONENT_INDEX, componentName, GlobalSearchScope.allScope(project)));
  }

  public static void processAllComponents(@NotNull PsiElement context,
                                          final PairProcessor<String, DartComponentInfo> processor,
                                          Condition<String> nameFilter) {
    final Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(DART_COMPONENT_INDEX, context.getProject());
    for (final String componentName : allKeys) {
      if (nameFilter.value(componentName)) {
        continue;
      }
      if (processComponentsByName(context, new Processor<DartComponentInfo>() {
        @Override
        public boolean process(DartComponentInfo info) {
          return processor.process(componentName, info);
        }
      }, componentName)) {
        return;
      }
    }
  }

  public static boolean processComponentsByName(PsiElement context,
                                                Processor<DartComponentInfo> processor,
                                                String componentName) {
    final List<DartComponentInfo> allComponents = FileBasedIndex.getInstance().getValues(
      DART_COMPONENT_INDEX, componentName, context.getResolveScope()
    );
    for (DartComponentInfo componentInfo : allComponents) {
      if (!processor.process(componentInfo)) return true;
    }
    return false;
  }

  private static class MyDataIndexer implements DataIndexer<String, DartComponentInfo, FileContent> {
    @NotNull
    @Override
    public Map<String, DartComponentInfo> map(@NotNull FileContent inputData) {
      return DartIndexUtil.indexFile(inputData).getComponentInfoMap();
    }
  }
}

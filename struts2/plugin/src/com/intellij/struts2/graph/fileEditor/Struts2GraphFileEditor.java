/*
 * Copyright 2014 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts2.graph.fileEditor;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.graph.builder.util.GraphViewUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author Yann C&eacute;bron
 */
public class Struts2GraphFileEditor extends PerspectiveFileEditor {

  private Struts2GraphComponent myComponent;
  private final XmlFile myXmlFile;

  public Struts2GraphFileEditor(final Project project, final VirtualFile file) {
    super(project, file);

    final PsiFile psiFile = getPsiFile();
    assert psiFile instanceof XmlFile;

    myXmlFile = (XmlFile)psiFile;
  }

  @Nullable
  protected DomElement getSelectedDomElement() {
    final List<DomElement> selectedDomElements = getStruts2GraphComponent().getSelectedDomElements();

    return selectedDomElements.size() > 0 ? selectedDomElements.get(0) : null;
  }

  protected void setSelectedDomElement(final DomElement domElement) {
    getStruts2GraphComponent().setSelectedDomElement(domElement);
  }

  @NotNull
  protected JComponent createCustomComponent() {
    return getStruts2GraphComponent();
  }

  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return getStruts2GraphComponent().getBuilder().getView().getJComponent();
  }

  public void commit() {
  }

  public void reset() {
    getStruts2GraphComponent().getBuilder().queueUpdate();
  }

  @NotNull
  public String getName() {
    return "Graph";
  }

  public StructureViewBuilder getStructureViewBuilder() {
    return GraphViewUtil.createStructureViewBuilder(getStruts2GraphComponent().getOverview());
  }

  private Struts2GraphComponent getStruts2GraphComponent() {
    if (myComponent == null) {
      myComponent = createGraphComponent();
      Disposer.register(this, myComponent);
    }
    return myComponent;
  }


  /**
   * Creates graph component while showing modal wait dialog.
   *
   * @return new instance.
   */
  private Struts2GraphComponent createGraphComponent() {
    final Struts2GraphComponent[] graphComponent = {null};
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      public void run() {
        graphComponent[0] = ApplicationManager.getApplication().runReadAction(new Computable<Struts2GraphComponent>() {
          @Override
          public Struts2GraphComponent compute() {
            return new Struts2GraphComponent(myXmlFile);
          }
        });
      }
    }, "Generating graph", false, myXmlFile.getProject());


    return graphComponent[0];
  }
}
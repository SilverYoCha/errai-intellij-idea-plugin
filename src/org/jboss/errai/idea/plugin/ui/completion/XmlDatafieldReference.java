/*
 * Copyright 2013 Red Hat, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.jboss.errai.idea.plugin.ui.completion;

import com.google.common.collect.Multimap;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jboss.errai.idea.plugin.ui.TemplateDataField;
import org.jboss.errai.idea.plugin.ui.TemplateUtil;
import org.jboss.errai.idea.plugin.ui.model.TemplateMetaData;
import org.jboss.errai.idea.plugin.util.AnnotationSearchResult;
import org.jboss.errai.idea.plugin.util.AnnotationValueElement;
import org.jboss.errai.idea.plugin.util.Types;
import org.jboss.errai.idea.plugin.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Mike Brock
 */
public class XmlDatafieldReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  public XmlDatafieldReference(XmlAttribute element, boolean soft) {
    super(element, soft);
  }

  public static class DataFieldRef {
    private final String name;
    private final String fileName;

    public DataFieldRef(String name, String fileName) {
      this.name = name;
      this.fileName = fileName;
    }

    public String getName() {
      return name;
    }

    public String getFileName() {
      return fileName;
    }
  }

  private List<DataFieldRef> findLinkedTemplateAndDataFields() {
    final Project project = getElement().getProject();
    DaemonCodeAnalyzer.getInstance(project).restart();

    final XmlFile xmlFile = getXmlFile();
    final List<DataFieldRef> dataFieldRefs = new ArrayList<DataFieldRef>();
    final Multimap<String, TemplateDataField> allDataFieldTags
        = TemplateUtil.findAllDataFieldTags(xmlFile, xmlFile.getRootTag(), true);

    for (TemplateMetaData metaData : TemplateUtil.getTemplateOwners(xmlFile)) {
      final Collection<AnnotationSearchResult> allAnnotatedElements
          = Util.findAllAnnotatedElements(metaData.getTemplateClass(), Types.DATAFIELD);

      for (String dataField : TemplateUtil.extractDataFieldList(allAnnotatedElements)) {
        if (allDataFieldTags.containsKey(dataField) || dataField.contains(Util.INTELLIJ_MAGIC_STRING)) continue;

        dataFieldRefs.add(new DataFieldRef(dataField, metaData.getTemplateClass().getQualifiedName()));
      }
    }

    return dataFieldRefs;
  }

  private XmlFile getXmlFile() {
    PsiElement el = getElement();
    while (!(el instanceof XmlFile) && el != null) {
      el = el.getParent();
    }
    return (XmlFile) el;
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    final PsiElement element = getElement();
    ((XmlAttributeImpl) element).setValue(newElementName);
    return element;
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    final ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? (resolveResults[0]).getElement() : null;
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
      return ResolveCache.getInstance(myElement.getContainingFile().getProject())
              .resolveWithCaching(this, MyResolver.INSTANCE, false, incompleteCode, getXmlFile());           
  }

  private static class MyResolver implements ResolveCache.PolyVariantResolver<XmlDatafieldReference> {
  static final MyResolver INSTANCE = new MyResolver();

    @NotNull
    public ResolveResult[] resolve(@NotNull XmlDatafieldReference xmlDatafieldReference, boolean incompleteCode) {
      final List<ResolveResult> result = new ArrayList<ResolveResult>();
      final XmlFile xmlFile = xmlDatafieldReference.getXmlFile();
      final Collection<TemplateMetaData> templateOwners = TemplateUtil.getTemplateOwners(xmlFile);
      final String dataFieldName = ((XmlAttribute) xmlDatafieldReference.getElement()).getValue();
      for (TemplateMetaData metaData : templateOwners) {
        if (dataFieldName != null) {
          if (!metaData.getTemplateExpression().hasRootNode() || dataFieldName.equals(metaData.getTemplateExpression().getRootNode())) {
            result.add(new PsiElementResolveResult(metaData.getTemplateClass()));
          }
        }
        final Collection<AnnotationSearchResult> allAnnotatedElements =
              Util.findAllAnnotatedElements(metaData.getTemplateClass(), Types.DATAFIELD);
        for (AnnotationSearchResult element : allAnnotatedElements) {
          final AnnotationValueElement annotationValueElement = Util.getValueStringFromAnnotationWithDefault(element.getAnnotation());
          if (annotationValueElement.getValue().equals(dataFieldName)) {
            result.add(new PsiElementResolveResult(annotationValueElement.getLogicalElement()));
          }          
        }
      }      
      // unknown template or a different XML file
      if (templateOwners.size() == 0) {
        result.add(new PsiElementResolveResult(xmlDatafieldReference.getElement()));
      }
      return ContainerUtil.toArray(result, new ResolveResult[result.size()]);
    }
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    List<Object> results = new ArrayList<Object>();
    for (DataFieldRef ref : findLinkedTemplateAndDataFields()) {
      results.add(LookupElementBuilder.create(ref.getName()).withTypeText(ref.getFileName(), true));
    }
    return results.toArray();
  }
}

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

package org.jboss.errai.idea.plugin.util;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Mike Brock
 */
public class ExpressionErrorReference extends PsiReferenceBase<PsiLiteralExpression> {
  private final String errorElemement;
  private final TextRange textRange;

  public ExpressionErrorReference(@NotNull PsiLiteralExpression element, String errorElemement, TextRange textRange) {
    super(element);
    this.errorElemement = errorElemement;
    this.textRange = textRange;
  }

  @Override
  public TextRange getRangeInElement() {
    return textRange;
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return errorElemement;
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    return null;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    return new Object[0];
  }
}

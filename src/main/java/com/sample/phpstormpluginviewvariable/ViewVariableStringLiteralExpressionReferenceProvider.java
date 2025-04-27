package com.sample.phpstormpluginviewvariable;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

public class ViewVariableStringLiteralExpressionReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof StringLiteralExpression)) {
            return PsiReference.EMPTY_ARRAY;
        }
        var reference = new ViewStringLiteralExpressionReference((StringLiteralExpression) element);
        return new PsiReference[]{reference};
    }
}


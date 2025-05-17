package com.sample.phpstormpluginviewvariable;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * ViewVariableStringLiteralExpressionReferenceProvider
 * Viewファイル内のStringLiteralExpression（主に変数名やsetVarの第一引数）に対して、
 * 独自の参照解決（Reference）を提供するProviderクラス。
 * 例：setVar('sum', ...)やView内の変数名の参照解決を担当。
 */
public class ViewVariableStringLiteralExpressionReferenceProvider extends PsiReferenceProvider {
    /**
     * StringLiteralExpressionに対して参照（Reference）を返す。
     * 例：setVarの第一引数やView内の変数名の参照解決を行う。
     */
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof StringLiteralExpression)) {
            return PsiReference.EMPTY_ARRAY;
        }
        var reference = new ViewStringLiteralExpressionReference((StringLiteralExpression) element);
        return new PsiReference[]{reference};
    }
}


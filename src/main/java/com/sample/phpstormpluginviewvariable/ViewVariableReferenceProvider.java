package com.sample.phpstormpluginviewvariable;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * ViewVariableReferenceProvider
 * Viewファイル内の変数（Variable）に対して、setVarメソッドの引数への参照解決を提供するProviderクラス。
 * 例：View内の$sum変数から、対応する$this->setVar('sum', ...)へのジャンプを実現する。
 */
public class ViewVariableReferenceProvider extends PsiReferenceProvider {
    /**
     * 変数に対して参照（Reference）を返す。
     * Viewファイル内の変数から、対応するsetVarメソッドの引数への参照を提供する。
     */
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof Variable)) {
            return PsiReference.EMPTY_ARRAY;
        }
        var reference = new ViewVariableReference((Variable) element);
        return new PsiReference[]{reference};
    }
} 
package com.sample.phpstormpluginviewvariable.reference;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.sample.phpstormpluginviewvariable.reference.debug.AllPsiElementReferenceProvider;
import com.sample.phpstormpluginviewvariable.reference.stringliteral.ViewVariableStringLiteralExpressionReferenceProvider;

/**
 * ViewReferenceContributor
 * Viewファイル内の特定要素（例：StringLiteralExpression）に対して参照解決Providerを登録するクラス。
 * 例：setVarの第一引数やView内の変数名に対して、独自のReferenceProviderを紐付ける。
 */
public class ViewReferenceContributor extends PsiReferenceContributor {

    /**
     * ReferenceProviderの登録処理。
     * StringLiteralExpressionに対してViewStringLiteralExpressionReferenceProviderを登録する。
     * また、Variableに対してViewVariableReferenceProviderを登録する。
     */
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
        // すべてのPsiElementに対する参照解決（デバッグ用）
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(PsiElement.class),
                new AllPsiElementReferenceProvider()
        );

        // setVarの第一引数に対する参照解決
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(StringLiteralExpression.class),
                new ViewVariableStringLiteralExpressionReferenceProvider()
        );
    }
}
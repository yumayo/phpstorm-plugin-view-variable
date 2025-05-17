package com.sample.phpstormpluginviewvariable;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.sample.phpstormpluginviewvariable.util.Log;
import org.jetbrains.annotations.NotNull;

public class AllPsiElementReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        Log.info("Element type: " + element.getClass().getSimpleName());
        Log.info("Element text: " + element.getText());
        Log.info("Element location: " + element.getContainingFile().getName() + ":" + element.getTextOffset());
        
        return PsiReference.EMPTY_ARRAY;
    }
} 
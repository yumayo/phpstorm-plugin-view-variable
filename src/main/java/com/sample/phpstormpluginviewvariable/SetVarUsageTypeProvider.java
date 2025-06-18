package com.sample.phpstormpluginviewvariable;

import com.intellij.psi.PsiElement;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetVarUsageTypeProvider implements UsageTypeProvider {

    @Override
    public @Nullable UsageType getUsageType(@NotNull PsiElement element) {
        // setVar('sum', ...) の 'sum' 部分を「書き込み」として判定
        if (!(element instanceof StringLiteralExpression)) {
            return null;
        }

        PsiElement parent = element.getParent();
        if (!(parent instanceof ParameterList)) {
            return null;
        }
        PsiElement grandParent = parent.getParent();
        if (!(grandParent instanceof MethodReference)) {
            return null;
        }
        MethodReference methodRef = (MethodReference) grandParent;
        if (!"setVar".equals(methodRef.getName())) {
            return null;
        }
        PsiElement[] args = methodRef.getParameters();
        if (args.length < 1) {
            return null;
        }
        if (args[0] != element) {
            return null;
        }
        return UsageType.WRITE;
    }
}

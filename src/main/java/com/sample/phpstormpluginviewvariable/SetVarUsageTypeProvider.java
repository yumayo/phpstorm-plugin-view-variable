package com.sample.phpstormpluginviewvariable;

import com.intellij.psi.PsiElement;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.Nullable;

public class SetVarUsageTypeProvider implements UsageTypeProvider {

    @Override
    public @Nullable UsageType getUsageType(PsiElement element) {
        // setVar('sum', ...) の 'sum' 部分を「書き込み」として判定
        if (element instanceof StringLiteralExpression) {
            PsiElement parent = element.getParent();
            if (parent instanceof ParameterList) {
                PsiElement grandParent = parent.getParent();
                if (grandParent instanceof MethodReference) {
                    MethodReference methodRef = (MethodReference) grandParent;
                    if ("setVar".equals(methodRef.getName())) {
                        PsiElement[] args = methodRef.getParameters();
                        if (args.length > 0 && args[0] == element) {
                            return UsageType.WRITE;
                        }
                    }
                }
            }
        }
        return null;
    }
}

package com.sample.phpstormpluginviewvariable;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * ViewVariableReference
 * Viewファイル内の変数から、対応するsetVarメソッドの引数への参照解決を実装するクラス。
 * 例：View内の$sum変数から、対応する$this->setVar('sum', ...)へのジャンプを実現する。
 */
public class ViewVariableReference extends PsiReferenceBase<Variable> {
    public ViewVariableReference(@NotNull Variable element) {
        super(element);
    }

    @Override
    public @Nullable PsiElement resolve() {
        String varName = myElement.getName();
        if (varName == null || !varName.startsWith("$")) {
            return null;
        }
        varName = varName.substring(1);

        Project project = myElement.getProject();
        PsiFile viewFile = myElement.getContainingFile();
        if (viewFile == null) {
            return null;
        }

        Collection<MethodReference> methodRefs = ControllerFile.getMethodReferences(
                viewFile.getVirtualFile(),
                project
        );

        for (MethodReference methodRef : methodRefs) {
            if (!"setVar".equals(methodRef.getName())) {
                continue;
            }
            PsiElement[] args = methodRef.getParameters();
            if (args.length < 1) {
                continue;
            }
            if (!(args[0] instanceof StringLiteralExpression)) {
                continue;
            }
            StringLiteralExpression keyArg = (StringLiteralExpression) args[0];
            if (varName.equals(keyArg.getContents())) {
                return keyArg;
            }
        }
        return null;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (!(element instanceof StringLiteralExpression)) {
            return false;
        }
        StringLiteralExpression keyArg = (StringLiteralExpression) element;
        String varName = myElement.getName();
        if (varName == null || !varName.startsWith("$")) {
            return false;
        }
        varName = varName.substring(1);
        return varName.equals(keyArg.getContents());
    }
}
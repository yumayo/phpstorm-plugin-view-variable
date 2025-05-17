package com.sample.phpstormpluginviewvariable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPredefinedVariableProvider;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ViewPredefinedVariableProvider implements PhpPredefinedVariableProvider {
    @Override
    public @NotNull Set<CharSequence> getPredefinedVariables(@NotNull PhpFile phpFile) {
        String fileName = phpFile.getName();
        Log.info("Processing PHP file: " + fileName);

        // ビューファイルかどうかを判断
        if (isViewFile(phpFile)) {
            return getViewVariables(phpFile);
        }

        // その他のファイルタイプに対しては空のセットを返す
        return Set.of();
    }

    private boolean isViewFile(PhpFile phpFile) {
        VirtualFile virtualFile = phpFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        String filePath = virtualFile.getPath();
        return filePath.contains("/View/") && filePath.endsWith(".php");
    }

    private Set<CharSequence> getViewVariables(PhpFile viewFile) {
        Set<CharSequence> variables = new HashSet<>();

        var methodRefs = ControllerFile.getMethodReferences(viewFile.getVirtualFile(), viewFile.getProject());

        for (MethodReference methodRef : methodRefs) {
            if (!"setVar".equals(methodRef.getName())) {
                continue;
            }

            // 第一引数が変数名を取得
            var args = methodRef.getParameters();
            if (args.length < 2) {
                continue;
            }

            if (!(args[0] instanceof StringLiteralExpression)) {
                continue;
            }

            StringLiteralExpression keyArg = (StringLiteralExpression) args[0];
            String varName = keyArg.getContents();
            variables.add(varName);
            Log.info("Found variable in controller: " + varName);
        }

        return variables;
    }
}

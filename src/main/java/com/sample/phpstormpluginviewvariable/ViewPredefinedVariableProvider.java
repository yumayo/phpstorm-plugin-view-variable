package com.sample.phpstormpluginviewvariable;

import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPredefinedVariableProvider;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.sample.phpstormpluginviewvariable.model.ControllerFile;
import com.sample.phpstormpluginviewvariable.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * ViewPredefinedVariableProvider
 * Viewファイルで利用可能な変数名（setVarで渡された変数）を補完候補として提供するProvider。
 * コントローラーのsetVarで渡された変数名をViewファイルで補完できるようにする。
 */
public class ViewPredefinedVariableProvider implements PhpPredefinedVariableProvider {
    /**
     * Viewファイルで利用可能な変数名をセットとして返す。
     * ControllerのsetVarで渡された変数名を取得し、View側で補完候補として表示する。
     */
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

    /**
     * 指定したファイルがViewファイルかどうかを判定する。
     */
    private boolean isViewFile(PhpFile phpFile) {
        VirtualFile virtualFile = phpFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        String filePath = virtualFile.getPath();
        return filePath.contains("/views/") && filePath.endsWith(".php");
    }

    /**
     * ControllerのsetVar呼び出しから、Viewで利用可能な変数名を取得する。
     */
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

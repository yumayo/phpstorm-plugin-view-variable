package com.sample.phpstormpluginviewvariable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * ViewStringLiteralExpressionReference
 * Viewファイル内の文字列リテラル（setVarの第一引数や変数名）に対する参照解決（Go to Declaration等）を実装するクラス。
 * 例：setVar('sum', ...)やView内の$sumなどの参照元・参照先の解決を担当。
 */
public class ViewStringLiteralExpressionReference extends PsiReferenceBase<StringLiteralExpression> {
    private static final Object[] EMPTY_ARRAY = new Object[0];

    public ViewStringLiteralExpressionReference(@NotNull StringLiteralExpression element) {
        super(element);
    }

    /**
     * 参照解決（Go to Declaration等）を実装。
     * 変数名やsetVarの第一引数から、対応する変数宣言やsetVar呼び出しを解決する。
     */
    @Override
    public @Nullable PsiElement resolve() {
        Log.info("");
        Log.info("====================================================================================================");
        Log.info("resolve() called");
        String varName = myElement.getContents();
        Log.info("varName: " + varName);
        Project project = myElement.getProject();
        Log.info("project: " + project.getName());

        // Get the containing method of myElement
        Method containingMethod = PsiTreeUtil.getParentOfType(myElement, Method.class);
        if (containingMethod != null) {
            String methodName = containingMethod.getName();
            Log.info("Containing method: " + methodName);
        } else {
            Log.info("Not inside a method");
        }

        // コントローラーのクラス名からビューファイルのパスを推測
        PhpClass controllerClass = PsiTreeUtil.getParentOfType(myElement, PhpClass.class);
        if (controllerClass == null) {
            Log.info("parentClass not found");
            return null;
        }

        String className = controllerClass.getName();
        Log.info("controllerClass: " + className);

        // "Controller"サフィックスを削除してビュー名を取得
        String viewName = className.replace("Controller.php", "").toLowerCase();

        Log.info("viewName: " + viewName);

        // ビューファイルを検索
        Collection<VirtualFile> viewFiles = FilenameIndex.getVirtualFilesByName(viewName + ".php", GlobalSearchScope.projectScope(project));
        Log.info("viewFiles found: " + viewFiles.size());

        // 各ビューファイルから変数を検索
        for (VirtualFile viewFile : viewFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(viewFile);
            if (psiFile == null) {
                continue;
            }

            Collection<Variable> variables = PsiTreeUtil.findChildrenOfType(psiFile, Variable.class);
            Log.info("variables in file " + viewFile.getName() + ": " + variables.size());
            for (Variable variable : variables) {
                if (varName.equals(variable.getName())) {
                    Log.info("MATCHED variable (usage): " + variable.getName());
                    return variable;
                }
            }
        }

        Log.info("No match found");
        return null;
    }

    /**
     * 指定した要素がこの参照の対象かどうかを判定する。
     */
    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (element instanceof Variable) {
            Variable var = (Variable) element;
            return var.getName().equals(myElement.getContents());
        }
        return false;
    }

    /**
     * 補完候補の配列を返す（未使用）。
     */
    @Override
    public Object @NotNull [] getVariants() {
        return EMPTY_ARRAY;
    }
}
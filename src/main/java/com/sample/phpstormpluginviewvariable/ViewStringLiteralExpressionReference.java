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
import java.util.HashSet;
import java.util.Set;

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
     * 対応するコントローラーのアクションとビューファイルに限定して判定を行う。
     */
    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (!(element instanceof Variable)) {
            return false;
        }

        Variable var = (Variable) element;
        String varName = var.getName();
        String targetVarName = myElement.getContents();

        if (!varName.equals(targetVarName)) {
            return false;
        }

        // 変数がビューファイル内にあるか確認
        PsiFile viewFile = var.getContainingFile();
        if (viewFile == null) {
            return false;
        }

        // 現在の要素がコントローラーファイル内にあるか確認
        PsiFile controllerFile = myElement.getContainingFile();
        if (controllerFile == null) {
            return false;
        }

        // ビューファイルとコントローラーファイルの対応関係を確認
        Project project = myElement.getProject();
        VirtualFile viewVirtualFile = viewFile.getVirtualFile();
        if (viewVirtualFile == null) {
            return false;
        }

        // コントローラーのメソッド参照を取得
        Collection<MethodReference> methodRefs = ControllerFile.getMethodReferences(viewVirtualFile, project);
        
        // 現在の要素がsetVarの第一引数として使用されているか確認
        for (MethodReference methodRef : methodRefs) {
            if (!"setVar".equals(methodRef.getName())) {
                continue;
            }

            PsiElement[] args = methodRef.getParameters();
            if (args.length < 1) {
                continue;
            }

            if (args[0] == myElement) {
                return true;
            }
        }

        return false;
    }

    /**
     * 補完候補の配列を返す。
     * 対応するコントローラーのアクション内のsetVarの第一引数のみを候補として提供する。
     */
    @Override
    public Object @NotNull [] getVariants() {
        Project project = myElement.getProject();
        PsiFile controllerFile = myElement.getContainingFile();
        if (controllerFile == null) {
            return EMPTY_ARRAY;
        }

        VirtualFile controllerVirtualFile = controllerFile.getVirtualFile();
        if (controllerVirtualFile == null) {
            return EMPTY_ARRAY;
        }

        // コントローラーのメソッド参照を取得
        Collection<MethodReference> methodRefs = ControllerFile.getMethodReferences(controllerVirtualFile, project);
        Set<String> variants = new HashSet<>();

        // setVarの第一引数を収集
        for (MethodReference methodRef : methodRefs) {
            if (!"setVar".equals(methodRef.getName())) {
                continue;
            }

            PsiElement[] args = methodRef.getParameters();
            if (args.length < 1) {
                continue;
            }

            if (args[0] instanceof StringLiteralExpression) {
                StringLiteralExpression keyArg = (StringLiteralExpression) args[0];
                variants.add(keyArg.getContents());
            }
        }

        return variants.toArray();
    }
}
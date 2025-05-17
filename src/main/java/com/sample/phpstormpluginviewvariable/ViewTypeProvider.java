package com.sample.phpstormpluginviewvariable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import com.sample.phpstormpluginviewvariable.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * ViewTypeProvider
 * Viewファイル内で使用されている変数の型推論を担当するTypeProvider。
 * ControllerのsetVarで渡された値の型をView側で推論できるようにする。
 */
public class ViewTypeProvider implements PhpTypeProvider4 {

    /**
     * このTypeProviderのユニークキーを返す。
     */
    @Override
    public char getKey() {
        return 'C'; // ユニークなキーを提供（他のTypeProviderと被らないようにする）
    }

    /**
     * Viewファイル内の変数の型を推論する。
     * ControllerのsetVarで渡された値の型を取得し、View側で型補完や宣言ジャンプを可能にする。
     */
    @Nullable
    @Override
    public PhpType getType(PsiElement psiElement) {
        Log.info("getType: " + psiElement);

        if (!(psiElement instanceof Variable)) {
            return null;
        }

        // ビューファイル内の変数かを確認
        if (!isInViewFile(psiElement)) {
            return null;
        }

        // コントローラーでの変数定義を検索し、型を推論
        Variable variable = (Variable)psiElement;
        return inferVariableTypeFromController(variable);
    }

    /**
     * 指定したPsiElementがViewファイル内かどうかを判定する。
     */
    private boolean isInViewFile(PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) {
            return false;
        }
        
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        
        String filePath = virtualFile.getPath();
        return filePath.contains("/View/") && filePath.endsWith(".php");
    }

    /**
     * ControllerのsetVar呼び出しから、変数の型を推論する。
     */
    private PhpType inferVariableTypeFromController(Variable variable) {
        Log.info("inferVariableTypeFromController: " + variable);

        String varName = variable.getName();
        Project project = variable.getProject();
        PsiFile viewFile = variable.getContainingFile();
        
        if (viewFile == null) {
            Log.info("View file is null");
            return null;
        }
        
        VirtualFile viewVirtualFile = viewFile.getVirtualFile();
        if (viewVirtualFile == null) {
            Log.info("View virtual file is null");
            return null;
        }

        // ビューファイルに対応するコントローラーのメソッド参照を取得
        var methodRefs = ControllerFile.getMethodReferences(viewVirtualFile, project);

        for (MethodReference methodRef : methodRefs) {
            if (!"setVar".equals(methodRef.getName())) {
                continue;
            }

            // 第一引数が変数名と一致するか確認
            PsiElement[] args = methodRef.getParameters();
            if (args.length < 2) {
                continue;
            }

            if (!(args[0] instanceof StringLiteralExpression)) {
                continue;
            }

            StringLiteralExpression keyArg = (StringLiteralExpression) args[0];
            if (!varName.equals(keyArg.getContents())) {
                continue;
            }

            // 第二引数の型をPhpStormの型推論システムで取得
            PsiElement valueArg = args[1];
            if (valueArg instanceof com.jetbrains.php.lang.psi.elements.PhpExpression) {
                PhpType type = ((com.jetbrains.php.lang.psi.elements.PhpExpression) valueArg).getType();
                if (!type.isEmpty()) {
                    Log.info("Inferred type for " + varName + ": " + type);
                    return type;
                }
            }
        }

        Log.info("No matching setVar call found for variable: " + varName);
        return null;
    }

    /**
     * 型補完用（未実装）。
     */
    @Override
    public @Nullable PhpType complete(String s, Project project) {
        Log.info("complete: " + s);
        return null; // 実装省略
    }

    /**
     * シグネチャから要素を取得（未実装）。
     */
    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String s, Set<String> set, int i, Project project) {
        Log.info("getBySignature: " + s);
        return null; // 実装省略
    }
}

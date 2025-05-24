package com.sample.phpstormpluginviewvariable;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.completion.PhpCompletionContributor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.sample.phpstormpluginviewvariable.model.ControllerFile;
import com.sample.phpstormpluginviewvariable.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * PhpViewVariableCompletionContributor
 * PhpCompletionContributorを継承したViewファイル専用の補完コントリビューター。
 * PhpStormのPHP補完システムとより深く統合される。
 */
public class PhpViewVariableCompletionContributor extends PhpCompletionContributor {

    public PhpViewVariableCompletionContributor() {
        Log.info("PhpViewVariableCompletionContributor initialized");
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        Log.info("fillCompletionVariants called");
        
        PsiElement position = parameters.getPosition();
        
        // Viewファイルかどうかチェック
        if (!isInViewFile(position)) {
            Log.info("Not in view file, delegating to parent");
            super.fillCompletionVariants(parameters, result);
            return;
        }

        // 親クラスの処理も実行
        super.fillCompletionVariants(parameters, result);

        // 変数コンテキストでの補完かチェック
        if (isVariableContext(position)) {
            Log.info("Adding view variable completions");
            addViewVariableCompletions(position, result);
        }
    }

    /**
     * 変数コンテキストかどうかを判定する。
     */
    private boolean isVariableContext(PsiElement element) {
        // 変数要素またはその周辺をチェック
        PsiElement current = element;
        
        for (int i = 0; i < 3 && current != null; i++) {
            if (current instanceof Variable) {
                return true;
            }
            
            String text = current.getText();
            if (text != null && (text.startsWith("$") || text.equals("$"))) {
                return true;
            }
            
            // 前の要素が$の場合
            PsiElement prevSibling = current.getPrevSibling();
            if (prevSibling != null && "$".equals(prevSibling.getText())) {
                return true;
            }
            
            current = current.getParent();
        }
        
        return false;
    }

    /**
     * Viewファイルでの変数補完候補を追加する。
     */
    private void addViewVariableCompletions(PsiElement position, CompletionResultSet result) {
        // コントローラーからsetVarで設定された変数を取得
        Set<String> viewVariables = getViewVariablesFromController(position);
        
        // 各変数を補完候補として追加
        for (String varName : viewVariables) {
            LookupElementBuilder element = LookupElementBuilder.create(varName)
                    .withTypeText("from controller")
                    .withIcon(com.jetbrains.php.PhpIcons.VAR)
                    .withPresentableText("$" + varName);
            
            result.addElement(element);
            Log.info("Added PHP completion candidate: $" + varName);
        }
    }

    /**
     * 指定したPsiElementがViewファイル内かどうかを判定する。
     */
    private boolean isInViewFile(PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (!(containingFile instanceof PhpFile)) {
            return false;
        }
        
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        
        String filePath = virtualFile.getPath();
        // WindowsとUnixのパス区切り文字に対応
        String normalizedPath = filePath.replace("\\", "/");
        boolean isView = normalizedPath.contains("/views/") && filePath.endsWith(".php");
        Log.info("File path: " + filePath + " (normalized: " + normalizedPath + "), is view: " + isView);
        return isView;
    }

    /**
     * コントローラーからsetVarで設定された変数名を取得する。
     */
    private Set<String> getViewVariablesFromController(PsiElement element) {
        Set<String> variables = new HashSet<>();
        
        PsiFile viewFile = element.getContainingFile();
        if (viewFile == null) {
            return variables;
        }
        
        VirtualFile viewVirtualFile = viewFile.getVirtualFile();
        if (viewVirtualFile == null) {
            return variables;
        }
        
        Project project = element.getProject();
        
        // ビューファイルに対応するコントローラーのメソッド参照を取得
        var methodRefs = ControllerFile.getMethodReferences(viewVirtualFile, project);
        Log.info("Found " + methodRefs.size() + " method references from controller");
        
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
            Log.info("Found variable from controller for PHP completion: " + varName);
        }
        
        return variables;
    }
}

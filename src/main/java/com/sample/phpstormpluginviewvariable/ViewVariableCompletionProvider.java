package com.sample.phpstormpluginviewvariable;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.sample.phpstormpluginviewvariable.model.ControllerFile;
import com.sample.phpstormpluginviewvariable.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ViewVariableCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        PsiElement position = parameters.getPosition();
        Log.info("Completion requested at position: " + position + " in file: " + position.getContainingFile().getName());

        // Viewファイルかどうかチェック
        if (!isInViewFile(position)) {
            Log.info("Not in view file, skipping completion");
            return;
        }

        // 変数コンテキストかどうかをより厳密にチェック
        if (!isVariableCompletionContext(position, parameters)) {
            Log.info("Not in variable completion context, skipping");
            return;
        }

        Log.info("Adding view variable completions for InvocationCount: " + parameters.getInvocationCount());

        // コントローラーからsetVarで設定された変数を取得
        Map<String, String> viewVariables = getViewVariablesFromController(position);

        // 各変数を補完候補として追加
        for (Map.Entry<String, String> entry : viewVariables.entrySet()) {
            String varName = entry.getKey();
            String type = entry.getValue();
            
            LookupElementBuilder element = LookupElementBuilder.create("$" + varName)
                    .withTypeText(type)
                    .withIcon(PhpIcons.VARIABLE)
                    .withPresentableText(varName);

            result.addElement(element);
            Log.info("Added completion candidate: $" + varName + " with type: " + type);
        }
    }

    /**
     * 変数補完のコンテキストかどうかを判定する。
     */
    private boolean isVariableCompletionContext(@NotNull PsiElement position, @NotNull CompletionParameters parameters) {
        // 元のテキストをチェック
        String originalText = parameters.getOriginalFile().getText();
        int offset = parameters.getOffset();

        // カーソルの前の文字をチェック
        if (offset > 0 && originalText.charAt(offset - 1) == '$') {
            Log.info("Variable completion context: after $ symbol");
            return true;
        }

        // 現在の要素が変数であるかチェック
        PsiElement current = position;
        for (int i = 0; i < 5 && current != null; i++) {
            if (current instanceof Variable) {
                Log.info("Variable completion context: in Variable element");
                return true;
            }

            String text = current.getText();
            if (text != null && text.startsWith("$")) {
                Log.info("Variable completion context: text starts with $");
                return true;
            }

            current = current.getParent();
        }

        // 前のトークンが$かチェック
        PsiElement prevLeaf = com.intellij.psi.util.PsiTreeUtil.prevLeaf(position);
        if (prevLeaf != null && "$".equals(prevLeaf.getText().trim())) {
            Log.info("Variable completion context: previous token is $");
            return true;
        }

        Log.info("Not a variable completion context");
        return false;
    }

    /**
     * 指定したPsiElementがViewファイル内かどうかを判定する。
     */
    private boolean isInViewFile(PsiElement element) {
        PsiFile containingFile = element.getContainingFile();

        // オリジナルファイルを取得
        PsiFile originalFile = containingFile.getOriginalFile();
        VirtualFile virtualFile = originalFile.getVirtualFile();
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
     * コントローラーからsetVarで設定された変数名と型を取得する。
     */
    private Map<String, String> getViewVariablesFromController(PsiElement element) {
        Map<String, String> variables = new HashMap<>();

        PsiFile containingFile = element.getContainingFile();

        // オリジナルファイルを取得
        PsiFile originalFile = containingFile.getOriginalFile();
        VirtualFile virtualFile = originalFile.getVirtualFile();
        if (virtualFile == null) {
            return variables;
        }

        Project project = element.getProject();

        // ビューファイルに対応するコントローラーのメソッド参照を取得
        var methodRefs = ControllerFile.getMethodReferences(virtualFile, project);
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

            // 第二引数の型をPhpStormの型推論システムで取得
            String type = "mixed";
            if (args[1] instanceof com.jetbrains.php.lang.psi.elements.PhpExpression valueArg) {
                PhpType phpType = valueArg.getType();
                type = getSafeTypeString(phpType);
            }

            variables.put(varName, type);
            Log.info("Found variable from controller for completion: " + varName + " with type: " + type);
        }

        return variables;
    }

    /**
     * PhpTypeから安全な文字列表現を取得
     */
    private String getSafeTypeString(PhpType type) {
        try {
            // まずtoStringResolvedを試す
            String resolved = type.toStringResolved();
            if (resolved != null && !resolved.isEmpty() && isValidString(resolved)) {
                return cleanTypeString(resolved);
            }
        } catch (Exception e) {
            Log.info("Error getting resolved type string: " + e.getMessage());
        }

        try {
            // 次にgetTypesを使って個別の型を取得
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String typeStr : type.getTypes()) {
                if (typeStr != null && isValidString(typeStr)) {
                    if (!first) {
                        sb.append("|");
                    }
                    sb.append(cleanTypeString(typeStr));
                    first = false;
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        } catch (Exception e) {
            Log.info("Error getting individual types: " + e.getMessage());
        }

        return "mixed"; // フォールバック
    }

    /**
     * 文字列が有効な文字のみを含むかチェック
     */
    private boolean isValidString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // ASCII文字、基本的なUTF-8文字、PHPの型名で使われる文字のみ許可
        for (char c : str.toCharArray()) {
            if (c < 32 || c > 126) { // 基本的なASCII範囲外
                if (c != '|' && c != '?' && c != '[' && c != ']' && c != '\\') {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * 型文字列をクリーンアップ（先頭のバックスラッシュを除去）
     */
    private String cleanTypeString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return typeStr;
        }
        
        // 先頭のバックスラッシュを除去
        if (typeStr.startsWith("\\")) {
            typeStr = typeStr.substring(1);
        }
        
        // ユニオン型の各部分をクリーンアップ
        if (typeStr.contains("|")) {
            String[] parts = typeStr.split("\\|");
            StringBuilder cleaned = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    cleaned.append("|");
                }
                String part = parts[i].trim();
                if (part.startsWith("\\")) {
                    part = part.substring(1);
                }
                cleaned.append(part);
            }
            return cleaned.toString();
        }
        
        return typeStr;
    }
}

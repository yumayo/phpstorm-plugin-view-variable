package com.sample.phpstormpluginviewvariable;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.sample.phpstormpluginviewvariable.model.ControllerFile;
import com.sample.phpstormpluginviewvariable.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * ViewVariableCompletionContributor
 * Viewファイルでの変数のオートコンプリート候補を提供するContributor。
 * コントローラーのsetVarで渡された変数名を候補として表示する。
 */
public class ViewVariableCompletionContributor extends CompletionContributor {

    public ViewVariableCompletionContributor() {
        Log.info("ViewVariableCompletionContributor initialized");
        
        // PHP変数の直接的なパターンマッチング
        extend(CompletionType.BASIC,
                PhpPatterns.psiElement(PhpTokenTypes.VARIABLE)
                        .withLanguage(com.jetbrains.php.lang.PhpLanguage.INSTANCE),
                new ViewVariableCompletionProvider());
    }

    private static class ViewVariableCompletionProvider extends CompletionProvider<CompletionParameters> {
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

            Log.info("Adding view variable completions");

            // コントローラーからsetVarで設定された変数を取得
            Set<String> viewVariables = getViewVariablesFromController(position);
            
            // 各変数を補完候補として追加
            for (String varName : viewVariables) {
                LookupElementBuilder element = LookupElementBuilder.create(varName)
                        .withTypeText("from controller")
                        .withIcon(PhpIcons.VARIABLE)
                        .withPresentableText("$" + varName)
                        .withInsertHandler((insertionContext, item) -> {
                            // 必要に応じてカスタムインサート処理
                        });
                
                result.addElement(element);
                Log.info("Added completion candidate: $" + varName);
            }
        }

        /**
         * 変数補完のコンテキストかどうかを判定する。
         */
        private boolean isVariableCompletionContext(PsiElement position, CompletionParameters parameters) {
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
         * コントローラーからsetVarで設定された変数名を取得する。
         */
        private Set<String> getViewVariablesFromController(PsiElement element) {
            Set<String> variables = new HashSet<>();

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
                variables.add(varName);
                Log.info("Found variable from controller for completion: " + varName);
            }
            
            return variables;
        }
    }
}

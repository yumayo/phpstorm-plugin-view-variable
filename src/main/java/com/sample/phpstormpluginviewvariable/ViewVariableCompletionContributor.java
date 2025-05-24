package com.sample.phpstormpluginviewvariable;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
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
        
        // PHP変数補完のパターンに特化
        extend(CompletionType.BASIC,
                PhpPatterns.psiElement(PhpTokenTypes.VARIABLE)
                        .withLanguage(com.jetbrains.php.lang.PhpLanguage.INSTANCE),
                new ViewVariableCompletionProvider());
                
        // $記号の後での補完もサポート  
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .afterLeaf(PlatformPatterns.psiElement(PhpTokenTypes.VARIABLE_MARKER))
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

            Log.info("Adding view variable completions");

            // コントローラーからsetVarで設定された変数を取得
            Set<String> viewVariables = getViewVariablesFromController(position);
            
            // 各変数を補完候補として追加
            for (String varName : viewVariables) {
                LookupElementBuilder element = LookupElementBuilder.create(varName)
                        .withTypeText("from controller")
                        .withIcon(com.jetbrains.php.PhpIcons.VAR)
                        .withPresentableText("$" + varName);
                
                result.addElement(element);
                Log.info("Added completion candidate: $" + varName);
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
                Log.info("Found variable from controller for completion: " + varName);
            }
            
            return variables;
        }
    }
}

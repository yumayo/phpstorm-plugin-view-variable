package com.sample.phpstormpluginviewvariable;

import com.intellij.codeInsight.completion.*;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.sample.phpstormpluginviewvariable.util.Log;

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
                PhpPatterns.psiElement(PhpTokenTypes.VARIABLE),
                new ViewVariableCompletionProvider());
    }
}

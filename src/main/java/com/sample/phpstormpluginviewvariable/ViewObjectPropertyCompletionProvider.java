package com.sample.phpstormpluginviewvariable;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.sample.phpstormpluginviewvariable.model.ControllerFile;
import com.sample.phpstormpluginviewvariable.model.ViewFile;
import com.sample.phpstormpluginviewvariable.util.Log;
import com.sample.phpstormpluginviewvariable.util.PhpTypeString;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * ViewObjectPropertyCompletionProvider
 * Viewファイルでのオブジェクトプロパティ・メソッドのオートコンプリート候補を提供するProvider。
 * $object->property の形でのプロパティ補完を実現する。
 */
public class ViewObjectPropertyCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        
        PsiElement position = parameters.getPosition();
        Log.info("ViewObjectPropertyCompletionProvider called at position: " + position.getText());
        
        // アロー演算子の直前の変数を取得
        Variable variable = getVariableBeforeArrow(position);
        if (variable == null) {
            Log.info("No variable found before arrow");
            return;
        }
        
        String variableName = variable.getName();
        Log.info("Variable name: " + variableName);
        
        // 変数の型を取得
        PhpType variableType = getVariableTypeFromController(variable, variableName);
        if (variableType == null || variableType.isEmpty()) {
            Log.info("No type found for variable: " + variableName);
            return;
        }
        
        Log.info("Variable type: " + PhpTypeString.getSafeTypeString(variableType));
        
        // 型からクラスを解決してプロパティ・メソッドを取得
        addPropertyAndMethodCompletions(variableType, result, position.getProject());
    }
    
    /**
     * アロー演算子の直前の変数を取得
     */
    private Variable getVariableBeforeArrow(PsiElement position) {
        // position -> identifier after arrow
        // position.parent -> field reference
        // field reference の firstChild -> variable
        
        PsiElement parent = position.getParent();
        if (parent instanceof FieldReference) {
            FieldReference fieldRef = (FieldReference) parent;
            PsiElement classReference = fieldRef.getClassReference();
            if (classReference instanceof Variable) {
                return (Variable) classReference;
            }
        }
        
        return null;
    }
    
    /**
     * コントローラーから変数の型を取得
     */
    private PhpType getVariableTypeFromController(Variable variable, String variableName) {
        Log.info("getVariableTypeFromController called for variable: " + variableName);

        PsiFile viewFile = variable.getContainingFile();
        if (viewFile == null) {
            Log.info("ViewFile is null, returning null");
            return null;
        }

        VirtualFile viewVirtualFile = viewFile.getOriginalFile().getVirtualFile();
        if (viewVirtualFile == null) {
            Log.info("viewVirtualFile is null, returning null");
            return null;
        }

        Project project = variable.getProject();
        Log.info("Project base path: " + (project.getBasePath() != null ? project.getBasePath() : "null"));
        Log.info("Final VirtualFile path: " + viewVirtualFile.getPath());
        Log.info("VirtualFile name: " + viewVirtualFile.getName());
        Log.info("VirtualFile parent: " + (viewVirtualFile.getParent() != null ? viewVirtualFile.getParent().getPath() : "null"));

        if (!ViewFile.isViewFile(viewVirtualFile.getPath())) {
            Log.info("Not a view file. path=" + viewVirtualFile.getPath());
            return null;
        }
        
        // コントローラーのsetVar呼び出しを取得
        Collection<MethodReference> methodRefs = ControllerFile.getMethodReferences(viewVirtualFile, project);
        Log.info("Found " + methodRefs.size() + " method references from controller");

        for (MethodReference methodRef : methodRefs) {
            Log.info("Processing method reference: " + methodRef.getName());
            if (!"setVar".equals(methodRef.getName())) {
                Log.info("Method is not setVar, continuing");
                continue;
            }
            
            PsiElement[] args = methodRef.getParameters();
            Log.info("setVar has " + args.length + " arguments");
            if (args.length < 2) {
                Log.info("setVar has less than 2 arguments, continuing");
                continue;
            }
            
            if (!(args[0] instanceof StringLiteralExpression)) {
                Log.info("First argument is not StringLiteralExpression, continuing");
                continue;
            }
            
            StringLiteralExpression keyArg = (StringLiteralExpression) args[0];
            String keyValue = keyArg.getContents();
            Log.info("setVar key: '" + keyValue + "', looking for: '" + variableName + "'");
            if (!variableName.equals(keyValue)) {
                Log.info("Variable name does not match, continuing");
                continue;
            }
            
            // 第二引数の型を取得
            PsiElement valueArg = args[1];
            Log.info("Found matching setVar, value arg type: " + valueArg.getClass().getSimpleName());
            if (valueArg instanceof PhpExpression) {
                PhpType type = ((PhpExpression) valueArg).getType();
                Log.info("Original type: " + (type != null ? PhpTypeString.getSafeTypeString(type) : "null"));
                
                // メソッド参照の場合、実際の戻り値の型を解決
                PhpType resolvedType = resolveMethodReturnType(type, valueArg);
                if (resolvedType != null && !resolvedType.isEmpty()) {
                    Log.info("Resolved type: " + PhpTypeString.getSafeTypeString(resolvedType));
                    return resolvedType;
                }
                
                Log.info("Returning original type: " + (type != null ? PhpTypeString.getSafeTypeString(type) : "null"));
                return type;
            } else {
                Log.info("Value argument is not PhpExpression, continuing");
            }
        }

        // foreachループ内の変数かチェック
        PhpType foreachType = getForeachVariableType(variable, variableName);
        if (foreachType != null) {
            Log.info("Found foreach type, returning: " + PhpTypeString.getSafeTypeString(foreachType));
            return foreachType;
        }
        Log.info("No foreach type found");
        
        Log.info("No matching setVar found, returning null");
        return null;
    }
    
    /**
     * メソッド参照の戻り値の型を解決
     */
    private PhpType resolveMethodReturnType(PhpType originalType, PsiElement valueArg) {
        Log.info("resolveMethodReturnType called");
        
        if (originalType == null || originalType.isEmpty()) {
            Log.info("Original type is null or empty");
            return null;
        }
        
        // プリミティブ型の場合はそのまま返す
        if (originalType.getTypes().stream().allMatch(PhpType::isPrimitiveType)) {
            Log.info("Type is primitive, returning as is");
            return originalType;
        }
        
        // メソッド参照の場合、実際のメソッドを解決して戻り値の型を取得
        if (valueArg instanceof MethodReference) {
            MethodReference methodRef = (MethodReference) valueArg;
            Log.info("Resolving method reference: " + methodRef.getName());
            
            // メソッドの解決を試行
            try {
                PsiElement resolved = methodRef.resolve();
                if (resolved instanceof Method) {
                    Method method = (Method) resolved;
                    PhpType returnType = method.getType();
                    Log.info("Method return type: " + (returnType != null ? PhpTypeString.getSafeTypeString(returnType) : "null"));
                    return returnType;
                } else {
                    Log.info("Resolved element is not a Method: " + (resolved != null ? resolved.getClass().getSimpleName() : "null"));
                }
            } catch (Exception e) {
                Log.info("Error resolving method: " + e.getMessage());
            }
        }
        
        // 変数参照の場合、その変数の型を取得
        if (valueArg instanceof Variable) {
            Variable var = (Variable) valueArg;
            Log.info("Resolving variable: " + var.getName());
            PhpType varType = var.getType();
            Log.info("Variable type: " + (varType != null ? PhpTypeString.getSafeTypeString(varType) : "null"));
            return varType;
        }
        
        Log.info("Could not resolve method return type, returning original");
        return originalType;
    }
    
    /**
     * foreachループ内の変数の型を取得
     */
    private PhpType getForeachVariableType(Variable variable, String variableName) {
        // 変数が含まれるforeachステートメントを探す
        ForeachStatement foreach = PsiTreeUtil.getParentOfType(variable, ForeachStatement.class);
        if (foreach == null) {
            Log.info("Variable is not inside foreach loop");
            return null;
        }

        // foreachの値変数が一致するかチェック
        Variable valueVariable = foreach.getValue();
        if (valueVariable == null || !variableName.equals(valueVariable.getName())) {
            Log.info("Variable name does not match foreach value variable");
            return null;
        }

        Log.info("Found foreach loop for variable: " + variableName);

        // 配列変数を取得
        PsiElement arrayExpression = foreach.getArray();
        if (!(arrayExpression instanceof Variable)) {
            Log.info("Foreach array is not a simple variable");
            return null;
        }

        Variable arrayVariable = (Variable) arrayExpression;
        String arrayVariableName = arrayVariable.getName();
        Log.info("Foreach array variable: " + arrayVariableName);

        // 配列変数の型をコントローラーから取得
        Log.info("About to call getVariableTypeFromController for: " + arrayVariableName);
        PhpType arrayType = getVariableTypeFromController(variable, arrayVariableName);
        if (arrayType == null) {
            Log.info("No type found for array variable: " + arrayVariableName);
            Log.info("Trying to get type from ViewTypeProvider...");
            // ViewTypeProviderを使って型を解決
            arrayType = getTypeFromViewTypeProvider(arrayVariable, arrayVariableName);
            Log.info("Array variable type from ViewTypeProvider: " + (arrayType != null ? PhpTypeString.getSafeTypeString(arrayType) : "null"));

            if (arrayType == null) {
                Log.info("Trying to get type from current file context...");
                // 最後の手段として現在のファイルコンテキストから取得
                arrayType = arrayVariable.getType();
                Log.info("Array variable type from context: " + (arrayType != null ? PhpTypeString.getSafeTypeString(arrayType) : "null"));
            }
        }

        if (arrayType == null) {
            Log.info("Still no type found for array variable");
            return null;
        }

        Log.info("Array type: " + PhpTypeString.getSafeTypeString(arrayType));

        // 配列型から要素型を推論
        return getElementTypeFromArrayType(arrayType, variable.getProject());
    }

    /**
     * ViewTypeProviderを使って変数の型を取得
     */
    private PhpType getTypeFromViewTypeProvider(Variable variable, String variableName) {
        try {
            // ViewTypeProviderのインスタンスを作成
            ViewTypeProvider viewTypeProvider = new ViewTypeProvider();

            // 変数の型を取得
            PhpType type = viewTypeProvider.getType(variable);
            Log.info("ViewTypeProvider returned type: " + (type != null ? PhpTypeString.getSafeTypeString(type) : "null"));

            return type;
        } catch (Exception e) {
            Log.info("Error getting type from ViewTypeProvider: " + e.getMessage());
            return null;
        }
    }

    /**
     * 配列型から要素型を推論
     */
    private PhpType getElementTypeFromArrayType(PhpType arrayType, Project project) {
        Log.info("getElementTypeFromArrayType called with: " + PhpTypeString.getSafeTypeString(arrayType));
        
        // フォールバック: 手動で型文字列を解析
        for (String typeName : arrayType.getTypes()) {
            Log.info("Processing array type: " + typeName);

            // Quest[] -> Quest に変換
            if (typeName.endsWith("[]")) {
                String elementTypeName = typeName.substring(0, typeName.length() - 2);
                Log.info("Extracted element type: " + elementTypeName);
                return PhpType.builder().add(elementTypeName).build();
            }

            // array<Quest> -> Quest に変換
            if (typeName.startsWith("array<") && typeName.endsWith(">")) {
                String elementTypeName = typeName.substring(6, typeName.length() - 1);
                Log.info("Extracted element type from array<>: " + elementTypeName);
                return PhpType.builder().add(elementTypeName).build();
            }

            // array|Quest[] のような複合型を処理
            if (typeName.contains("|")) {
                String[] types = typeName.split("\\|");
                for (String type : types) {
                    if (type.endsWith("[]")) {
                        String elementTypeName = type.substring(0, type.length() - 2);
                        Log.info("Extracted element type from union: " + elementTypeName);
                        return PhpType.builder().add(elementTypeName).build();
                    }
                }
            }

            // メソッド参照型: #M#C\App\modules\GmTool\Model\Episode.getQuests
            // PhpIndexを使用してメソッドを直接解決
            if (typeName.startsWith("#M#C\\")) {
                Log.info("Processing method reference type: " + typeName);
                PhpType resolvedType = resolveMethodReferenceType(typeName, project);
                if (resolvedType != null) {
                    Log.info("Resolved method reference to type: " + PhpTypeString.getSafeTypeString(resolvedType));
                    // 解決された型から直接要素型を抽出（無限再帰を避けるため、メソッド参照ではない型として処理）
                    for (String resolvedTypeName : resolvedType.getTypes()) {
                        Log.info("Processing resolved type: " + resolvedTypeName);
                        
                        // Quest[] -> Quest に変換
                        if (resolvedTypeName.endsWith("[]")) {
                            String elementTypeName = resolvedTypeName.substring(0, resolvedTypeName.length() - 2);
                            Log.info("Extracted element type from resolved array type: " + elementTypeName);
                            return PhpType.builder().add(elementTypeName).build();
                        }
                        
                        // array<Quest> -> Quest に変換
                        if (resolvedTypeName.startsWith("array<") && resolvedTypeName.endsWith(">")) {
                            String elementTypeName = resolvedTypeName.substring(6, resolvedTypeName.length() - 1);
                            Log.info("Extracted element type from resolved array<> type: " + elementTypeName);
                            return PhpType.builder().add(elementTypeName).build();
                        }
                        
                        // 通常のクラス名の場合、そのまま返す（単一要素として扱う）
                        if (resolvedTypeName.contains("\\") && !resolvedTypeName.startsWith("#")) {
                            Log.info("Treating resolved type as element type: " + resolvedTypeName);
                            return PhpType.builder().add(resolvedTypeName).build();
                        }
                    }
                }
                continue;
            }

            // 関数シグネチャ型の処理
            if (typeName.startsWith("#π(") && typeName.contains(")")) {
                int startIndex = 3; // "#π("をスキップ
                int endIndex = typeName.indexOf(")", startIndex);
                if (endIndex > startIndex) {
                    String innerType = typeName.substring(startIndex, endIndex);
                    Log.info("Extracted inner type from function signature: " + innerType);
                    // 内部の型を再帰的に処理
                    PhpType innerPhpType = PhpType.builder().add(innerType).build();
                    PhpType result = getElementTypeFromArrayType(innerPhpType, project);
                    if (result != null) {
                        return result;
                    }
                }
            }

            // その他の複雑な型フォーマットからクラス名を抽出
            if (typeName.contains("\\") && !typeName.startsWith("#")) {
                // シンプルなクラス名の場合、この型の配列として扱う
                Log.info("Treating as array element type: " + typeName);
                return PhpType.builder().add(typeName).build();
            }
        }

        Log.info("Could not extract element type from array type");
        return null;
    }

    /**
     * メソッド参照型を解決してメソッドの戻り値型を取得
     * 例: #M#C\App\modules\GmTool\Model\Episode.getQuests -> Quest[]
     */
    private PhpType resolveMethodReferenceType(String methodReferenceType, Project project) {
        Log.info("resolveMethodReferenceType called with: " + methodReferenceType);
        
        if (!methodReferenceType.startsWith("#M#C\\")) {
            Log.info("Not a method reference type");
            return null;
        }
        
        // #M#C\App\modules\GmTool\Model\Episode.getQuests から クラス名とメソッド名を抽出
        String methodRef = methodReferenceType.substring(4); // "#M#C\"を除去
        if (!methodRef.contains(".")) {
            Log.info("Invalid method reference format");
            return null;
        }
        
        String className = methodRef.substring(0, methodRef.lastIndexOf("."));
        String methodName = methodRef.substring(methodRef.lastIndexOf(".") + 1);
        Log.info("Extracted class: " + className + ", method: " + methodName);
        
        try {
            // PhpIndexを使用してクラスを検索
            Collection<PhpClass> classes = com.jetbrains.php.PhpIndex.getInstance(project)
                    .getClassesByName(className.substring(className.lastIndexOf("\\") + 1));
            
            Log.info("Found " + classes.size() + " classes for short name");
            
            for (PhpClass phpClass : classes) {
                Log.info("Checking class: " + phpClass.getFQN());
                if (phpClass.getFQN().equals(className)) {
                    Log.info("Found matching class: " + className);
                    
                    // メソッドを検索
                    Method method = phpClass.findMethodByName(methodName);
                    if (method != null) {
                        PhpType returnType = method.getType();
                        Log.info("Found method " + methodName + " with return type: " + 
                               (returnType != null ? PhpTypeString.getSafeTypeString(returnType) : "null"));
                        return returnType;
                    } else {
                        Log.info("Method " + methodName + " not found in class " + className);
                    }
                }
            }
            
            // 短縮名で見つからない場合、FQNで直接検索
            if (classes.isEmpty()) {
                Log.info("Trying FQN search for: " + className);
                classes = com.jetbrains.php.PhpIndex.getInstance(project).getClassesByFQN(className);
                Log.info("Found " + classes.size() + " classes by FQN");
                
                for (PhpClass phpClass : classes) {
                    Method method = phpClass.findMethodByName(methodName);
                    if (method != null) {
                        PhpType returnType = method.getType();
                        Log.info("Found method " + methodName + " with return type: " + 
                               (returnType != null ? PhpTypeString.getSafeTypeString(returnType) : "null"));
                        return returnType;
                    }
                }
            }
            
        } catch (Exception e) {
            Log.info("Error resolving method reference: " + e.getMessage());
        }
        
        Log.info("Could not resolve method reference: " + methodReferenceType);
        return null;
    }
    
    /**
     * 型からプロパティとメソッドの補完候補を追加
     */
    private void addPropertyAndMethodCompletions(PhpType type, CompletionResultSet result, Project project) {
        Log.info("addPropertyAndMethodCompletions called with type: " + PhpTypeString.getSafeTypeString(type));
        
        // 型名からPhpClassを解決
        for (String typeName : type.getTypes()) {
            String cleanTypeName = PhpTypeString.cleanTypeString(typeName);
            Log.info("Looking for class: " + cleanTypeName);
            
            try {
                // PhpClassを検索
                Collection<PhpClass> classes = com.jetbrains.php.PhpIndex.getInstance(project)
                        .getClassesByName(cleanTypeName);
                
                Log.info("Found " + classes.size() + " classes for name: " + cleanTypeName);
                
                if (classes.isEmpty()) {
                    Log.info("No classes found, trying with different name variations");
                    // 最後のクラス名部分だけで検索してみる
                    String shortName = cleanTypeName;
                    if (cleanTypeName.contains("\\")) {
                        shortName = cleanTypeName.substring(cleanTypeName.lastIndexOf("\\") + 1);
                        Log.info("Trying with short name: " + shortName);
                        classes = com.jetbrains.php.PhpIndex.getInstance(project).getClassesByName(shortName);
                        Log.info("Found " + classes.size() + " classes for short name: " + shortName);
                    }
                }
                
                for (PhpClass phpClass : classes) {
                    Log.info("Processing class: " + phpClass.getName() + " (FQN: " + phpClass.getFQN() + ")");
                    
                    // プロパティを追加
                    Field[] fields = phpClass.getOwnFields();
                    Log.info("Class has " + fields.length + " own fields");
                    
                    for (Field field : fields) {
                        Log.info("Processing field: " + field.getName() + " (modifier: " + field.getModifier() + ")");
                        if (field.getModifier().isPublic()) {
                            LookupElementBuilder element = LookupElementBuilder.create(field.getName())
                                    .withIcon(PhpIcons.FIELD)
                                    .withTypeText(PhpTypeString.getSafeTypeString(field.getType()));
                            result.addElement(element);
                            Log.info("Added property: " + field.getName());
                        } else {
                            Log.info("Skipped private field: " + field.getName());
                        }
                    }

                    // メソッドを追加
                    Method[] methods = phpClass.getOwnMethods();
                    Log.info("Class has " + methods.length + " own methods");
                    
                    for (Method method : methods) {
                        Log.info("Processing method: " + method.getName() + " (modifier: " + method.getModifier() + ")");
                        if (method.getModifier().isPublic() && !method.getName().startsWith("__")) {
                            LookupElementBuilder element = LookupElementBuilder.create(method.getName())
                                    .withIcon(PhpIcons.METHOD)
                                    .withTypeText(PhpTypeString.getSafeTypeString(method.getType()))
                                    .withTailText("()")
                                    .withInsertHandler(new InsertHandler<LookupElement>() {
                                        @Override
                                        public void handleInsert(InsertionContext context, LookupElement item) {
                                            // メソッド名の後に括弧を追加
                                            context.getDocument().insertString(context.getTailOffset(), "()");
                                            // カーソルを括弧の中に移動
                                            context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
                                        }
                                    });
                            result.addElement(element);
                            Log.info("Added method: " + method.getName());
                        } else {
                            Log.info("Skipped private/magic method: " + method.getName());
                        }
                    }
                }
            } catch (Exception e) {
                Log.info("Error processing class " + cleanTypeName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        Log.info("addPropertyAndMethodCompletions completed");
    }
}
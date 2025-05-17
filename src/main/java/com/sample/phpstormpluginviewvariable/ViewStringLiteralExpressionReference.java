package com.sample.phpstormpluginviewvariable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.sample.phpstormpluginviewvariable.util.Log;
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

        // 現在のファイルがコントローラーファイルかビューファイルかを判定
        PsiFile currentFile = myElement.getContainingFile();
        if (currentFile == null) {
            return null;
        }

        String filePath = currentFile.getVirtualFile().getPath();
        boolean isControllerFile = filePath.contains("/Controller/");
        boolean isViewFile = filePath.contains("/View/");

        if (isControllerFile) {
            // コントローラーファイルからビューファイルへのジャンプ
            return resolveToViewFile(currentFile, varName, project);
        } else if (isViewFile) {
            // ビューファイルからコントローラーファイルへのジャンプ
            return resolveToControllerFile(currentFile, varName, project);
        }

        return null;
    }

    /**
     * コントローラーファイルからビューファイルへのジャンプを実装
     */
    private PsiElement resolveToViewFile(PsiFile controllerFile, String varName, Project project) {
        // コントローラーファイルのパス例: /modules/Cli/Controller/Debug/TestController.php
        String controllerPath = controllerFile.getVirtualFile().getPath();
        String fileName = controllerFile.getVirtualFile().getName(); // TestController.php

        // サブディレクトリとコントローラー名を取得
        String[] split = controllerPath.split("/Controller/");
        String subDir = "";
        String controllerClassName = fileName.replace("Controller.php", ""); // Test
        if (split.length == 2) {
            String afterController = split[1]; // 例: Debug/TestController.php
            int slashIdx = afterController.lastIndexOf("/");
            if (slashIdx > 0) {
                subDir = afterController.substring(0, slashIdx); // 例: Debug
            }
        }

        // setVarの呼び出し元メソッド（アクション名）を取得
        Method containingMethod = PsiTreeUtil.getParentOfType(myElement, Method.class);
        String actionName = "index"; // デフォルト
        if (containingMethod != null) {
            String methodName = containingMethod.getName();
            if (methodName.endsWith("Action")) {
                actionName = methodName.substring(0, methodName.length() - "Action".length());
            } else {
                actionName = methodName;
            }
        }

        // Viewファイルのパスを組み立て
        String viewPath = controllerPath.substring(0, controllerPath.indexOf("/Controller/")) + "/View/";
        if (!subDir.isEmpty()) {
            viewPath += subDir + "/";
        }
        viewPath += controllerClassName.toLowerCase() + "/" + actionName + ".php";

        VirtualFile viewVirtualFile = LocalFileSystem.getInstance().findFileByPath(viewPath);
        if (viewVirtualFile == null) {
            Log.info("View file not found: " + viewPath);
            return null;
        }

        PsiFile viewFile = PsiManager.getInstance(project).findFile(viewVirtualFile);
        if (viewFile == null) {
            return null;
        }

        // ビューファイル内の変数を検索
        Collection<Variable> variables = PsiTreeUtil.findChildrenOfType(viewFile, Variable.class);
        for (Variable variable : variables) {
            if (varName.equals(variable.getName())) {
                Log.info("Found variable in view file: " + variable.getName());
                return variable;
            }
        }

        return null;
    }

    /**
     * ビューファイルからコントローラーファイルへのジャンプを実装
     */
    private PsiElement resolveToControllerFile(PsiFile viewFile, String varName, Project project) {
        // ビューファイルのパスからコントローラーファイルのパスを推測
        String viewPath = viewFile.getVirtualFile().getPath();
        String controllerPath = viewPath.replace("/View/", "/Controller/");
        controllerPath = controllerPath.replace(".php", "Controller.php");

        VirtualFile controllerVirtualFile = LocalFileSystem.getInstance().findFileByPath(controllerPath);
        if (controllerVirtualFile == null) {
            Log.info("Controller file not found: " + controllerPath);
            return null;
        }

        PsiFile controllerFile = PsiManager.getInstance(project).findFile(controllerVirtualFile);
        if (controllerFile == null) {
            return null;
        }

        // コントローラーファイル内のsetVar呼び出しを検索
        Collection<MethodReference> methodRefs = PsiTreeUtil.findChildrenOfType(controllerFile, MethodReference.class);
        for (MethodReference methodRef : methodRefs) {
            if (!"setVar".equals(methodRef.getName())) {
                continue;
            }

            PsiElement[] args = methodRef.getParameters();
            if (args.length < 2) {
                continue;
            }

            if (!(args[0] instanceof StringLiteralExpression)) {
                continue;
            }

            StringLiteralExpression keyArg = (StringLiteralExpression) args[0];
            if (varName.equals(keyArg.getContents())) {
                Log.info("Found setVar call in controller: " + methodRef.getName());
                return methodRef;
            }
        }

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
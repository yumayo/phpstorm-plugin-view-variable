package com.sample.phpstormpluginviewvariable.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.sample.phpstormpluginviewvariable.util.Log;

import java.util.Collection;
import java.util.HashSet;

/**
 * ControllerFile
 * Viewファイルと対応するControllerファイル・アクションを特定し、
 * そのアクション内で呼ばれているsetVarのMethodReferenceを取得するユーティリティクラス。
 */
public class ControllerFile {
    /**
     * 指定したViewファイル（VirtualFile）とProjectから、
     * 対応するControllerファイルのアクション内で呼ばれているsetVarのMethodReferenceを返す。
     * Viewファイル名からアクション名を推測し、Controllerファイルの該当メソッドのみを対象とする。
     */
    public static Collection<MethodReference> getMethodReferences(VirtualFile viewVirtualFile, Project project) {
        // ビューファイルのパスからコントローラーファイルのパスを推測
        String viewPath = viewVirtualFile.getPath();
        // WindowsとUnixのパス区切り文字を正規化
        String normalizedViewPath = viewPath.replace("\\", "/");
        Log.info("View path: " + viewPath + " (normalized: " + normalizedViewPath + ")");

        // ビューファイル名からアクション名を取得
        String viewFileName = viewVirtualFile.getName();
        String baseActionName = viewFileName.replace(".php", "");
        String actionName = toCamelCase(baseActionName) + "Action";
        Log.info("Action name: " + actionName);

        int viewIndex = normalizedViewPath.indexOf("/views/");
        if (viewIndex == -1) {
            Log.info("Not a view file: " + normalizedViewPath);
            return new HashSet<>();
        }
        String viewSubPath = normalizedViewPath.substring(viewIndex + "/views/".length());
        Log.info("View sub path: " + viewSubPath);

        String[] pathParts = viewSubPath.split("/");
        Log.info("Path parts: " + String.join(", ", pathParts));

        String controllerDir;
        String controllerFileName;
        if (pathParts.length == 2) {
            controllerDir = "";
            String controllerName = toPascalCase(pathParts[0]);
            controllerFileName = controllerName + "Controller.php";
        } else if (pathParts.length == 3) {
            controllerDir = toPascalCase(pathParts[0]) + "/";
            String controllerName = toPascalCase(pathParts[1]);
            controllerFileName = controllerName + "Controller.php";
        } else {
            Log.info("Unsupported path structure: " + pathParts.length + " parts");
            return new HashSet<>();
        }

        String controllerPath = normalizedViewPath.substring(0, viewIndex) + "/Controller/" + controllerDir + controllerFileName;
        Log.info("Controller path: " + controllerPath);

        VirtualFile controllerVirtualFile = LocalFileSystem.getInstance().findFileByPath(controllerPath);
        if (controllerVirtualFile == null) {
            Log.info("Controller file not found: " + controllerPath);
            return new HashSet<>();
        }

        // コントローラーファイルをPsiManagerを使用して取得
        PsiFile controllerFile = PsiManager.getInstance(project).findFile(controllerVirtualFile);
        if (controllerFile == null) {
            Log.info("Controller file not found: " + controllerPath);
            return new HashSet<>();
        }

        // コントローラーファイル内の指定されたアクション名のメソッド内のsetVarメソッド呼び出しを検索
        Method actionMethod = PsiTreeUtil.findChildrenOfType(controllerFile, Method.class).stream()
            .filter(method -> actionName.equals(method.getName()))
            .findFirst()
            .orElse(null);

        if (actionMethod == null) {
            Log.info("Action method not found: " + actionName);
            return new HashSet<>();
        }
        
        Log.info("Found action method: " + actionName + ", searching for setVar calls");

        Collection<MethodReference> variables = new HashSet<>();
        for (MethodReference methodRef : PsiTreeUtil.findChildrenOfType(actionMethod, MethodReference.class)) {
            // 参照元ファイルがcontrollerFileと一致する場合のみ追加
            if (methodRef.getContainingFile() == controllerFile) {
                variables.add(methodRef);
                Log.info("Added method reference: " + methodRef.getName());
            }
        }
        
        Log.info("Found " + variables.size() + " method references in " + actionName);
        return variables;
    }
    
    /**
     * 文字列をパスカルケースに変換
     */
    private static String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] parts = input.split("-");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }
    
    /**
     * 文字列をキャメルケースに変換
     */
    private static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] parts = input.split("-");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                if (i == 0) {
                    result.append(part.toLowerCase());
                } else {
                    result.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        result.append(part.substring(1).toLowerCase());
                    }
                }
            }
        }
        return result.toString();
    }
}

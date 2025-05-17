package com.sample.phpstormpluginviewvariable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;

import java.util.Collection;
import java.util.HashSet;

public class ControllerFile {
    public static Collection<MethodReference> getMethodReferences(VirtualFile viewVirtualFile, Project project) {
        Collection<MethodReference> variables = new HashSet<>();

        // ビューファイルのパスからコントローラーファイルのパスを推測
        String viewPath = viewVirtualFile.getPath();
        Log.info("View path: " + viewPath);

        // ビューファイル名からアクション名を取得
        String viewFileName = viewVirtualFile.getName();
        String actionName = viewFileName.replace(".php", "") + "Action";
        Log.info("Action name: " + actionName);

        int viewIndex = viewPath.indexOf("/View/");
        if (viewIndex == -1) {
            Log.info("Not a view file: " + viewPath);
            return variables;
        }
        String viewSubPath = viewPath.substring(viewIndex + "/View/".length());
        Log.info("View sub path: " + viewSubPath);

        String[] pathParts = viewSubPath.split("/");
        Log.info("Path parts: " + String.join(", ", pathParts));

        String controllerDir;
        String controllerFileName;
        if (pathParts.length == 2) {
            controllerDir = "";
            controllerFileName = pathParts[0] + "Controller.php";
        } else if (pathParts.length == 3) {
            controllerDir = pathParts[0].replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase() + "/";
            controllerFileName = pathParts[1] + "Controller.php";
        } else {
            return variables;
        }

        String controllerPath = viewPath.substring(0, viewIndex) + "/Controller/" + controllerDir + controllerFileName;
        Log.info("Controller path: " + controllerPath);

        VirtualFile controllerVirtualFile = LocalFileSystem.getInstance().findFileByPath(controllerPath);
        if (controllerVirtualFile == null) {
            Log.info("Controller file not found: " + controllerPath);
            return variables;
        }

        // コントローラーファイルをPsiManagerを使用して取得
        PsiFile controllerFile = PsiManager.getInstance(project).findFile(controllerVirtualFile);
        if (controllerFile == null) {
            Log.info("Controller file not found: " + controllerPath);
            return variables;
        }

        // コントローラーファイル内の指定されたアクション名のメソッド内のsetVarメソッド呼び出しを検索
        Method actionMethod = PsiTreeUtil.findChildrenOfType(controllerFile, Method.class).stream()
            .filter(method -> actionName.equals(method.getName()))
            .findFirst()
            .orElse(null);

        if (actionMethod == null) {
            return variables;
        }

        for (MethodReference methodRef : PsiTreeUtil.findChildrenOfType(actionMethod, MethodReference.class)) {
            // 参照元ファイルがcontrollerFileと一致する場合のみ追加
            if (methodRef.getContainingFile() == controllerFile) {
                variables.add(methodRef);
            }
        }
        return variables;
    }
}

package com.sample.phpstormpluginviewvariable.model;

import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.sample.phpstormpluginviewvariable.util.Log;

public class ViewFile {

    public static boolean isViewFile(PhpFile phpFile) {
        VirtualFile virtualFile = phpFile.getVirtualFile();
        if (virtualFile == null) {
            Log.info("Virtual file is null");
            return false;
        }
        String filePath = virtualFile.getPath();
        return isViewFile(filePath);
    }

    /**
     * 指定したファイルパスがViewのファイルか検証する
     */
    public static boolean isViewFile(String filePath) {
        // WindowsとUnixのパス区切り文字に対応
        String normalizedPath = filePath.replace("\\", "/");
        boolean isView = normalizedPath.contains("/views/") && (filePath.endsWith(".php") || filePath.endsWith(".phtml"));
        Log.info("File path: " + filePath + " (normalized: " + normalizedPath + "), is view: " + isView);
        return isView;
    }
}

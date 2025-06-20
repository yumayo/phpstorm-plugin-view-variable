package com.sample.phpstormpluginviewvariable.util;

import com.jetbrains.php.lang.psi.resolve.types.PhpType;

public class PhpTypeString {

    /**
     * PhpTypeから安全な文字列表現を取得
     */
    public static String getSafeTypeString(PhpType type) {
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
    private static boolean isValidString(String str) {
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
    private static String cleanTypeString(String typeStr) {
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

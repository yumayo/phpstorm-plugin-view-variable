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

        for (char c : str.toCharArray()) {
            if (c < 32 || c > 126) {
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
    public static String cleanTypeString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return typeStr;
        }

        if (typeStr.startsWith("\\")) {
            typeStr = typeStr.substring(1);
        }

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

    /**
     * PhpTypeから内部プレフィックス（#C, #V, #顶など）を除去してクリーンな型を作成
     */
    public static PhpType cleanPhpType(PhpType type) {
        if (type == null || type.isEmpty()) {
            return null;
        }

        PhpType.PhpTypeBuilder builder = PhpType.builder();
        boolean hasValidType = false;

        for (String typeName : type.getTypes()) {
            Log.info("Processing type for cleaning: " + typeName);

            // #C プレフィックス（クラス型）を除去
            if (typeName.startsWith("#C")) {
                String cleanType = typeName.substring(2);
                if (!cleanType.isEmpty()) {
                    builder.add(cleanType);
                    hasValidType = true;
                    Log.info("Cleaned class type: " + cleanType);
                }
            }
            // #V プレフィックス（変数型）をスキップ
            else if (typeName.startsWith("#V")) {
                Log.info("Skipping variable type: " + typeName);
            }
            // その他の特殊プレフィックスをスキップ
            else if (typeName.startsWith("#")) {
                Log.info("Skipping special type: " + typeName);
            }
            // 通常の型名
            else if (!typeName.equals("?") && !typeName.isEmpty()) {
                builder.add(cleanTypeString(typeName));
                hasValidType = true;
                Log.info("Added normal type: " + cleanTypeString(typeName));
            }
        }

        if (hasValidType) {
            PhpType result = builder.build();
            Log.info("Created cleaned type: " + getSafeTypeString(result));
            return result;
        }

        return null;
    }
}

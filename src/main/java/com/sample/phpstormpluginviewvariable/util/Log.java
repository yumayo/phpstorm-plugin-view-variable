package com.sample.phpstormpluginviewvariable.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;

/**
 * 呼び出し元の情報を自動的に取得するスタティックロガー
 * 毎回ロガーのインスタンスを作成する必要がなく、直接静的メソッドを呼び出して使用できる
 * 
 * デバッグモードの制御:
 * - システムプロパティ "phpstormpluginviewvariable.debug.enabled" が "true" の場合
 * - IntelliJ IDEがInternal Modeで実行されている場合
 * - IntelliJ IDEがEAP (Early Access Program) ビルドの場合
 * のいずれかの条件を満たした場合のみログを出力
 */
public class Log {
    
    private static final boolean DEBUG_ENABLED = isDebugEnabled();
    
    /**
     * デバッグモードが有効かどうかを判定
     */
    private static boolean isDebugEnabled() {
        // システムプロパティでの明示的な設定をチェック
        String debugProperty = System.getProperty("phpstormpluginviewvariable.debug.enabled");
        if (debugProperty != null && "true".equalsIgnoreCase(debugProperty)) {
            return true;
        }

        try {
            // IntelliJ Platform APIを使用してデバッグ環境を検出
            Application application = ApplicationManager.getApplication();
            if (application != null) {
                // Internal Modeまたは EAP ビルドの場合はデバッグモードを有効にする
                return application.isInternal() || application.isEAP();
            }
        } catch (Exception e) {
            // ApplicationManagerが利用できない場合（テスト環境等）は無視
            // この例外は通常発生しないが、安全のためキャッチしておく
        }

        return false;
    }

    /**
     * デバッグレベルのログを出力する
     *
     * @param message メッセージ
     */
    public static void debug(String message) {
        logWithLevel("DEBUG", message, null);
    }

    /**
     * 情報レベルのログを出力する
     *
     * @param message メッセージ
     */
    public static void info(String message) {
        logWithLevel("INFO", message, null);
    }

    /**
     * 警告レベルのログを出力する
     *
     * @param message メッセージ
     */
    public static void warn(String message) {
        logWithLevel("WARN", message, null);
    }

    /**
     * エラーレベルのログを出力する
     *
     * @param message メッセージ
     */
    public static void error(String message) {
        logWithLevel("ERROR", message, null);
    }

    /**
     * エラーレベルのログを例外とともに出力する
     *
     * @param message メッセージ
     * @param throwable 発生した例外
     */
    public static void error(String message, Throwable throwable) {
        logWithLevel("ERROR", message, throwable);
    }

    /**
     * 呼び出し元のクラス情報とメソッド名を含めたログを出力する
     *
     * @param level ログレベル
     * @param message メッセージ
     * @param throwable 例外（ない場合はnull）
     */
    private static void logWithLevel(String level, String message, Throwable throwable) {
        // デバッグモードが無効の場合は何もしない
        if (!DEBUG_ENABLED) {
            return;
        }
        // 呼び出し元のスタックトレース情報を取得
        StackTraceElement caller = getCaller();
        String className = extractSimpleClassName(caller.getClassName());
        String methodName = caller.getMethodName();
        int lineNumber = caller.getLineNumber();

        // フォーマット済みメッセージを作成
        String formattedMessage = String.format("[%s:%s:%d] %s",
                className, methodName, lineNumber, message);

        // IntelliJのロガーに出力
        Logger logger = Logger.getInstance(caller.getClassName());
        switch (level) {
            case "DEBUG":
                logger.debug(formattedMessage);
                break;
            case "INFO":
                logger.info(formattedMessage);
                break;
            case "WARN":
                logger.warn(formattedMessage);
                break;
            case "ERROR":
                if (throwable != null) {
                    logger.error(formattedMessage, throwable);
                } else {
                    logger.error(formattedMessage);
                }
                break;
        }

        var threadId = Thread.currentThread().threadId();

        // 標準出力にも出力（デバッグ用）
        String stdoutMessage = String.format("[%s] [%s] [%s:%s:%d] %s",
                level, threadId, className, methodName, lineNumber, message);
        System.out.println(stdoutMessage);

        // 例外がある場合はスタックトレースも出力
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    /**
     * 呼び出し元の情報を取得する
     *
     * @return 呼び出し元のスタックトレース要素
     */
    private static StackTraceElement getCaller() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // スタックトレースの順番:
        // 0: Thread.getStackTrace
        // 1: Log.getCaller
        // 2: Log.logWithLevel
        // 3: Log.debug/info/warn/error
        // 4: 実際の呼び出し元 ← これが欲しい
        return stackTrace.length > 4 ? stackTrace[4] : stackTrace[stackTrace.length - 1];
    }

    /**
     * 完全修飾クラス名から単純クラス名を抽出する
     *
     * @param fullClassName 完全修飾クラス名
     * @return 単純クラス名
     */
    private static String extractSimpleClassName(String fullClassName) {
        int lastDotIndex = fullClassName.lastIndexOf('.');
        return lastDotIndex >= 0 ? fullClassName.substring(lastDotIndex + 1) : fullClassName;
    }
}
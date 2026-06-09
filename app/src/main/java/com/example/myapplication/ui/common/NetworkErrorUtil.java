package com.namgyun.tamakitchen.ui.common;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.HttpException;

public class NetworkErrorUtil {

    private NetworkErrorUtil() {}

    public static String getUserMessage(Throwable t) {
        if (t == null) {
            return getDefaultMessage();
        }

        Throwable cause = t;

        while (cause != null) {
            if (cause instanceof UnknownHostException) {
                return getNetworkMessage();
            }

            if (cause instanceof SocketTimeoutException) {
                return "서버 응답이 지연되고 있어요. 잠시 후 다시 시도해주세요.";
            }

            if (cause instanceof ConnectException) {
                return "서버에 연결할 수 없어요. 잠시 후 다시 시도해주세요.";
            }

            if (cause instanceof HttpException) {
                return getHttpMessage((HttpException) cause);
            }

            if (cause instanceof IOException) {
                return getNetworkMessage();
            }

            String safeMessage = getMessageFromText(cause.getMessage());
            if (safeMessage != null) {
                return safeMessage;
            }

            cause = cause.getCause();
        }

        String safeMessage = getMessageFromText(t.getMessage());
        if (safeMessage != null) {
            return safeMessage;
        }

        return getDefaultMessage();
    }

    private static String getMessageFromText(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        String lower = message.toLowerCase();

        if (lower.contains("unknownhostexception")
                || lower.contains("unable to resolve host")
                || lower.contains("no address associated with hostname")
                || lower.contains("failed to resolve")
                || lower.contains("name not resolved")
                || lower.contains("dns")
                || lower.contains("network is unreachable")
                || lower.contains("no route to host")
                || lower.contains("software caused connection abort")) {
            return getNetworkMessage();
        }

        if (lower.contains("timeout")
                || lower.contains("timed out")
                || lower.contains("sockettimeoutexception")) {
            return "서버 응답이 지연되고 있어요. 잠시 후 다시 시도해주세요.";
        }

        if (lower.contains("failed to connect"
        )
                || lower.contains("connection refused")
                || lower.contains("connectexception")
                || lower.contains("connection reset")
                || lower.contains("connection aborted")) {
            return "서버에 연결할 수 없어요. 잠시 후 다시 시도해주세요.";
        }

        return null;
    }

    private static String getHttpMessage(HttpException e) {
        int code = e.code();

        if (code == 401) {
            return "로그인이 필요해요.";
        }

        if (code == 403) {
            return "접근 권한이 없어요.";
        }

        if (code == 404) {
            return "요청한 정보를 찾을 수 없어요.";
        }

        if (code >= 500) {
            return "서버에 문제가 발생했어요. 잠시 후 다시 시도해주세요.";
        }

        return getDefaultMessage();
    }

    public static String getNetworkMessage() {
        return "인터넷 연결을 확인해주세요.";
    }

    public static String getDefaultMessage() {
        return "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요.";
    }
}
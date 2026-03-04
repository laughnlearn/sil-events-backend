package com.college.events.util;

public final class MimeTypeUtil {

    private MimeTypeUtil() {
    }

    public static boolean isSupported(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("image/")
                || mimeType.equals("application/pdf")
                || mimeType.startsWith("video/");
    }
}

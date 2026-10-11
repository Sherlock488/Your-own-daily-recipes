package com.mealmenu;

import java.nio.file.Path;

public final class UserDataPaths {

    private UserDataPaths() {
    }

    public static Path applicationDirectory() {
        return Path.of(System.getProperty("user.home"), ".daily-menu");
    }

    public static Path accountDirectory(String accountId) {
        return applicationDirectory().resolve("users").resolve(accountId);
    }
}

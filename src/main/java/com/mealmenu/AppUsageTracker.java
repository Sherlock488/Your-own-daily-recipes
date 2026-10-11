package com.mealmenu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

public final class AppUsageTracker {

    private final Path storageFile;
    private final LocalDate firstUseDate;

    public AppUsageTracker(String accountId) {
        storageFile = UserDataPaths.accountDirectory(accountId).resolve("usage.properties");
        Properties properties = new Properties();
        LocalDate firstUse = null;

        if (Files.exists(storageFile)) {
            try (InputStream input = Files.newInputStream(storageFile)) {
                properties.load(input);
                String saved = properties.getProperty("first-use-date", "");
                if (!saved.isBlank()) {
                    firstUse = LocalDate.parse(saved);
                }
            } catch (Exception exception) {
                System.err.println("Could not read usage history: " + exception.getMessage());
            }
        }

        if (firstUse == null) {
            firstUse = LocalDate.now();
            properties.setProperty("first-use-date", firstUse.toString());
            try {
                Files.createDirectories(storageFile.getParent());
                try (OutputStream output = Files.newOutputStream(storageFile)) {
                    properties.store(output, "Daily Menu usage history");
                }
            } catch (IOException exception) {
                System.err.println("Could not save usage history: " + exception.getMessage());
            }
        }
        firstUseDate = firstUse;
    }

    public long daysUsing() {
        return Math.max(1, ChronoUnit.DAYS.between(firstUseDate, LocalDate.now()) + 1);
    }
}

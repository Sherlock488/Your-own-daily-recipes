package com.mealmenu;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Stores optional password autofill separately from the existing login session.
 * Windows DPAPI protects each saved password for the current Windows user.
 */
public final class RememberedPasswordStore {

    private final Protector protector;

    public RememberedPasswordStore() {
        this(new WindowsProtector());
    }

    RememberedPasswordStore(Protector protector) {
        this.protector = protector;
    }

    public boolean isAvailable() {
        return protector.isAvailable();
    }

    public boolean hasSavedPassword(String accountId) {
        return !accountId.isBlank() && Files.isRegularFile(fileFor(accountId));
    }

    public char[] load(String accountId) throws IOException {
        if (!hasSavedPassword(accountId)) {
            return new char[0];
        }
        byte[] plain = protector.unprotect(Files.readAllBytes(fileFor(accountId)));
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(plain));
            char[] password = new char[decoded.remaining()];
            decoded.get(password);
            if (decoded.hasArray()) {
                Arrays.fill(decoded.array(), '\0');
            }
            return password;
        } finally {
            Arrays.fill(plain, (byte) 0);
        }
    }

    public void remember(String accountId, char[] password) throws IOException {
        if (accountId.isBlank() || password.length == 0) {
            throw new IOException("An authenticated account and password are required.");
        }
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] plain = new byte[encoded.remaining()];
        encoded.get(plain);
        if (encoded.hasArray()) {
            Arrays.fill(encoded.array(), (byte) 0);
        }
        byte[] protectedPassword;
        try {
            protectedPassword = protector.protect(plain);
        } finally {
            Arrays.fill(plain, (byte) 0);
        }

        Path destination = fileFor(accountId);
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "password-", ".tmp");
        try {
            Files.write(temporary, protectedPassword);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public void forget(String accountId) throws IOException {
        if (!accountId.isBlank()) {
            Files.deleteIfExists(fileFor(accountId));
        }
    }

    private Path fileFor(String accountId) {
        return UserDataPaths.accountDirectory(accountId).resolve("remembered-password.dpapi");
    }

    interface Protector {
        boolean isAvailable();
        byte[] protect(byte[] plain) throws IOException;
        byte[] unprotect(byte[] encrypted) throws IOException;
    }

    private static final class WindowsProtector implements Protector {

        private final Path powershell = Path.of(
                System.getenv().getOrDefault("SystemRoot", "C:\\Windows"),
                "System32", "WindowsPowerShell", "v1.0", "powershell.exe");

        @Override
        public boolean isAvailable() {
            return System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                    .startsWith("windows") && Files.isRegularFile(powershell);
        }

        @Override
        public byte[] protect(byte[] plain) throws IOException {
            return transform(plain, "Protect");
        }

        @Override
        public byte[] unprotect(byte[] encrypted) throws IOException {
            return transform(encrypted, "Unprotect");
        }

        private byte[] transform(byte[] input, String operation) throws IOException {
            if (!isAvailable()) {
                throw new IOException("Windows password protection is unavailable.");
            }
            // Password bytes travel through stdin, never through command-line arguments.
            String script = """
                    $ErrorActionPreference = 'Stop'
                    try {
                        Add-Type -AssemblyName System.Security
                        $data = [Convert]::FromBase64String([Console]::In.ReadToEnd().Trim())
                        $entropy = [Text.Encoding]::UTF8.GetBytes('dailyrecipes.password.v1')
                        $result = [Security.Cryptography.ProtectedData]::%s(
                            $data, $entropy, [Security.Cryptography.DataProtectionScope]::CurrentUser)
                        [Console]::Out.Write([Convert]::ToBase64String($result))
                    } catch {
                        exit 1
                    }
                    """.formatted(operation);
            Process process = new ProcessBuilder(powershell.toString(),
                    "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", script)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.getInputStream().readAllBytes();
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
            byte[] transport = Base64.getEncoder().encode(input);
            try {
                try (var stdin = process.getOutputStream()) {
                    stdin.write(transport);
                }
                if (!process.waitFor(15, TimeUnit.SECONDS) || process.exitValue() != 0) {
                    throw new IOException("Windows password protection failed.");
                }
                byte[] response = output.get(2, TimeUnit.SECONDS);
                try {
                    return Base64.getMimeDecoder().decode(response);
                } finally {
                    Arrays.fill(response, (byte) 0);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Password protection was interrupted.", exception);
            } catch (java.util.concurrent.ExecutionException
                     | java.util.concurrent.TimeoutException | IllegalArgumentException exception) {
                throw new IOException("Windows password protection failed.", exception);
            } finally {
                Arrays.fill(transport, (byte) 0);
                process.destroyForcibly();
            }
        }
    }
}

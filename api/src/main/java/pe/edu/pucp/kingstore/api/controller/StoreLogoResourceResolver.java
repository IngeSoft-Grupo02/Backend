package pe.edu.pucp.kingstore.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Component
public class StoreLogoResourceResolver {

    private static final int MAX_LOGO_BYTES = 3 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final String localStorageBaseDir;
    private final String s3BucketName;
    private final String s3Region;
    private final HttpClient httpClient;

    public StoreLogoResourceResolver(
            @Value("${storage.local.base-dir:upload-local}") String localStorageBaseDir,
            @Value("${aws.s3.bucket-name:}") String s3BucketName,
            @Value("${aws.s3.region:us-east-1}") String s3Region) {
        this.localStorageBaseDir = localStorageBaseDir;
        this.s3BucketName = s3BucketName == null ? "" : s3BucketName.trim();
        this.s3Region = s3Region == null || s3Region.isBlank() ? "us-east-1" : s3Region.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public Optional<ResolvedLogo> resolve(String logoUrl) throws IOException {
        if (logoUrl == null || logoUrl.isBlank()) {
            return Optional.empty();
        }

        URI uri = parseUri(logoUrl.trim());
        if (uri == null) {
            return Optional.empty();
        }

        String path = normalizedPath(uri);
        if (path.startsWith("/uploads/")) {
            return resolveLocalUpload(path);
        }
        if (isAllowedS3Logo(uri, path)) {
            return resolveRemoteImage(uri);
        }
        return Optional.empty();
    }

    private URI parseUri(String value) {
        try {
            if (value.startsWith("/")) {
                return URI.create(value);
            }
            URI uri = URI.create(value);
            if (uri.getScheme() == null) {
                return null;
            }
            return uri;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizedPath(URI uri) {
        String path = uri.getPath();
        return path == null ? "" : path.replace('\\', '/');
    }

    private Optional<ResolvedLogo> resolveLocalUpload(String requestPath) throws IOException {
        String relative = requestPath.substring("/uploads/".length());
        if (relative.isBlank() || relative.contains("\0")) {
            return Optional.empty();
        }

        Path base = Paths.get(localStorageBaseDir).toAbsolutePath().normalize();
        Path target = base.resolve(relative).normalize();
        if (!target.startsWith(base) || !Files.isRegularFile(target)) {
            return Optional.empty();
        }

        long size = Files.size(target);
        if (size <= 0 || size > MAX_LOGO_BYTES) {
            return Optional.empty();
        }

        byte[] bytes = Files.readAllBytes(target);
        return mediaType(Files.probeContentType(target), requestPath)
                .map(type -> new ResolvedLogo(bytes, type));
    }

    private boolean isAllowedS3Logo(URI uri, String path) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || s3BucketName.isBlank()) {
            return false;
        }
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String regionalHost = s3BucketName + ".s3." + s3Region + ".amazonaws.com";
        String legacyHost = s3BucketName + ".s3.amazonaws.com";
        return (host.equalsIgnoreCase(regionalHost) || host.equalsIgnoreCase(legacyHost))
                && path.startsWith("/logos/")
                && !path.contains("..");
    }

    private Optional<ResolvedLogo> resolveRemoteImage(URI uri) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            byte[] bytes = response.body();
            if (bytes == null || bytes.length == 0 || bytes.length > MAX_LOGO_BYTES) {
                return Optional.empty();
            }
            String contentType = response.headers().firstValue("content-type").orElse(null);
            return mediaType(contentType, uri.getPath())
                    .map(type -> new ResolvedLogo(bytes, type));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<MediaType> mediaType(String contentType, String path) {
        String normalized = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        MediaType type = switch (normalized) {
            case "image/jpeg", "image/jpg" -> MediaType.IMAGE_JPEG;
            case "image/png" -> MediaType.IMAGE_PNG;
            case "image/webp" -> MediaType.parseMediaType("image/webp");
            case "image/gif" -> MediaType.IMAGE_GIF;
            default -> mediaTypeFromExtension(path);
        };
        return type == null ? Optional.empty() : Optional.of(type);
    }

    private MediaType mediaTypeFromExtension(String path) {
        String lower = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        return null;
    }

    public record ResolvedLogo(byte[] bytes, MediaType mediaType) {
    }
}

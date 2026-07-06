package pe.edu.pucp.kingstore.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StoreLogoResourceResolverTest {

    @TempDir
    Path uploadDir;

    @Test
    void resolvesLocalUploadUnderConfiguredDirectory() throws Exception {
        Path logos = Files.createDirectories(uploadDir.resolve("logos"));
        Files.write(logos.resolve("store.png"), new byte[] { 1, 2, 3 });
        StoreLogoResourceResolver resolver = new StoreLogoResourceResolver(
                uploadDir.toString(), "kingstore-assets", "us-east-1");

        var result = resolver.resolve("/uploads/logos/store.png");

        assertThat(result).isPresent();
        assertThat(result.get().mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(result.get().bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void blocksLocalPathTraversal() throws Exception {
        StoreLogoResourceResolver resolver = new StoreLogoResourceResolver(
                uploadDir.toString(), "kingstore-assets", "us-east-1");

        var result = resolver.resolve("/uploads/../secret.png");

        assertThat(result).isEmpty();
    }

    @Test
    void blocksArbitraryRemoteUrl() throws Exception {
        StoreLogoResourceResolver resolver = new StoreLogoResourceResolver(
                uploadDir.toString(), "kingstore-assets", "us-east-1");

        var result = resolver.resolve("https://example.com/logo.png");

        assertThat(result).isEmpty();
    }
}

package pe.edu.pucp.kingstore.service.quotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationDesign;
import pe.edu.pucp.kingstore.repository.quotation.QuotationDesignRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.storage.StorageService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotationDesignServiceTest {

    @Mock private QuotationDesignRepository quotationDesignRepository;
    @Mock private StorageService storageService;

    private QuotationDesignService service;

    @BeforeEach
    void setUp() {
        service = new QuotationDesignService(quotationDesignRepository, storageService);
    }

    @Test
    void uploadDesigns_shouldUploadToStorageAndPersistMetadata() {
        Quotation quotation = quotation();
        MockMultipartFile file = new MockMultipartFile(
                "designs", "diseño frontal.png", "image/png", "imagen".getBytes());
        when(storageService.uploadBytes(any(), any(byte[].class), eq("image/png")))
                .thenReturn("https://bucket.s3.amazonaws.com/design/street-kings/quotations/310/file.png");
        when(quotationDesignRepository.save(any(QuotationDesign.class)))
                .thenAnswer(invocation -> {
                    QuotationDesign design = invocation.getArgument(0);
                    design.setId(1);
                    return design;
                });

        List<QuotationDesign> designs = service.uploadDesigns(
                quotation, "street-kings", 10, List.of(file));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).uploadBytes(keyCaptor.capture(), any(byte[].class), eq("image/png"));
        assertThat(keyCaptor.getValue())
                .startsWith("design/street-kings/quotations/310/")
                .endsWith("-diseno-frontal.png");
        assertThat(designs).hasSize(1);
        assertThat(designs.get(0).getOriginalFileName()).isEqualTo("diseño frontal.png");
        assertThat(designs.get(0).getStorageKey()).isEqualTo(keyCaptor.getValue());
        assertThat(designs.get(0).getFileUrl()).contains("https://bucket.s3.amazonaws.com/");
        assertThat(designs.get(0).getContentType()).isEqualTo("image/png");
        assertThat(designs.get(0).getSizeBytes()).isEqualTo(6);
        assertThat(quotation.getDesigns()).hasSize(1);
    }

    @Test
    void uploadDesigns_withEmptyFile_shouldFail() {
        MockMultipartFile file = new MockMultipartFile("designs", "vacio.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadDesigns(quotation(), "street-kings", 10, List.of(file)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El archivo está vacío.");
        verify(storageService, never()).uploadBytes(any(), any(), any());
    }

    @Test
    void uploadDesigns_withInvalidContentType_shouldFail() {
        MockMultipartFile file = new MockMultipartFile("designs", "nota.txt", "text/plain", "hola".getBytes());

        assertThatThrownBy(() -> service.uploadDesigns(quotation(), "street-kings", 10, List.of(file)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Formato de archivo no permitido.");
        verify(storageService, never()).uploadBytes(any(), any(), any());
    }

    @Test
    void uploadDesigns_whenStorageFails_shouldReturnClearMessage() {
        MockMultipartFile file = new MockMultipartFile(
                "designs", "referencia.jpg", "image/jpeg", "imagen".getBytes());
        when(storageService.uploadBytes(any(), any(), eq("image/jpeg")))
                .thenThrow(new RuntimeException("S3 unavailable"));

        assertThatThrownBy(() -> service.uploadDesigns(quotation(), "street-kings", 10, List.of(file)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se pudo subir el diseño. Inténtalo nuevamente.");
    }

    private Quotation quotation() {
        Quotation quotation = new Quotation();
        quotation.setId(310);
        return quotation;
    }
}

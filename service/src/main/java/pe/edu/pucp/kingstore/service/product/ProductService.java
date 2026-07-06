package pe.edu.pucp.kingstore.service.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.product.DiscountPublicDTO;
import pe.edu.pucp.kingstore.domain.dto.product.ProductPublicDTO;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import java.util.List;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import java.util.Objects;
import pe.edu.pucp.kingstore.domain.dto.product.ProductRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.product.ProductResponseDTO;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.storage.StorageService;
import pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService extends AbstractCrudService<Product> {

    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;

    public ProductService(ProductRepository productRepository,
                          DiscountRepository discountRepository) {
        super(productRepository, "Product");
        this.productRepository = productRepository;
        this.discountRepository = discountRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findByStore(Integer storeId) {
        requireId(storeId);
        return productRepository.findByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveByStore(Integer storeId) {
        requireId(storeId);
        return productRepository.findByStoreIdAndActive(storeId, true);
    }

    @Transactional(readOnly = true)
    public List<Product> searchByNameInStore(String name, Integer storeId) {
        requireText(name, "Product name");
        requireId(storeId);
        return productRepository.findByNameContainingAndStoreId(name.trim(), storeId);
    }


    // ── Scope guard ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Product findInStore(Integer productId, Integer storeId) {
        requireId(productId);
        requireId(storeId);
        Product product = getById(productId);
        if (product.getStore() == null
                || !Objects.equals(product.getStore().getId(), storeId)
                || isDeleted(product)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return product;
    }

    @Transactional(readOnly = true)
    public Product findInStoreIncludingDeleted(Integer productId, Integer storeId) {
        requireId(productId);
        requireId(storeId);
        Product product = getById(productId);
        if (product.getStore() == null || !Objects.equals(product.getStore().getId(), storeId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return product;
    }

    // ── Request → entity ─────────────────────────────────────────────────────────
    @Transactional
    public Product createForStore(Store store, ProductRequestDTO request) {
        Product product = new Product();
        product.setStore(store);
        applyRequest(product, request);
        return create(product);
    }

    @Transactional
    public Product updateForStore(Product product, ProductRequestDTO request) {
        if (isDeleted(product)) {
            Product replacement = product.getReplacedByProduct();
            if (replacement != null && !isDeleted(replacement)) {
                return replacement;
            }
            throw new ResourceNotFoundException("Product", product.getId());
        }
        boolean referenced = hasReferences(product.getId());
        if (referenced && isInventoryOnlyChange(product, request)) {
            applyInventoryRequest(product, request);
            return productRepository.save(product);
        }
        if (referenced) {
            return replaceReferencedProduct(product, request);
        }
        applyRequest(product, request);
        return productRepository.save(product);
    }

    @Transactional
    public Product toggleActive(Product product, boolean active) {
        if (isDeleted(product)) {
            throw new ResourceNotFoundException("Product", product.getId());
        }
        product.setActive(active);
        product.setStatus(active ? ProductStatus.ACTIVE : ProductStatus.INACTIVE);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Product product = getById(id);
        markDeleted(product, null);
        productRepository.save(product);
    }

    // ── Image upload ──────────────────────────────────────────────────────────────
    @Transactional
    public String uploadImage(Store store, MultipartFile image,
                              StorageService storageService) throws IOException {
        String filename     = image.getOriginalFilename() == null ? "" : image.getOriginalFilename();
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "-");
        String key          = "products/" + store.getSlug() + "/" + UUID.randomUUID() + "-" + safeFilename;
        return storageService.uploadBytes(key, image.getBytes(),
                MerchantStringUtil.contentType(filename));
    }
    // Catalogo PUBLIC
    @Transactional(readOnly = true)
    public List<Product> findPublicByStore(Integer storeId){
        requireId(storeId);
        return productRepository.findByStoreIdAndActive(storeId, true).stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .toList();
    }
    @Transactional(readOnly = true)
    public Product findPublicInStore(Integer productId, Integer storeId) {
        requireId(productId);
        requireId(storeId);
        Product product = getById(productId);
        if (!Objects.equals(product.getStore().getId(), storeId)
                || !Boolean.TRUE.equals(product.getActive())
                || product.getStatus() != ProductStatus.ACTIVE
                || isDeleted(product)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return product;
    }

    @Transactional(readOnly = true)
    public ProductPublicDTO toPublicDTO(Product product){
        List<ProductPublicDTO.ProductVariantPublicDTO> variants =
                product.getVariants() == null ? List.of()
                        : product.getVariants().stream()
                          .filter(v -> v.getStock()>0)
                          .map(v -> new ProductPublicDTO.ProductVariantPublicDTO(
                                  v.getId(), v.getSize(), v.getColor(), v.getStock()))
                          .toList();
        List<DiscountPublicDTO> discounts =
                discountRepository.findByStoreId(product.getStore().getId()).stream()
                        .filter(d -> Boolean.TRUE.equals(d.getActive()))
                        .filter(d -> !Boolean.TRUE.equals(d.getDeleted()))
                        .filter(d -> appliesToProduct(d, product))
                        .map(d -> new DiscountPublicDTO(
                                d.getId(),
                                d.getName(),
                                d.getVolumeType(),
                                d.getMinQuantity(),
                                d.getMaxQuantity(),
                                d.getDiscountPercentage()))
                        .toList();
        return new ProductPublicDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getImageUrls() == null ? List.of() : product.getImageUrls(),
                customizableOrDefault(product),
                variants,
                discounts
        );
    }

    private boolean appliesToProduct(Discount discount, Product product){
        if(discount.getProduct () == null) return true;
        return Objects.equals(discount.getProduct().getId(), product.getId());
    }

    // ── Entity → DTO ──────────────────────────────────────────────────────────────
    public ProductResponseDTO toResponseDTO(Product product) {
        List<ProductResponseDTO.ProductVariantResponseDTO> variants = product.getVariants() == null
                ? List.of()
                : product.getVariants().stream()
                  .map(v -> new ProductResponseDTO.ProductVariantResponseDTO(
                          v.getId(), v.getSize(), v.getColor(), v.getStock()))
                  .collect(Collectors.toList());
        int stock = variants.stream().mapToInt(v -> v.getStock()).sum();
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getCostPrice(),
                product.getImageUrls() == null ? List.of() : product.getImageUrls(),
                customizableOrDefault(product),
                product.getActive(),
                resolveStatusLabel(product, stock),
                stock,
                variants,
                product.getStore() != null ? product.getStore().getId() : null
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────────
    private Product replaceReferencedProduct(Product product, ProductRequestDTO request) {
        Product replacement = new Product();
        replacement.setStore(product.getStore());
        applyRequest(replacement, request);
        Product savedReplacement = productRepository.save(replacement);
        copyProductDiscounts(product, savedReplacement);
        markDeleted(product, savedReplacement);
        productRepository.save(product);
        return savedReplacement;
    }

    private void markDeleted(Product product, Product replacement) {
        product.setDeleted(true);
        product.setDeletedAt(LocalDateTime.now());
        product.setReplacedByProduct(replacement);
        product.setActive(false);
        product.setStatus(ProductStatus.INACTIVE);
    }

    private boolean hasReferences(Integer productId) {
        if (productId == null) {
            return false;
        }
        return productRepository.countCartItemReferences(productId) > 0
                || productRepository.countQuotationItemReferences(productId) > 0
                || productRepository.countOrderItemReferences(productId) > 0
                || productRepository.countDiscountReferences(productId) > 0;
    }

    private boolean isInventoryOnlyChange(Product product, ProductRequestDTO request) {
        if (request == null) {
            throw new BusinessRuleException("Product request is required");
        }
        List<String> currentImages = buildImageUrls(product.getImageUrls());
        List<String> requestImages = buildImageUrls(request.getImageUrls());
        return Objects.equals(clean(product.getName()), clean(request.getName()))
                && Objects.equals(clean(product.getDescription()), clean(request.getDescription()))
                && sameNumber(product.getBasePrice(), request.getPrice() == null ? 0 : request.getPrice())
                && sameNumber(product.getCostPrice(), request.getCostPrice() == null ? 0 : request.getCostPrice())
                && Objects.equals(customizableOrDefault(product), request.getCustomizable() == null
                        ? customizableOrDefault(product) : request.getCustomizable())
                && Objects.equals(currentImages, requestImages);
    }

    private void applyInventoryRequest(Product product, ProductRequestDTO request) {
        applyVariantRequestInPlace(product, request.getVariants(), true);
        ProductStatus status = resolveStatus(request);
        product.setStatus(status);
        product.setActive(status == ProductStatus.ACTIVE);
    }

    private void applyVariantRequestInPlace(Product product,
                                            List<ProductRequestDTO.ProductVariantRequestDTO> variants,
                                            boolean keepZeroStock) {
        LinkedHashMap<VariantKey, Integer> requested = requestedVariantStock(variants, keepZeroStock);
        if (product.getVariants() == null) {
            product.setVariants(new ArrayList<>());
        }
        for (ProductVariant variant : product.getVariants()) {
            VariantKey key = VariantKey.from(variant);
            if (requested.containsKey(key)) {
                variant.setStock(requested.remove(key));
            } else {
                variant.setStock(0);
            }
            variant.setActive(true);
        }
        requested.forEach((key, stock) -> {
            ProductVariant variant = new ProductVariant();
            variant.setSize(key.size());
            variant.setColor(key.color());
            variant.setStock(stock);
            variant.setActive(true);
            product.getVariants().add(variant);
        });
    }

    private LinkedHashMap<VariantKey, Integer> requestedVariantStock(
            List<ProductRequestDTO.ProductVariantRequestDTO> requests,
            boolean keepZeroStock) {
        LinkedHashMap<VariantKey, Integer> variants = new LinkedHashMap<>();
        if (requests == null) {
            return variants;
        }
        for (ProductRequestDTO.ProductVariantRequestDTO request : requests) {
            if (request == null) {
                continue;
            }
            requireText(request.getSize(), "Variant size");
            if (request.getStock() == null || request.getStock() < 0) {
                throw new BusinessRuleException("Variant stock cannot be negative");
            }
            if (!keepZeroStock && request.getStock() == 0) {
                continue;
            }
            Color color = request.getColor() == null ? Color.BLACK : request.getColor();
            variants.put(new VariantKey(request.getSize().trim(), color), request.getStock());
        }
        return variants;
    }

    private void copyProductDiscounts(Product oldProduct, Product newProduct) {
        List<Discount> discounts = discountRepository.findByProductId(oldProduct.getId());
        for (Discount discount : discounts) {
            Discount copy = new Discount();
            copy.setStore(discount.getStore());
            copy.setProduct(newProduct);
            copy.setName(discount.getName());
            copy.setDiscountType(discount.getDiscountType());
            copy.setAppliesTo(discount.getAppliesTo());
            copy.setUsageCount(discount.getUsageCount());
            copy.setVolumeType(discount.getVolumeType());
            copy.setMinQuantity(discount.getMinQuantity());
            copy.setMaxQuantity(discount.getMaxQuantity());
            copy.setDiscountPercentage(discount.getDiscountPercentage());
            copy.setActive(discount.getActive());
            copy.setDeleted(false);
            discountRepository.save(copy);

            discount.setActive(false);
            discount.setDeleted(true);
            discount.setDeletedAt(LocalDateTime.now());
            discountRepository.save(discount);
        }
    }

    private void applyRequest(Product product, ProductRequestDTO request) {
        if (request == null) throw new BusinessRuleException("Product request is required");
        ProductStatus status  = resolveStatus(request);
        boolean       isDraft = status == ProductStatus.DRAFT;

        if (!isDraft) requireText(request.getName(), "Product name");
        if (!isDraft && (request.getPrice() == null || request.getPrice() <= 0)) {
            throw new BusinessRuleException("Product price must be positive");
        }
        product.setName(MerchantStringUtil.blankToNull(request.getName()) == null
                ? "Sin nombre" : request.getName().trim());
        product.setDescription(MerchantStringUtil.blankToNull(request.getDescription()));
        product.setBasePrice(request.getPrice()     == null ? 0 : request.getPrice());
        product.setCostPrice(request.getCostPrice() == null ? 0 : request.getCostPrice());
        Boolean customizable = request.getCustomizable();
        if (customizable == null) {
            customizable = product.getId() == null ? Boolean.FALSE : customizableOrDefault(product);
        }
        product.setCustomizable(customizable);
        replaceCollection(product.getImageUrls(),  buildImageUrls(request.getImageUrls()),   product::setImageUrls);
        replaceCollection(product.getAttributes(), List.of(),                                product::setAttributes);
        if (product.getId() == null) {
            replaceCollection(product.getVariants(), buildVariants(request.getVariants()), product::setVariants);
        } else {
            applyVariantRequestInPlace(product, request.getVariants(), false);
        }
        product.setStatus(status);
        product.setActive(status == ProductStatus.ACTIVE);
    }

    private ProductStatus resolveStatus(ProductRequestDTO request) {
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            return MerchantStringUtil.parseProductStatus(request.getStatus());
        }
        if (Boolean.FALSE.equals(request.getActive())) return ProductStatus.INACTIVE;
        int stock = request.getVariants() == null ? 0 : request.getVariants().stream()
                                                        .filter(Objects::nonNull)
                                                        .map(ProductRequestDTO.ProductVariantRequestDTO::getStock)
                                                        .filter(Objects::nonNull)
                                                        .mapToInt(Integer::intValue).sum();
        return stock == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE;
    }

    private List<String> buildImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return new ArrayList<>();
        List<String> clean = imageUrls.stream()
                .filter(Objects::nonNull).map(String::trim)
                .filter(url -> !url.isBlank()).distinct().limit(5).toList();
        for (String url : clean) {
            String lower = url.toLowerCase();
            if (lower.startsWith("data:") || lower.startsWith("blob:")) {
                throw new BusinessRuleException("Product images must be uploaded before saving");
            }
            if (url.length() > 255) {
                throw new BusinessRuleException("Product image URL exceeds 255 characters");
            }
        }
        return new ArrayList<>(clean);
    }

    private List<ProductVariant> buildVariants(
            List<ProductRequestDTO.ProductVariantRequestDTO> requests) {
        return requestedVariantStock(requests, false).entrySet().stream().map(entry -> {
            ProductVariant v = new ProductVariant();
            v.setSize(entry.getKey().size());
            v.setColor(entry.getKey().color());
            v.setStock(entry.getValue());
            v.setActive(true);
            return v;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private <T> void replaceCollection(List<T> current, List<T> next,
                                       java.util.function.Consumer<List<T>> setter) {
        if (current == null) { setter.accept(new ArrayList<>(next)); return; }
        current.clear();
        current.addAll(next);
    }

    private String clean(String value) {
        return MerchantStringUtil.blankToNull(value) == null ? null : value.trim();
    }

    private boolean sameNumber(double current, double next) {
        return Math.abs(current - next) < 0.0001;
    }

    private boolean isDeleted(Product product) {
        return Boolean.TRUE.equals(product.getDeleted());
    }

    private record VariantKey(String size, Color color) {
        private static VariantKey from(ProductVariant variant) {
            return new VariantKey(variant.getSize(), variant.getColor() == null ? Color.BLACK : variant.getColor());
        }
    }

    private String resolveStatusLabel(Product product, int stock) {
        ProductStatus status = product.getStatus();
        if (status == null) {
            status = !Boolean.TRUE.equals(product.getActive()) ? ProductStatus.INACTIVE
                    : stock == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE;
        }
        return switch (status) {
            case DRAFT        -> "Borrador";
            case OUT_OF_STOCK -> "Fuera de stock";
            case ACTIVE       -> "Activo";
            case INACTIVE     -> "Inactivo";
        };
    }

    private Boolean customizableOrDefault(Product product) {
        return product.getCustomizable() == null ? Boolean.TRUE : product.getCustomizable();
    }

    @Override
    protected void validateForSave(Product product) {
        if (product.getStore() == null || product.getStore().getId() == null) {
            throw new BusinessRuleException("Product must belong to a store");
        }
        requireText(product.getName(), "Product name");
        if (product.getCostPrice() < 0 || product.getBasePrice() < 0) {
            throw new BusinessRuleException("Product prices cannot be negative");
        }
        if (product.getBasePrice() < product.getCostPrice()) {
            throw new BusinessRuleException("Base price cannot be lower than cost price");
        }
        if (product.getStatus() == null) {
            product.setStatus(Boolean.FALSE.equals(product.getActive()) ? ProductStatus.INACTIVE : ProductStatus.ACTIVE);
        }
        product.setActive(product.getStatus() == ProductStatus.ACTIVE);
        if (product.getVariants() != null) {
            product.getVariants().forEach(variant -> {
                if (variant.getStock() < 0) {
                    throw new BusinessRuleException("Product variant stock cannot be negative");
                }
            });
        }
    }


}

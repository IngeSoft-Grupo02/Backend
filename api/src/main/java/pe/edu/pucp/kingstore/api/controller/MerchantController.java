package pe.edu.pucp.kingstore.api.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.Material;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.storage.StorageService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/merchant")
public class MerchantController {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024;
    private static final int MAX_PRODUCT_IMAGES = 5;
    private static final int MAX_IMAGE_URL_LENGTH = 255;
    private static final int MAX_DISCOUNTS_PER_STORE = 5;
    private static final double DEFAULT_BULK_PRODUCT_PRICE = 89.0;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final StoreRepository storeRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final MerchantRepository merchantRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;
    private final OrderRepository orderRepository;
    private final QuotationRepository quotationRepository;
    private final StorageService storageService;

    public MerchantController(StoreRepository storeRepository,
                              StoreCategoryRepository storeCategoryRepository,
                              MerchantRepository merchantRepository,
                              UserAccountRepository userAccountRepository,
                              ProductRepository productRepository,
                              DiscountRepository discountRepository,
                              OrderRepository orderRepository,
                              QuotationRepository quotationRepository,
                              StorageService storageService) {
        this.storeRepository = storeRepository;
        this.storeCategoryRepository = storeCategoryRepository;
        this.merchantRepository = merchantRepository;
        this.userAccountRepository = userAccountRepository;
        this.productRepository = productRepository;
        this.discountRepository = discountRepository;
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
        this.storageService = storageService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(Authentication authentication) {
        return handle(() -> ResponseEntity.ok(toMerchantProfileResponse(currentMerchant(authentication))));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication authentication,
                                           @RequestBody MerchantProfileRequest request) {
        return handle(() -> {
            Merchant merchant = currentMerchant(authentication);
            UserAccount account = merchant.getUserAccount();
            if (account == null) {
                throw new BusinessRuleException("Merchant account is not configured");
            }

            requireText(request.firstName(), "First name");
            requireText(request.paternalSurname(), "Paternal surname");
            String normalizedEmail = normalizeEmail(request.email());
            if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
                throw new BusinessRuleException("Email format is invalid");
            }
            userAccountRepository.findByEmail(normalizedEmail)
                    .filter(existing -> !Objects.equals(existing.getId(), account.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessRuleException("Email is already registered");
                    });

            account.setEmail(normalizedEmail);
            merchant.setFirstName(request.firstName().trim());
            merchant.setPaternalSurname(request.paternalSurname().trim());
            merchant.setMaternalSurname(blankToEmpty(request.maternalSurname()));
            merchant.setPhone(blankToNull(request.phone()));

            userAccountRepository.save(account);
            return ResponseEntity.ok(toMerchantProfileResponse(merchantRepository.save(merchant)));
        });
    }

    @PatchMapping("/profile/password")
    public ResponseEntity<?> updatePassword(Authentication authentication,
                                            @RequestBody MerchantPasswordRequest request) {
        return handle(() -> {
            Merchant merchant = currentMerchant(authentication);
            UserAccount account = merchant.getUserAccount();
            if (account == null) {
                throw new BusinessRuleException("Merchant account is not configured");
            }
            requireText(request.currentPassword(), "Current password");
            requireText(request.newPassword(), "New password");
            requireText(request.confirmPassword(), "Password confirmation");
            if (!Objects.equals(account.getPassword(), request.currentPassword())) {
                throw new BusinessRuleException("Current password is incorrect");
            }
            if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
                throw new BusinessRuleException("Passwords do not match");
            }
            validateNewPassword(request.newPassword());
            account.setPassword(request.newPassword());
            userAccountRepository.save(account);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        });
    }

    @GetMapping("/stores")
    public ResponseEntity<?> stores(Authentication authentication) {
        return handle(() -> ResponseEntity.ok(merchantStores(authentication).stream()
                .map(this::toStoreResponse)
                .toList()));
    }

    @GetMapping("/categories")
    public ResponseEntity<?> categories(@RequestParam(required = false) String search) {
        return handle(() -> {
            String term = search == null ? "" : search.trim().toLowerCase();
            return ResponseEntity.ok(storeCategoryRepository.findAll().stream()
                    .filter(category -> Boolean.TRUE.equals(category.getActive()))
                    .filter(category -> term.isBlank()
                            || category.getStoreCategoryName().toLowerCase().contains(term))
                    .map(category -> new StoreCategoryResponse(
                            category.getId(),
                            category.getStoreCategoryName()
                    ))
                    .toList());
        });
    }

    @PostMapping(value = "/stores/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadStoreLogo(Authentication authentication,
                                             @RequestPart("logo") MultipartFile logo) {
        return handle(() -> {
            Merchant merchant = currentMerchant(authentication);
            if (logo == null || logo.isEmpty()) {
                throw new BusinessRuleException("Store logo is required");
            }

            String filename = logo.getOriginalFilename() == null ? "" : logo.getOriginalFilename();
            String extension = extension(filename);
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                throw new BusinessRuleException("Invalid logo extension. Allowed values: jpg, jpeg, png, webp");
            }
            if (logo.getSize() > MAX_IMAGE_SIZE_BYTES) {
                throw new BusinessRuleException("Logo image exceeds 2 MB");
            }

            String key = "logos/merchants/" + merchant.getId() + "/" + UUID.randomUUID() + "." + extension;
            String logoUrl = storageService.uploadBytes(key, logo.getBytes(), contentType(filename));
            return ResponseEntity.ok(new StoreLogoResponse(logoUrl));
        });
    }

    @PostMapping("/stores")
    public ResponseEntity<?> createStore(Authentication authentication,
                                         @RequestBody MerchantStoreRequest request) {
        return handle(() -> {
            Merchant merchant = currentMerchant(authentication);
            Store store = new Store();
            store.setMerchant(merchant);
            store.setStoreStatus(StoreStatus.ACTIVE);
            applyStoreRequest(store, request, true);
            return ResponseEntity.status(201).body(toStoreResponse(storeRepository.save(store)));
        });
    }

    @PutMapping("/stores/{id}")
    public ResponseEntity<?> updateStore(Authentication authentication,
                                         @PathVariable Integer id,
                                         @RequestBody MerchantStoreRequest request) {
        return handle(() -> {
            Store store = storeInMerchantScope(authentication, id);
            applyStoreRequest(store, request, false);
            if (request.status() != null && !request.status().isBlank()) {
                store.setStoreStatus(parseStoreStatus(request.status()));
            }
            return ResponseEntity.ok(toStoreResponse(storeRepository.save(store)));
        });
    }

    @DeleteMapping("/stores/{id}")
    public ResponseEntity<?> deleteStore(Authentication authentication,
                                         @PathVariable Integer id) {
        return handle(() -> {
            Store store = storeInMerchantScope(authentication, id);
            store.setStoreStatus(StoreStatus.INACTIVE);
            storeRepository.save(store);
            return ResponseEntity.ok(Map.of("message", "Store deactivated successfully"));
        });
    }

    @GetMapping("/store")
    public ResponseEntity<?> currentStore(Authentication authentication,
                                          @RequestParam(required = false) Integer storeId) {
        return handle(() -> ResponseEntity.ok(toStoreResponse(currentMerchantStore(authentication, storeId))));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication,
                                       @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            List<Product> products = productRepository.findByStoreId(store.getId());
            List<Order> orders = orderRepository.findByStoreId(store.getId());
            List<Quotation> quotations = quotationRepository.findByStoreId(store.getId());

            long pendingOrders = orders.stream()
                    .filter(order -> switch (order.getStatus()) {
                        case PAYMENT_CONFIRMED, IN_PREPARATION, IN_TRANSIT -> true;
                        default -> false;
                    })
                    .count();
            long pendingQuotes = quotations.stream()
                    .filter(quotation -> quotation.getStatus() == QuotationStatus.PENDING)
                    .count();
            long drafts = products.stream().filter(product -> !Boolean.TRUE.equals(product.getActive())).count();
            List<RecentOrderResponse> recentOrders = orders.stream()
                    .sorted(Comparator.comparing(Order::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(5)
                    .map(order -> toRecentOrderResponse(order, store.getId()))
                    .toList();

            return ResponseEntity.ok(new DashboardResponse(pendingOrders, pendingQuotes, drafts, recentOrders));
        });
    }

    @GetMapping("/products")
    public ResponseEntity<?> products(Authentication authentication,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) Boolean active,
                                      @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            List<Product> products = productRepository.findByStoreId(store.getId());
            if (active != null) {
                products = products.stream().filter(product -> Objects.equals(product.getActive(), active)).toList();
            }
            if (search != null && !search.isBlank()) {
                String term = search.trim().toLowerCase();
                products = products.stream()
                        .filter(product -> product.getName() != null && product.getName().toLowerCase().contains(term))
                        .toList();
            }
            return ResponseEntity.ok(products.stream().map(this::toProductResponse).toList());
        });
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> product(Authentication authentication,
                                     @PathVariable Integer id,
                                     @RequestParam(required = false) Integer storeId) {
        return handle(() -> ResponseEntity.ok(toProductResponse(productInMerchantStore(authentication, id, storeId))));
    }

    @PostMapping(value = "/products/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProductImage(Authentication authentication,
                                                @RequestParam(required = false) Integer storeId,
                                                @RequestPart("image") MultipartFile image) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            if (image == null || image.isEmpty()) {
                throw new BusinessRuleException("Product image is required");
            }

            String filename = image.getOriginalFilename() == null ? "" : image.getOriginalFilename();
            String extension = extension(filename);
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                throw new BusinessRuleException("Invalid image extension. Allowed values: jpg, jpeg, png, webp");
            }
            if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
                throw new BusinessRuleException("Product image exceeds 2 MB");
            }

            String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "-");
            String key = "products/" + store.getSlug() + "/" + UUID.randomUUID() + "-" + safeFilename;
            String imageUrl = storageService.uploadBytes(key, image.getBytes(), contentType(filename));
            return ResponseEntity.ok(new ProductImageResponse(imageUrl));
        });
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(Authentication authentication,
                                           @RequestParam(required = false) Integer storeId,
                                           @RequestBody ProductRequest request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            Product product = new Product();
            product.setStore(store);
            applyProductRequest(product, request);
            product.setActive(request.active() == null || request.active());
            return ResponseEntity.status(201).body(toProductResponse(productRepository.save(product)));
        });
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(Authentication authentication,
                                           @PathVariable Integer id,
                                           @RequestParam(required = false) Integer storeId,
                                           @RequestBody ProductRequest request) {
        return handle(() -> {
            Product product = productInMerchantStore(authentication, id, storeId);
            applyProductRequest(product, request);
            if (request.active() != null) {
                product.setActive(request.active());
            }
            return ResponseEntity.ok(toProductResponse(productRepository.save(product)));
        });
    }

    @PatchMapping("/products/{id}/active")
    public ResponseEntity<?> updateProductActive(Authentication authentication,
                                                 @PathVariable Integer id,
                                                 @RequestParam(required = false) Integer storeId,
                                                 @RequestBody ActiveRequest request) {
        return handle(() -> {
            Product product = productInMerchantStore(authentication, id, storeId);
            if (request.active() == null) {
                throw new BusinessRuleException("Active flag is required");
            }
            product.setActive(request.active());
            return ResponseEntity.ok(toProductResponse(productRepository.save(product)));
        });
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(Authentication authentication,
                                           @PathVariable Integer id,
                                           @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Product product = productInMerchantStore(authentication, id, storeId);
            product.setActive(false);
            productRepository.save(product);
            return ResponseEntity.ok(Map.of("message", "Product deactivated successfully"));
        });
    }

    @PostMapping(value = "/products/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkProducts(Authentication authentication,
                                          @RequestParam(required = false) Integer storeId,
                                          @RequestPart("products") MultipartFile productsCsv,
                                          @RequestPart(value = "images", required = false) MultipartFile imagesZip) {
        return handle(() -> {
            if (productsCsv == null || productsCsv.isEmpty()) {
                throw new BusinessRuleException("Products CSV is required");
            }
            Store store = currentMerchantStore(authentication, storeId);
            BulkProductResult result = processProductBulk(store, productsCsv, imagesZip);
            return ResponseEntity.ok(result);
        });
    }

    @GetMapping("/discounts")
    public ResponseEntity<?> discounts(Authentication authentication,
                                       @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            return ResponseEntity.ok(discountRepository.findByStoreId(store.getId()).stream()
                    .map(this::toDiscountResponse)
                    .toList());
        });
    }

    @PostMapping("/discounts")
    public ResponseEntity<?> createDiscount(Authentication authentication,
                                            @RequestParam(required = false) Integer storeId,
                                            @RequestBody DiscountRequest request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            enforceDiscountLimit(store.getId(), null);
            Product product = request.productId() == null
                    ? null
                    : productInMerchantStore(authentication, request.productId(), store.getId());
            Discount discount = new Discount();
            discount.setStore(store);
            discount.setProduct(product);
            applyDiscountRequest(discount, request);
            discount.setActive(resolveDiscountActive(request));
            return ResponseEntity.status(201).body(toDiscountResponse(discountRepository.save(discount)));
        });
    }

    @PutMapping("/discounts/{id}")
    public ResponseEntity<?> updateDiscount(Authentication authentication,
                                            @PathVariable Integer id,
                                            @RequestParam(required = false) Integer storeId,
                                            @RequestBody DiscountRequest request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            Discount discount = discountInMerchantStore(authentication, id, store.getId());
            discount.setStore(store);
            discount.setProduct(null);
            if (request.productId() != null) {
                discount.setProduct(productInMerchantStore(authentication, request.productId(), store.getId()));
            }
            applyDiscountRequest(discount, request);
            discount.setActive(resolveDiscountActive(request));
            return ResponseEntity.ok(toDiscountResponse(discountRepository.save(discount)));
        });
    }

    @DeleteMapping("/discounts/{id}")
    public ResponseEntity<?> deleteDiscount(Authentication authentication,
                                            @PathVariable Integer id,
                                            @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Discount discount = discountInMerchantStore(authentication, id, storeId);
            discount.setActive(false);
            discountRepository.save(discount);
            return ResponseEntity.ok(Map.of("message", "Discount deactivated successfully"));
        });
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(Authentication authentication,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            List<Order> orders = status == null || status.isBlank()
                    ? orderRepository.findByStoreId(store.getId())
                    : orderRepository.findByStoreIdAndStatus(store.getId(), parseOrderStatus(status));
            return ResponseEntity.ok(orders.stream().map(order -> toOrderResponse(order, store.getId())).toList());
        });
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(Authentication authentication,
                                               @PathVariable Integer id,
                                               @RequestParam(required = false) Integer storeId,
                                               @RequestBody OrderStatusRequest request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            Order order = orderInMerchantStore(authentication, id, storeId);
            if (request.status() == null) {
                throw new BusinessRuleException("Order status is required");
            }
            order.setStatus(request.status());
            return ResponseEntity.ok(toOrderResponse(orderRepository.save(order), store.getId()));
        });
    }

    @GetMapping("/quotations")
    public ResponseEntity<?> quotations(Authentication authentication,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            List<Quotation> quotations = status == null || status.isBlank()
                    ? quotationRepository.findByStoreId(store.getId())
                    : quotationRepository.findByStoreIdAndStatus(store.getId(), parseQuotationStatus(status));
            return ResponseEntity.ok(quotations.stream().map(quotation -> toQuotationResponse(quotation, store.getId())).toList());
        });
    }

    @PatchMapping("/quotations/{id}/respond")
    public ResponseEntity<?> respondQuotation(Authentication authentication,
                                              @PathVariable Integer id,
                                              @RequestParam(required = false) Integer storeId,
                                              @RequestBody QuotationResponseRequest request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            Quotation quotation = quotationInMerchantStore(authentication, id, storeId);
            if (request.status() == null || request.status() == QuotationStatus.PENDING) {
                throw new BusinessRuleException("Quotation response must approve or reject the quotation");
            }
            quotation.setStatus(request.status());
            quotation.setObservations(request.observations());
            quotation.setResponseAt(LocalDateTime.now());
            return ResponseEntity.ok(toQuotationResponse(quotationRepository.save(quotation), store.getId()));
        });
    }

    private ResponseEntity<?> handle(EndpointCall call) {
        try {
            return call.execute();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private Merchant currentMerchant(Authentication authentication) {
        return merchantRepository.findByUserAccountId(currentUserAccountId(authentication))
                .orElseThrow(() -> new BusinessRuleException("Authenticated merchant profile was not found"));
    }

    private List<Store> merchantStores(Authentication authentication) {
        return storeRepository.findAllByMerchant_UserAccount_Id(currentUserAccountId(authentication)).stream()
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .sorted(Comparator.comparing(Store::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Store currentMerchantStore(Authentication authentication, Integer storeId) {
        List<Store> stores = merchantStores(authentication);
        if (stores.isEmpty()) {
            throw new BusinessRuleException("Merchant has no stores assigned");
        }
        if (storeId != null) {
            return stores.stream()
                    .filter(store -> Objects.equals(store.getId(), storeId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        }

        List<Store> activeStores = stores.stream()
                .filter(store -> store.getStoreStatus() == StoreStatus.ACTIVE)
                .toList();
        if (activeStores.size() == 1) {
            return activeStores.get(0);
        }
        if (stores.size() == 1) {
            return stores.get(0);
        }
        throw new BusinessRuleException("Store context is required");
    }

    private Integer currentUserAccountId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessRuleException("Authenticated merchant is required");
        }
        try {
            return Integer.parseInt(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Authenticated merchant id is invalid");
        }
    }

    private Store storeInMerchantScope(Authentication authentication, Integer storeId) {
        Integer userAccountId = currentUserAccountId(authentication);
        return storeRepository.findByIdAndMerchant_UserAccount_Id(storeId, userAccountId)
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
    }

    private Product productInMerchantStore(Authentication authentication, Integer productId, Integer storeId) {
        Store store = currentMerchantStore(authentication, storeId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (product.getStore() == null || !Objects.equals(product.getStore().getId(), store.getId())) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return product;
    }

    private Discount discountInMerchantStore(Authentication authentication, Integer discountId, Integer storeId) {
        Store store = currentMerchantStore(authentication, storeId);
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", discountId));
        Integer discountStoreId = discount.getStore() != null
                ? discount.getStore().getId()
                : discount.getProduct() != null && discount.getProduct().getStore() != null
                ? discount.getProduct().getStore().getId()
                : null;
        if (!Objects.equals(discountStoreId, store.getId())) {
            throw new ResourceNotFoundException("Discount", discountId);
        }
        return discount;
    }

    private Order orderInMerchantStore(Authentication authentication, Integer orderId, Integer storeId) {
        Store store = currentMerchantStore(authentication, storeId);
        return orderRepository.findByStoreId(store.getId()).stream()
                .filter(order -> Objects.equals(order.getId(), orderId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private Quotation quotationInMerchantStore(Authentication authentication, Integer quotationId, Integer storeId) {
        Store store = currentMerchantStore(authentication, storeId);
        return quotationRepository.findByStoreId(store.getId()).stream()
                .filter(quotation -> Objects.equals(quotation.getId(), quotationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", quotationId));
    }

    private void applyStoreRequest(Store store, MerchantStoreRequest request, boolean creating) {
        if (request == null) {
            throw new BusinessRuleException("Store request is required");
        }
        requireText(request.name(), "Store name");
        String slug = slugify(request.slug() == null || request.slug().isBlank() ? request.name() : request.slug());
        storeRepository.findBySlug(slug)
                .filter(existing -> !Objects.equals(existing.getId(), store.getId()))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Store slug is already registered");
                });

        store.setStoreName(request.name().trim());
        store.setSlug(slug);
        store.setDescription(blankToNull(request.description()));
        store.setLogoUrl(blankToNull(request.logoUrl()));
        store.setPrimaryColor(request.primaryColor() != null ? request.primaryColor() : defaultPrimaryColor(store.getPrimaryColor()));
        store.setSecondaryColor(request.secondaryColor() != null ? request.secondaryColor() : defaultSecondaryColor(store.getSecondaryColor()));
        store.setTertiaryColor(request.tertiaryColor() != null ? request.tertiaryColor() : defaultTertiaryColor(store.getTertiaryColor()));

        if (request.categoryId() != null) {
            store.setCategory(resolveCategory(request.categoryId()));
        } else if (creating || store.getCategory() == null) {
            store.setCategory(defaultCategory());
        }
    }

    private StoreCategory resolveCategory(Integer categoryId) {
        return storeCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessRuleException("Store category not found"));
    }

    private StoreCategory defaultCategory() {
        return storeCategoryRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("Store category is required"));
    }

    private void applyProductRequest(Product product, ProductRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Product request is required");
        }
        requireText(request.name(), "Product name");
        if (request.price() == null || request.price() <= 0) {
            throw new BusinessRuleException("Product price must be positive");
        }

        product.setName(request.name().trim());
        product.setDescription(blankToNull(request.description()));
        product.setBasePrice(request.price());
        product.setCostPrice(request.costPrice() == null ? 0 : request.costPrice());
        product.setMaterial(request.material() == null ? Material.COTTON : request.material());
        product.setImageUrls(productImageUrls(request.imageUrls()));
        product.setAttributes(new ArrayList<>());
        product.setVariants(toVariants(request.variants()));
    }

    private List<String> productImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> cleanUrls = imageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .limit(MAX_PRODUCT_IMAGES)
                .toList();

        for (String url : cleanUrls) {
            String normalized = url.toLowerCase();
            if (normalized.startsWith("data:") || normalized.startsWith("blob:")) {
                throw new BusinessRuleException("Product images must be uploaded before saving");
            }
            if (url.length() > MAX_IMAGE_URL_LENGTH) {
                throw new BusinessRuleException("Product image URL exceeds " + MAX_IMAGE_URL_LENGTH + " characters");
            }
        }
        return new ArrayList<>(cleanUrls);
    }

    private List<ProductVariant> toVariants(List<ProductVariantRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }
        return requests.stream().map(request -> {
            requireText(request.size(), "Variant size");
            if (request.stock() == null || request.stock() < 0) {
                throw new BusinessRuleException("Variant stock cannot be negative");
            }
            ProductVariant variant = new ProductVariant();
            variant.setSize(request.size().trim());
            variant.setColor(request.color() == null ? Color.BLACK : request.color());
            variant.setStock(request.stock());
            variant.setActive(true);
            return variant;
        }).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void applyDiscountRequest(Discount discount, DiscountRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Discount request is required");
        }
        Integer minQuantity = request.minQuantity();
        Integer maxQuantity = request.maxQuantity() == null ? minQuantity : request.maxQuantity();
        Double value = request.discountPercentage();
        if (minQuantity == null || minQuantity <= 0) {
            throw new BusinessRuleException("Minimum quantity must be positive");
        }
        if (maxQuantity == null || maxQuantity < minQuantity) {
            throw new BusinessRuleException("Maximum quantity must be greater than or equal to minimum quantity");
        }
        if (value == null || value < 0) {
            throw new BusinessRuleException("Discount value must be zero or greater");
        }
        if (isPercentageDiscount(request) && value > 100) {
            throw new BusinessRuleException("Discount percentage must be between 0 and 100");
        }
        String appliesTo = blankToNull(request.appliesTo()) == null ? "Todo el catalogo" : request.appliesTo().trim();
        String normalizedAppliesTo = normalizeText(appliesTo);
        if (normalizedAppliesTo.contains("categoria")) {
            throw new BusinessRuleException("Category discounts are not supported");
        }
        if (normalizedAppliesTo.contains("producto")) {
            if (discount.getProduct() == null) {
                throw new BusinessRuleException("Product is required for product discounts");
            }
            appliesTo = "Producto especifico";
        } else {
            appliesTo = "Todo el catalogo";
        }
        discount.setName(blankToNull(request.name()));
        discount.setDiscountType(blankToNull(request.type()) == null ? "Porcentaje" : request.type().trim());
        discount.setAppliesTo(appliesTo);
        discount.setUsageCount(request.usageCount() == null ? discount.getUsageCount() : request.usageCount());
        discount.setVolumeType(request.volumeType() == null ? VolumeType.UNIT : request.volumeType());
        discount.setMinQuantity(minQuantity);
        discount.setMaxQuantity(maxQuantity);
        discount.setDiscountPercentage(value);
    }

    private void enforceDiscountLimit(Integer storeId, Integer currentDiscountId) {
        long count = discountRepository.findByStoreId(storeId).stream()
                .filter(discount -> currentDiscountId == null || !Objects.equals(discount.getId(), currentDiscountId))
                .count();
        if (count >= MAX_DISCOUNTS_PER_STORE) {
            throw new BusinessRuleException("A store can only have up to " + MAX_DISCOUNTS_PER_STORE + " discounts");
        }
    }

    private String normalizeText(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private BulkProductResult processProductBulk(Store store, MultipartFile csv, MultipartFile zip) throws IOException {
        Map<String, byte[]> images = zip == null || zip.isEmpty() ? Map.of() : readImages(zip);
        List<BulkProductRow> rows = readProductRows(csv);
        List<String> errors = validateProductRows(rows, images);
        if (!errors.isEmpty()) {
            return new BulkProductResult(0, rows.size(), 0, errors);
        }

        Map<String, List<BulkProductRow>> byProduct = new LinkedHashMap<>();
        for (BulkProductRow row : rows) {
            byProduct.computeIfAbsent(row.name(), key -> new ArrayList<>()).add(row);
        }

        int imagesUploaded = 0;
        for (Map.Entry<String, List<BulkProductRow>> entry : byProduct.entrySet()) {
            List<BulkProductRow> productRows = entry.getValue();
            BulkProductRow first = productRows.get(0);
            List<String> imageUrls = new ArrayList<>();
            for (String imageName : first.images()) {
                byte[] bytes = images.get(imageName.toLowerCase());
                if (bytes != null) {
                    String key = "products/" + store.getSlug() + "/" + UUID.randomUUID() + "-" + imageName;
                    imageUrls.add(storageService.uploadBytes(key, bytes, contentType(imageName)));
                    imagesUploaded++;
                }
            }

            Product product = new Product();
            product.setStore(store);
            product.setName(first.name());
            product.setDescription(first.description());
            product.setBasePrice(first.price() == null ? DEFAULT_BULK_PRODUCT_PRICE : first.price());
            product.setCostPrice(0);
            product.setMaterial(Material.COTTON);
            product.setImageUrls(imageUrls);
            product.setAttributes(new ArrayList<>());
            product.setActive(true);
            product.setVariants(productRows.stream().map(row -> {
                ProductVariant variant = new ProductVariant();
                variant.setSize(row.size());
                variant.setColor(row.color());
                variant.setStock(row.stock());
                variant.setActive(true);
                return variant;
            }).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
            productRepository.save(product);
        }
        return new BulkProductResult(byProduct.size(), rows.size(), imagesUploaded, List.of());
    }

    private Map<String, byte[]> readImages(MultipartFile zip) throws IOException {
        Map<String, byte[]> images = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(zip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String fileName = entry.getName().contains("/")
                        ? entry.getName().substring(entry.getName().lastIndexOf('/') + 1)
                        : entry.getName();
                String extension = extension(fileName);
                if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                    throw new BusinessRuleException("Invalid image extension: " + fileName);
                }
                byte[] bytes = zis.readAllBytes();
                if (bytes.length > MAX_IMAGE_SIZE_BYTES) {
                    throw new BusinessRuleException("Image exceeds 2 MB: " + fileName);
                }
                images.put(fileName.toLowerCase(), bytes);
            }
        }
        return images;
    }

    private List<BulkProductRow> readProductRows(MultipartFile csv) throws IOException {
        List<BulkProductRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return rows;
            }
            String[] headers = splitCsv(header);
            Map<String, Integer> index = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                index.put(headers[i].trim().toUpperCase(), i);
            }
            String line;
            int rowNum = 2;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    rowNum++;
                    continue;
                }
                String[] cols = splitCsv(line);
                rows.add(new BulkProductRow(
                        rowNum,
                        get(cols, index, "NOMBRE"),
                        get(cols, index, "DESCRIPCION"),
                        parseDouble(get(cols, index, "PRECIO")),
                        get(cols, index, "TALLA"),
                        parseColor(get(cols, index, "COLOR")),
                        parseInt(get(cols, index, "STOCK")),
                        imageNames(get(cols, index, "IMAGENES"))
                ));
                rowNum++;
            }
        }
        return rows;
    }

    private List<String> validateProductRows(List<BulkProductRow> rows, Map<String, byte[]> images) {
        List<String> errors = new ArrayList<>();
        if (rows.isEmpty()) {
            errors.add("CSV has no product rows");
        }
        for (BulkProductRow row : rows) {
            if (row.name() == null || row.name().isBlank()) {
                errors.add("Row " + row.row() + ": NOMBRE is required");
            }
            if (row.description() == null || row.description().isBlank()) {
                errors.add("Row " + row.row() + ": DESCRIPCION is required");
            }
            if (row.price() != null && row.price() <= 0) {
                errors.add("Row " + row.row() + ": PRECIO must be a positive number");
            }
            if (row.size() == null || row.size().isBlank()) {
                errors.add("Row " + row.row() + ": TALLA is required");
            }
            if (row.color() == null) {
                errors.add("Row " + row.row() + ": COLOR is invalid");
            }
            if (row.stock() == null || row.stock() < 0) {
                errors.add("Row " + row.row() + ": STOCK must be zero or greater");
            }
            for (String image : row.images()) {
                if (!images.containsKey(image.toLowerCase())) {
                    errors.add("Row " + row.row() + ": image not found in ZIP: " + image);
                }
            }
        }
        return errors;
    }

    private StoreResponse toStoreResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getStoreName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl(),
                storeStatusLabel(store.getStoreStatus()),
                store.getPrimaryColor() != null ? store.getPrimaryColor().name() : null,
                store.getSecondaryColor() != null ? store.getSecondaryColor().name() : null,
                store.getTertiaryColor() != null ? store.getTertiaryColor().name() : null,
                store.getCategory() != null ? store.getCategory().getId() : null,
                store.getCategory() != null ? store.getCategory().getStoreCategoryName() : null,
                pendingQuotesCount(store.getId())
        );
    }

    private MerchantProfileResponse toMerchantProfileResponse(Merchant merchant) {
        UserAccount account = merchant.getUserAccount();
        return new MerchantProfileResponse(
                merchant.getId(),
                account != null ? account.getEmail() : null,
                fullName(merchant.getFirstName(), merchant.getPaternalSurname(), merchant.getMaternalSurname()),
                merchant.getFirstName(),
                merchant.getPaternalSurname(),
                merchant.getMaternalSurname(),
                merchant.getPhone(),
                merchant.getRuc()
        );
    }

    private RecentOrderResponse toRecentOrderResponse(Order order, Integer storeId) {
        return new RecentOrderResponse(
                order.getId(),
                customerName(orderCustomer(order)),
                orderStatusLabel(order.getStatus()),
                order.getFinalTotal(),
                order.getCreatedAt(),
                storeId
        );
    }

    private ProductResponse toProductResponse(Product product) {
        List<ProductVariantResponse> variants = product.getVariants() == null ? List.of() : product.getVariants().stream()
                .map(variant -> new ProductVariantResponse(variant.getId(), variant.getSize(), variant.getColor(), variant.getStock()))
                .toList();
        int stock = variants.stream().mapToInt(ProductVariantResponse::stock).sum();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getCostPrice(),
                product.getMaterial(),
                product.getImageUrls() == null ? List.of() : product.getImageUrls(),
                product.getActive(),
                productStatusLabel(product, stock),
                stock,
                variants,
                product.getStore() != null ? product.getStore().getId() : null
        );
    }

    private DiscountResponse toDiscountResponse(Discount discount) {
        Product product = discount.getProduct();
        Integer responseStoreId = discount.getStore() != null
                ? discount.getStore().getId()
                : product != null && product.getStore() != null ? product.getStore().getId() : null;
        return new DiscountResponse(
                discount.getId(),
                product != null ? product.getId() : null,
                discount.getVolumeType(),
                discount.getMinQuantity(),
                discount.getMaxQuantity(),
                discount.getDiscountPercentage(),
                discount.getActive(),
                Boolean.TRUE.equals(discount.getActive()) ? "Activa" : "Pausada",
                responseStoreId,
                product != null ? product.getName() : null,
                discount.getName(),
                discount.getDiscountType(),
                discount.getDiscountPercentage(),
                discount.getMinQuantity(),
                discount.getUsageCount(),
                discount.getAppliesTo()
        );
    }

    private OrderResponse toOrderResponse(Order order, Integer storeId) {
        Customer customer = orderCustomer(order);
        return new OrderResponse(
                order.getId(),
                customerName(customer),
                order.getStatus(),
                orderStatusLabel(order.getStatus()),
                order.getItems() == null ? 0 : order.getItems().size(),
                order.getFinalTotal(),
                order.getCreatedAt(),
                storeId
        );
    }

    private QuotationResponse toQuotationResponse(Quotation quotation, Integer storeId) {
        Customer customer = quotation.getShoppingCart() != null ? quotation.getShoppingCart().getCustomer() : null;
        return new QuotationResponse(
                quotation.getId(),
                customerName(customer),
                quotation.getStatus(),
                quotationStatusLabel(quotation.getStatus()),
                quotation.getSubTotal(),
                quotation.getDiscount(),
                quotation.getTotalAmount(),
                quotation.getRequestedAt(),
                quotation.getDescription(),
                quotation.getObservations(),
                storeId,
                quotationItems(quotation)
        );
    }

    private List<QuotationItemResponse> quotationItems(Quotation quotation) {
        if (quotation.getItems() == null) {
            return List.of();
        }
        return quotation.getItems().stream()
                .map(this::toQuotationItemResponse)
                .toList();
    }

    private QuotationItemResponse toQuotationItemResponse(QuotationItem item) {
        String productName = "Producto";
        String variantName = item.getProductVariant() != null
                ? item.getProductVariant().getSize() + " / " + item.getProductVariant().getColor().name()
                : null;
        return new QuotationItemResponse(productName, variantName, item.getQuantity(), item.getPrice(), item.getSubTotal());
    }

    private Customer orderCustomer(Order order) {
        return order.getQuotation() != null && order.getQuotation().getShoppingCart() != null
                ? order.getQuotation().getShoppingCart().getCustomer()
                : null;
    }

    private String customerName(Customer customer) {
        if (customer == null) {
            return "Cliente";
        }
        return fullName(customer.getFirstName(), customer.getPaternalSurname(), customer.getMaternalSurname());
    }

    private String fullName(String firstName, String paternalSurname, String maternalSurname) {
        return String.join(" ", List.of(safe(firstName), safe(paternalSurname), safe(maternalSurname)))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private long pendingQuotesCount(Integer storeId) {
        return quotationRepository.findByStoreIdAndStatus(storeId, QuotationStatus.PENDING).size();
    }

    private String productStatusLabel(Product product, int stock) {
        if (!Boolean.TRUE.equals(product.getActive())) {
            return "Inactivo";
        }
        if (stock == 0) {
            return "Sin stock";
        }
        return "Activo";
    }

    private String orderStatusLabel(OrderStatus status) {
        return switch (status) {
            case PAYMENT_CONFIRMED -> "Pagado";
            case IN_PREPARATION -> "En proceso";
            case IN_TRANSIT -> "Enviado";
            case DELIVERED -> "Entregado";
            case CANCELLED -> "Cancelado";
        };
    }

    private String quotationStatusLabel(QuotationStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case APPROVED -> "Aprobada";
            case REJECTED -> "Rechazada";
        };
    }

    private String storeStatusLabel(StoreStatus status) {
        return switch (status) {
            case ACTIVE -> "Activa";
            case INACTIVE -> "Inactiva";
            case SUSPENDED -> "Suspendida";
        };
    }

    private OrderStatus parseOrderStatus(String value) {
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "PAYMENT_CONFIRMED", "PAGADO", "APROBADO" -> OrderStatus.PAYMENT_CONFIRMED;
            case "IN_PREPARATION", "EN_PROCESO" -> OrderStatus.IN_PREPARATION;
            case "IN_TRANSIT", "ENVIADO" -> OrderStatus.IN_TRANSIT;
            case "DELIVERED", "ENTREGADO" -> OrderStatus.DELIVERED;
            case "CANCELLED", "CANCELADO" -> OrderStatus.CANCELLED;
            default -> throw new BusinessRuleException("Invalid order status: " + value);
        };
    }

    private QuotationStatus parseQuotationStatus(String value) {
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "PENDING", "PENDIENTE" -> QuotationStatus.PENDING;
            case "APPROVED", "APROBADA" -> QuotationStatus.APPROVED;
            case "REJECTED", "RECHAZADA" -> QuotationStatus.REJECTED;
            default -> throw new BusinessRuleException("Invalid quotation status: " + value);
        };
    }

    private StoreStatus parseStoreStatus(String value) {
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "ACTIVE", "ACTIVA" -> StoreStatus.ACTIVE;
            case "INACTIVE", "INACTIVA" -> StoreStatus.INACTIVE;
            case "SUSPENDED", "SUSPENDIDA" -> StoreStatus.SUSPENDED;
            default -> throw new BusinessRuleException("Invalid store status: " + value);
        };
    }

    private Boolean resolveDiscountActive(DiscountRequest request) {
        if (request.active() != null) {
            return request.active();
        }
        if (request.status() == null || request.status().isBlank()) {
            return true;
        }
        String normalized = request.status().trim().toLowerCase();
        return normalized.equals("activa") || normalized.equals("activo") || normalized.equals("active");
    }

    private boolean isPercentageDiscount(DiscountRequest request) {
        return request.type() == null || request.type().isBlank()
                || request.type().equalsIgnoreCase("Porcentaje")
                || request.type().equalsIgnoreCase("PERCENTAGE");
    }

    private void validateNewPassword(String password) {
        if (password.length() < 8) {
            throw new BusinessRuleException("New password must have at least 8 characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new BusinessRuleException("New password must contain at least one uppercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new BusinessRuleException("New password must contain at least one number");
        }
    }

    private String normalizeEmail(String email) {
        requireText(email, "Email");
        return email.trim().toLowerCase();
    }

    private PrimaryColor defaultPrimaryColor(PrimaryColor currentValue) {
        return currentValue != null ? currentValue : PrimaryColor.ONYX_BLACK;
    }

    private SecondaryColor defaultSecondaryColor(SecondaryColor currentValue) {
        return currentValue != null ? currentValue : SecondaryColor.OLIVE_DRAB;
    }

    private TertiaryColor defaultTertiaryColor(TertiaryColor currentValue) {
        return currentValue != null ? currentValue : TertiaryColor.RICH_CAMEL;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(fieldName + " is required");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Integer parseInt(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "NEGRO", "BLACK" -> Color.BLACK;
            case "BLANCO", "WHITE" -> Color.WHITE;
            case "ROJO", "RED" -> Color.RED;
            case "AZUL", "BLUE" -> Color.BLUE;
            case "VERDE", "GREEN" -> Color.GREEN;
            default -> null;
        };
    }

    private List<String> imageNames(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(";"))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .limit(5)
                .toList();
    }

    private String[] splitCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    private String get(String[] cols, Map<String, Integer> index, String key) {
        Integer i = index.get(key);
        return i == null || i >= cols.length ? null : cols[i].trim();
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }

    private String contentType(String filename) {
        String ext = extension(filename);
        return "image/" + (ext.equals("jpg") ? "jpeg" : ext);
    }

    private String slugify(String text) {
        return text.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    @FunctionalInterface
    private interface EndpointCall {
        ResponseEntity<?> execute() throws IOException;
    }

    public record MerchantProfileRequest(String email, String firstName, String paternalSurname,
                                         String maternalSurname, String phone) {}
    public record MerchantPasswordRequest(String currentPassword, String newPassword,
                                          String confirmPassword) {}
    public record MerchantProfileResponse(Integer id, String email, String name, String firstName,
                                          String paternalSurname, String maternalSurname,
                                          String phone, String ruc) {}
    public record MerchantStoreRequest(String name, String slug, String description,
                                       PrimaryColor primaryColor, SecondaryColor secondaryColor,
                                       TertiaryColor tertiaryColor, Integer categoryId,
                                       String logoUrl, String status) {}
    public record StoreCategoryResponse(Integer id, String name) {}
    public record StoreLogoResponse(String logoUrl) {}
    public record ProductImageResponse(String imageUrl) {}
    public record StoreResponse(Integer id, String name, String slug, String description, String logoUrl,
                                String status, String primaryColor, String secondaryColor,
                                String tertiaryColor, Integer categoryId, String categoryName, long pendingQuotes) {}
    public record DashboardResponse(long pendingOrders, long pendingQuotes, long drafts,
                                    List<RecentOrderResponse> recentOrders) {}
    public record RecentOrderResponse(Integer id, String customer, String status, Double total,
                                      LocalDateTime createdAt, Integer storeId) {}
    public record ProductVariantRequest(String size, Color color, Integer stock) {}
    public record ProductRequest(String name, String description, Double price, Double costPrice,
                                 Material material, List<String> imageUrls,
                                 List<ProductVariantRequest> variants, Boolean active) {}
    public record ProductVariantResponse(Integer id, String size, Color color, int stock) {}
    public record ProductResponse(Integer id, String name, String description, double price,
                                  double costPrice, Material material, List<String> imageUrls,
                                  Boolean active, String status, int stock,
                                  List<ProductVariantResponse> variants, Integer storeId) {}
    public record ActiveRequest(Boolean active) {}
    public record DiscountRequest(Integer productId, VolumeType volumeType,
                                  @JsonAlias("minUnits") Integer minQuantity,
                                  Integer maxQuantity,
                                  @JsonAlias("value") Double discountPercentage,
                                  Boolean active, String status, String name,
                                  String type, String appliesTo, Integer usageCount) {}
    public record DiscountResponse(Integer id, Integer productId, VolumeType volumeType,
                                   int minQuantity, int maxQuantity, double discountPercentage,
                                   Boolean active, String status, Integer storeId,
                                   String productName, String name, String type,
                                   double value, int minUnits, int usageCount,
                                   String appliesTo) {}
    public record OrderStatusRequest(OrderStatus status) {}
    public record OrderResponse(Integer id, String customer, OrderStatus status, String statusLabel,
                                int items, Double total, LocalDateTime createdAt, Integer storeId) {}
    public record QuotationResponseRequest(QuotationStatus status, String observations) {}
    public record QuotationItemResponse(String product, String variant, int quantity,
                                        double price, double subTotal) {}
    public record QuotationResponse(Integer id, String customer, QuotationStatus status,
                                    String statusLabel, double subTotal, double discount,
                                    double totalAmount, LocalDateTime requestedAt,
                                    String description, String observations, Integer storeId,
                                    List<QuotationItemResponse> items) {}
    public record BulkProductResult(int productsCreated, int variantsProcessed, int imagesUploaded,
                                    List<String> errors) {}
    private record BulkProductRow(int row, String name, String description, Double price,
                                  String size, Color color, Integer stock, List<String> images) {}
}

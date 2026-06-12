//package pe.edu.pucp.kingstore.api;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.security.core.Authentication;
//import pe.edu.pucp.kingstore.api.controller.MerchantController;
//import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
//import pe.edu.pucp.kingstore.domain.model.order.Order;
//import pe.edu.pucp.kingstore.domain.model.order.OrderItem;
//import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
//import pe.edu.pucp.kingstore.domain.model.product.Discount;
//import pe.edu.pucp.kingstore.domain.model.product.Product;
//import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
//import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
//import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
//import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
//import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
//import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
//import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
//import pe.edu.pucp.kingstore.domain.model.store.Store;
//import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
//import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
//import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
//import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
//import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
//import pe.edu.pucp.kingstore.domain.model.user.Customer;
//import pe.edu.pucp.kingstore.domain.model.user.Merchant;
//import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
//import pe.edu.pucp.kingstore.repository.order.OrderRepository;
//import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
//import pe.edu.pucp.kingstore.repository.product.ProductRepository;
//import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
//import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
//import pe.edu.pucp.kingstore.repository.store.StoreRepository;
//import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
//import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
//import pe.edu.pucp.kingstore.service.storage.StorageService;
//
//import java.io.ByteArrayOutputStream;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipOutputStream;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//class MerchantControllerCoverageTest {
//
//    private final StoreRepository storeRepository = mock(StoreRepository.class);
//    private final StoreCategoryRepository storeCategoryRepository = mock(StoreCategoryRepository.class);
//    private final MerchantRepository merchantRepository = mock(MerchantRepository.class);
//    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
//    private final ProductRepository productRepository = mock(ProductRepository.class);
//    private final DiscountRepository discountRepository = mock(DiscountRepository.class);
//    private final OrderRepository orderRepository = mock(OrderRepository.class);
//    private final QuotationRepository quotationRepository = mock(QuotationRepository.class);
//    private final StorageService storageService = mock(StorageService.class);
//    private final MerchantController controller = new MerchantController(
//            storeRepository,
//            storeCategoryRepository,
//            merchantRepository,
//            userAccountRepository,
//            productRepository,
//            discountRepository,
//            orderRepository,
//            quotationRepository,
//            storageService
//    );
//
//    @Test
//    void exposesMerchantProfileStoresAndDashboard() {
//        Authentication auth = auth("7");
//        Merchant merchant = merchant();
//        StoreCategory category = category();
//        Store primaryStore = store(10, merchant, category, "Merchant Store");
//        Store secondStore = store(11, merchant, category, "Annex Store");
//        Product draftProduct = product(20, primaryStore, "Polo Negro", false);
//        Order order = order(40, productVariant(21));
//        Quotation quotation = quotation(50, productVariant(22));
//
//        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.of(merchant));
//        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(primaryStore, secondStore));
//        when(quotationRepository.findByStoreIdAndStatus(10, QuotationStatus.PENDING)).thenReturn(List.of(quotation));
//        when(quotationRepository.findByStoreIdAndStatus(11, QuotationStatus.PENDING)).thenReturn(List.of());
//        when(productRepository.findByStoreId(10)).thenReturn(List.of(draftProduct));
//        when(orderRepository.findByStoreId(10)).thenReturn(List.of(order));
//        when(quotationRepository.findByStoreId(10)).thenReturn(List.of(quotation));
//        when(userAccountRepository.findByEmail("merchant-updated@test.com")).thenReturn(Optional.empty());
//        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        assertThat(controller.profile(auth).getStatusCode()).isEqualTo(HttpStatus.OK);
//        ResponseEntity<?> storesResponse = controller.stores(auth);
//        assertThat(storesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat((List<?>) storesResponse.getBody()).hasSize(2);
//
//        ResponseEntity<?> dashboard = controller.dashboard(auth, 10);
//        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
//        MerchantController.DashboardResponse body = (MerchantController.DashboardResponse) dashboard.getBody();
//        assertThat(body.pendingOrders()).isEqualTo(1);
//        assertThat(body.pendingQuotes()).isEqualTo(1);
//        assertThat(body.drafts()).isEqualTo(1);
//
//        ResponseEntity<?> updated = controller.updateProfile(auth, new MerchantController.MerchantProfileRequest(
//                "merchant-updated@test.com", "Ana", "Perez", "Lopez", "999111222"
//        ));
//        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(merchant.getFirstName()).isEqualTo("Ana");
//        assertThat(merchant.getPhone()).isEqualTo("999111222");
//
//        assertThat(controller.updatePassword(auth, new MerchantController.MerchantPasswordRequest(
//                "Secret123", "NewSecret1", "NewSecret1"
//        )).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(controller.updatePassword(auth, new MerchantController.MerchantPasswordRequest(
//                "bad", "NewSecret1", "NewSecret1"
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    @Test
//    void managesStoresAndProductLifecycleWithinMerchantScope() {
//        Authentication auth = auth("7");
//        Merchant merchant = merchant();
//        StoreCategory category = category();
//        Store store = store(10, merchant, category, "Merchant Store");
//        Product product = product(20, store, "Polo Negro", true);
//
//        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.of(merchant));
//        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(store));
//        when(storeRepository.findByIdAndMerchant_UserAccount_Id(10, 7)).thenReturn(Optional.of(store));
//        when(storeRepository.findBySlug("nueva-tienda")).thenReturn(Optional.empty());
//        when(storeCategoryRepository.findById(1)).thenReturn(Optional.of(category));
//        when(quotationRepository.findByStoreIdAndStatus(10, QuotationStatus.PENDING)).thenReturn(List.of());
//        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> {
//            Store saved = invocation.getArgument(0);
//            if (saved.getId() == null) {
//                saved.setId(99);
//            }
//            return saved;
//        });
//        when(productRepository.findByStoreId(10)).thenReturn(List.of(product));
//        when(productRepository.findById(20)).thenReturn(Optional.of(product));
//        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        ResponseEntity<?> createdStore = controller.createStore(auth, new MerchantController.MerchantStoreRequest(
//                "Nueva Tienda", null, "Nueva descripcion",
//                PrimaryColor.ONYX_BLACK, SecondaryColor.SLATE, TertiaryColor.RAW_GOLD,
//                1, "logo.png", "ACTIVA"
//        ));
//        assertThat(createdStore.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//
//        ResponseEntity<?> updatedStore = controller.updateStore(auth, 10, new MerchantController.MerchantStoreRequest(
//                "Merchant Store Updated", "merchant-store", "Desc",
//                PrimaryColor.CHARCOAL, SecondaryColor.SAGE, TertiaryColor.RAW_GOLD,
//                1, "logo-2.png", "SUSPENDIDA"
//        ));
//        assertThat(updatedStore.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(store.getStoreName()).isEqualTo("Merchant Store Updated");
//        assertThat(store.getStoreStatus()).isEqualTo(StoreStatus.SUSPENDED);
//
//        assertThat(controller.currentStore(auth, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(controller.products(auth, "negro", true, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        ResponseEntity<?> createdProduct = controller.createProduct(auth, 10, new MerchantController.ProductRequest(
//                "Camisa", "Camisa manga larga", 45.0, 20.0, List.of("img.png"),
//                List.of(new MerchantController.ProductVariantRequest("M", Color.BLUE, 8)), true
//        ));
//        assertThat(createdProduct.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//
//        assertThat(controller.createProduct(auth, 10, new MerchantController.ProductRequest(
//                "", "bad", 0.0, null, null, null, null, null
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//
//        List<ProductVariant> originalVariants = product.getVariants();
//        List<pe.edu.pucp.kingstore.domain.model.product.Attribute> originalAttributes = product.getAttributes();
//        ResponseEntity<?> updatedProduct = controller.updateProduct(auth, 20, 10, new MerchantController.ProductRequest(
//                "Polo Editado", "Producto actualizado", 55.0, 25.0, List.of("img-2.png"),
//                List.of(new MerchantController.ProductVariantRequest("L", Color.RED, 4)), true
//        ));
//        assertThat(updatedProduct.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(product.getVariants()).isSameAs(originalVariants);
//        assertThat(product.getAttributes()).isSameAs(originalAttributes);
//        assertThat(product.getVariants()).singleElement().satisfies(variant -> {
//            assertThat(variant.getSize()).isEqualTo("L");
//            assertThat(variant.getColor()).isEqualTo(Color.RED);
//            assertThat(variant.getStock()).isEqualTo(4);
//        });
//        assertThat(controller.updateProduct(auth, 20, 10, new MerchantController.ProductRequest(
//                "Polo Editado", "Producto actualizado", 55.0, 25.0, List.of("img-2.png", "img-2.png"),
//                List.of(new MerchantController.ProductVariantRequest("L", Color.RED, 4)), true
//        )).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(product.getImageUrls()).containsExactly("img-2.png");
//
//        assertThat(controller.updateProductActive(auth, 20, 10, new MerchantController.ActiveRequest(false)).getStatusCode())
//                .isEqualTo(HttpStatus.OK);
//        assertThat(controller.deleteProduct(auth, 20, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(productRepository).delete(product);
//        assertThat(controller.product(auth("invalid"), 20, 10).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    @Test
//    void managesDiscountsOrdersAndQuotationsInsideMerchantStore() {
//        Authentication auth = auth("7");
//        Merchant merchant = merchant();
//        StoreCategory category = category();
//        Store store = store(10, merchant, category, "Merchant Store");
//        Product product = product(20, store, "Polo", true);
//        Discount discount = discount(30, product);
//        Order order = order(40, product.getVariants().get(0));
//        Quotation quotation = quotation(50, product.getVariants().get(0));
//
//        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.of(merchant));
//        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(store));
//        when(productRepository.findById(20)).thenReturn(Optional.of(product));
//        when(discountRepository.findById(30)).thenReturn(Optional.of(discount));
//        when(discountRepository.findByProductStoreId(10)).thenReturn(List.of(discount));
//        when(discountRepository.save(any(Discount.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(orderRepository.findByStoreId(10)).thenReturn(List.of(order));
//        when(orderRepository.findByStoreIdAndStatus(10, OrderStatus.IN_PREPARATION)).thenReturn(List.of(order));
//        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(quotationRepository.findByStoreId(10)).thenReturn(List.of(quotation));
//        when(quotationRepository.findByStoreIdAndStatus(10, QuotationStatus.PENDING)).thenReturn(List.of(quotation));
//        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        assertThat(controller.discounts(auth, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(controller.createDiscount(auth, 10, new MerchantController.DiscountRequest(
//                20, VolumeType.UNIT, 2, 5, 10.0, true,
//                null, "Descuento por producto", "Porcentaje", "Producto especifico", 0
//        )).getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        ResponseEntity<?> catalogRule = controller.createDiscount(auth, 10, new MerchantController.DiscountRequest(
//                null, null, 20, null, 15.0, null,
//                "Activa", "Oferta de catalogo", "Porcentaje", "Todo el catalogo", 0
//        ));
//        assertThat(catalogRule.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        MerchantController.DiscountResponse catalogRuleBody =
//                (MerchantController.DiscountResponse) catalogRule.getBody();
//        assertThat(catalogRuleBody.productId()).isNull();
//        assertThat(catalogRuleBody.name()).isEqualTo("Oferta de catalogo");
//        assertThat(catalogRuleBody.minUnits()).isEqualTo(20);
//
//        assertThat(controller.updateDiscount(auth, 30, 10, new MerchantController.DiscountRequest(
//                null, VolumeType.DOZEN, 1, 2, 5.0, false,
//                null, "Actualizado", "Porcentaje", "Todo el catalogo", 3
//        )).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(controller.deleteDiscount(auth, 30, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        assertThat(controller.orders(auth, null, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(controller.orders(auth, "En proceso", 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(controller.updateOrderStatus(auth, 40, 10, new MerchantController.OrderStatusRequest(OrderStatus.IN_TRANSIT))
//                .getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);
//
//        assertThat(controller.quotations(auth, null, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(controller.quotations(auth, "Pendiente", 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(controller.respondQuotation(auth, 50, 10, new MerchantController.QuotationResponseRequest(
//                QuotationStatus.APPROVED, "Aprobada"
//        )).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.APPROVED);
//        assertThat(controller.respondQuotation(auth, 50, 10, new MerchantController.QuotationResponseRequest(
//                QuotationStatus.PENDING, null
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    @Test
//    void processesMerchantBulkProductUploadAndReportsCsvErrors() throws Exception {
//        Authentication auth = auth("7");
//        Merchant merchant = merchant();
//        StoreCategory category = category();
//        Store store = store(10, merchant, category, "Merchant Store");
//
//        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.of(merchant));
//        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(store));
//        List<Product> savedProducts = new ArrayList<>();
//        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
//            Product saved = invocation.getArgument(0);
//            savedProducts.add(saved);
//            return saved;
//        });
//        when(storageService.uploadBytes(anyString(), any(byte[].class), anyString())).thenReturn("https://cdn/img.png");
//
//        MockMultipartFile csv = new MockMultipartFile("products", "products.csv", "text/csv",
//                """
//                NOMBRE,DESCRIPCION,TALLA,COLOR,STOCK,IMAGENES
//                Polo,Polo basico,M,NEGRO,12,polo.png
//                Polo,Polo basico,L,BLACK,5,polo.png
//                """.getBytes(StandardCharsets.UTF_8));
//        MockMultipartFile zip = new MockMultipartFile("images", "images.zip", "application/zip", zip("polo.png"));
//
//        ResponseEntity<?> uploaded = controller.bulkProducts(auth, 10, csv, zip);
//        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.OK);
//        MerchantController.BulkProductResult result = (MerchantController.BulkProductResult) uploaded.getBody();
//        assertThat(result.productsCreated()).isEqualTo(1);
//        assertThat(result.variantsProcessed()).isEqualTo(2);
//        assertThat(result.imagesUploaded()).isEqualTo(1);
//        assertThat(savedProducts).singleElement().extracting(Product::getBasePrice).isEqualTo(89.0);
//
//        MockMultipartFile invalid = new MockMultipartFile("products", "products.csv", "text/csv",
//                """
//                NOMBRE,DESCRIPCION,PRECIO,TALLA,COLOR,STOCK,IMAGENES
//                ,Sin nombre,0,M,MORADO,-1,missing.png
//                """.getBytes(StandardCharsets.UTF_8));
//        MerchantController.BulkProductResult invalidResult =
//                (MerchantController.BulkProductResult) controller.bulkProducts(auth, 10, invalid, null).getBody();
//        assertThat(invalidResult.errors()).hasSizeGreaterThanOrEqualTo(4);
//    }
//
//    @Test
//    void coversUploadsCategoriesStoreDeletionProductStatusesAndDiscountErrors() throws Exception {
//        Authentication auth = auth("7");
//        Merchant merchant = merchant();
//        StoreCategory category = category();
//        category.setActive(true);
//        StoreCategory inactiveCategory = category();
//        inactiveCategory.setId(2);
//        inactiveCategory.setStoreCategoryName("Zapatos");
//        inactiveCategory.setActive(false);
//        Store store = store(10, merchant, category, "Merchant Store");
//        Product product = product(20, store, "Polo", true);
//
//        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.of(merchant));
//        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(store));
//        when(storeRepository.findByIdAndMerchant_UserAccount_Id(10, 7)).thenReturn(Optional.of(store));
//        when(storeCategoryRepository.findAll()).thenReturn(List.of(category, inactiveCategory));
//        when(storageService.uploadBytes(anyString(), any(byte[].class), anyString()))
//                .thenReturn("https://cdn.test/upload.png");
//        when(productRepository.findById(20)).thenReturn(Optional.of(product));
//        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        ResponseEntity<?> categories = controller.categories("mod");
//        assertThat(categories.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat((List<?>) categories.getBody()).hasSize(1);
//
//        MockMultipartFile logo = new MockMultipartFile("logo", "logo.jpg", "image/jpeg",
//                "logo".getBytes(StandardCharsets.UTF_8));
//        MerchantController.StoreLogoResponse logoResponse =
//                (MerchantController.StoreLogoResponse) controller.uploadStoreLogo(auth, logo).getBody();
//        assertThat(logoResponse.logoUrl()).isEqualTo("https://cdn.test/upload.png");
//        MockMultipartFile badLogo = new MockMultipartFile("logo", "logo.gif", "image/gif",
//                "logo".getBytes(StandardCharsets.UTF_8));
//        assertThat(controller.uploadStoreLogo(auth, badLogo).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        MockMultipartFile hugeLogo = new MockMultipartFile("logo", "logo.png", "image/png",
//                new byte[(2 * 1024 * 1024) + 1]);
//        assertThat(controller.uploadStoreLogo(auth, hugeLogo).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//
//        MockMultipartFile productImage = new MockMultipartFile("image", "foto producto.webp", "image/webp",
//                "image".getBytes(StandardCharsets.UTF_8));
//        MerchantController.ProductImageResponse imageResponse =
//                (MerchantController.ProductImageResponse) controller.uploadProductImage(auth, 10, productImage).getBody();
//        assertThat(imageResponse.imageUrl()).isEqualTo("https://cdn.test/upload.png");
//        assertThat(controller.uploadProductImage(auth, 10, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//
//        assertThat(controller.deleteStore(auth, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(store.getStoreStatus()).isEqualTo(StoreStatus.INACTIVE);
//
//        ResponseEntity<?> draft = controller.createProduct(auth, 10, new MerchantController.ProductRequest(
//                null, null, null, null, null, null, null, "borrador"
//        ));
//        assertThat(draft.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        MerchantController.ProductResponse draftBody = (MerchantController.ProductResponse) draft.getBody();
//        assertThat(draftBody.status()).isEqualTo("Borrador");
//        assertThat(draftBody.active()).isFalse();
//        assertThat(draftBody.name()).isEqualTo("Sin nombre");
//
//        ResponseEntity<?> outOfStock = controller.createProduct(auth, 10, new MerchantController.ProductRequest(
//                "Polo sin stock", "Desc", 40.0, 10.0, null,
//                List.of(new MerchantController.ProductVariantRequest("S", Color.GREEN, 0)), null, null
//        ));
//        assertThat(((MerchantController.ProductResponse) outOfStock.getBody()).status()).isEqualTo("Fuera de stock");
//        assertThat(((MerchantController.ProductResponse) outOfStock.getBody()).active()).isFalse();
//
//        ResponseEntity<?> inactive = controller.createProduct(auth, 10, new MerchantController.ProductRequest(
//                "Polo inactivo", "Desc", 40.0, 10.0, null,
//                List.of(new MerchantController.ProductVariantRequest("S", Color.GREEN, 5)), false, null
//        ));
//        assertThat(((MerchantController.ProductResponse) inactive.getBody()).status()).isEqualTo("Inactivo");
//
//        assertThat(controller.createProduct(auth, 10, new MerchantController.ProductRequest(
//                "Polo", "Desc", 40.0, 10.0, List.of("data:image/png;base64,abc"), null, true, null
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(controller.createProduct(auth, 10, new MerchantController.ProductRequest(
//                "Polo", "Desc", 40.0, 10.0, null, null, true, "estado-raro"
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(controller.createProduct(auth, 10, new MerchantController.ProductRequest(
//                "Polo", "Desc", 40.0, 10.0, null,
//                List.of(new MerchantController.ProductVariantRequest("S", Color.GREEN, -1)), true, null
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//
//        List<Discount> fiveDiscounts = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            Discount existing = discount(100 + i, product);
//            existing.setStore(store);
//            fiveDiscounts.add(existing);
//        }
//        when(discountRepository.findByStoreId(10)).thenReturn(fiveDiscounts);
//        assertThat(controller.createDiscount(auth, 10, new MerchantController.DiscountRequest(
//                null, VolumeType.UNIT, 1, 2, 10.0, true,
//                null, "Limitado", "Porcentaje", "Todo el catalogo", 0
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//
//        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
//        assertThat(controller.createDiscount(auth, 10, new MerchantController.DiscountRequest(
//                null, VolumeType.UNIT, 1, 2, 10.0, true,
//                null, "Categoria", "Porcentaje", "Categoria", 0
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(controller.createDiscount(auth, 10, new MerchantController.DiscountRequest(
//                null, VolumeType.UNIT, 1, 2, 10.0, true,
//                null, "Producto", "Porcentaje", "Producto especifico", 0
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(controller.createDiscount(auth, 10, new MerchantController.DiscountRequest(
//                null, VolumeType.UNIT, 1, 2, 150.0, true,
//                null, "Mayor a cien", "Porcentaje", "Todo el catalogo", 0
//        )).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//
//        doThrow(new org.springframework.dao.DataIntegrityViolationException("fk"))
//                .when(productRepository).delete(product);
//        assertThat(controller.deleteProduct(auth, 20, 10).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//    }
//
//    private static Authentication auth(String name) {
//        Authentication authentication = mock(Authentication.class);
//        when(authentication.getName()).thenReturn(name);
//        return authentication;
//    }
//
//    private static Merchant merchant() {
//        UserAccount account = new UserAccount();
//        account.setId(7);
//        account.setEmail("merchant@test.com");
//        account.setPassword("Secret123");
//
//        Merchant merchant = new Merchant();
//        merchant.setId(8);
//        merchant.setUserAccount(account);
//        merchant.setFirstName("Ana");
//        merchant.setPaternalSurname("Perez");
//        merchant.setMaternalSurname("Lopez");
//        merchant.setPhone("999999999");
//        merchant.setRuc("20123456789");
//        merchant.setActive(true);
//        return merchant;
//    }
//
//    private static StoreCategory category() {
//        StoreCategory category = new StoreCategory();
//        category.setId(1);
//        category.setStoreCategoryName("Moda");
//        return category;
//    }
//
//    private static Store store(Integer id, Merchant merchant, StoreCategory category, String name) {
//        Store store = new Store();
//        store.setId(id);
//        store.setStoreName(name);
//        store.setSlug(name.toLowerCase().replace(" ", "-"));
//        store.setDescription("Tienda");
//        store.setLogoUrl("logo.png");
//        store.setPrimaryColor(PrimaryColor.ONYX_BLACK);
//        store.setSecondaryColor(SecondaryColor.SLATE);
//        store.setTertiaryColor(TertiaryColor.RAW_GOLD);
//        store.setStoreStatus(StoreStatus.ACTIVE);
//        store.setCategory(category);
//        store.setMerchant(merchant);
//        return store;
//    }
//
//    private static ProductVariant productVariant(Integer id) {
//        ProductVariant variant = new ProductVariant();
//        variant.setId(id);
//        variant.setSize("M");
//        variant.setColor(Color.BLACK);
//        variant.setStock(10);
//        variant.setActive(true);
//        return variant;
//    }
//
//    private static Product product(Integer id, Store store, String name, boolean active) {
//        Product product = new Product();
//        product.setId(id);
//        product.setStore(store);
//        product.setName(name);
//        product.setDescription("Producto");
//        product.setBasePrice(30.0);
//        product.setCostPrice(15.0);
//        product.setImageUrls(new ArrayList<>(List.of("img.png")));
//        product.setAttributes(new ArrayList<>());
//        product.setVariants(new ArrayList<>(List.of(productVariant(id + 1))));
//        product.setActive(active);
//        product.setStatus(active ? ProductStatus.ACTIVE : ProductStatus.DRAFT);
//        return product;
//    }
//
//    private static Discount discount(Integer id, Product product) {
//        Discount discount = new Discount();
//        discount.setId(id);
//        discount.setProduct(product);
//        discount.setVolumeType(VolumeType.UNIT);
//        discount.setMinQuantity(2);
//        discount.setMaxQuantity(5);
//        discount.setDiscountPercentage(10.0);
//        discount.setActive(true);
//        return discount;
//    }
//
//    private static Order order(Integer id, ProductVariant variant) {
//        OrderItem item = new OrderItem();
//        item.setProductVariant(variant);
//        item.setQuantity(2);
//        item.setUnitPrice(20.0);
//        item.setSubTotal(40.0);
//
//        Order order = new Order();
//        order.setId(id);
//        order.setItems(List.of(item));
//        order.setStatus(OrderStatus.IN_PREPARATION);
//        order.setCreatedAt(LocalDateTime.now());
//        order.setPartialTotal(40.0);
//        order.setFinalTotal(40.0);
//        order.setTotalDiscount(0.0);
//        return order;
//    }
//
//    private static Quotation quotation(Integer id, ProductVariant variant) {
//        Customer customer = new Customer();
//        customer.setFirstName("Luis");
//        customer.setPaternalSurname("Rojas");
//        customer.setMaternalSurname("Diaz");
//
//        ShoppingCart cart = new ShoppingCart();
//        cart.setCustomer(customer);
//
//        QuotationItem item = new QuotationItem();
//        item.setProductVariant(variant);
//        item.setQuantity(1);
//        item.setPrice(30.0);
//        item.setSubTotal(30.0);
//
//        Quotation quotation = new Quotation();
//        quotation.setId(id);
//        quotation.setShoppingCart(cart);
//        quotation.setItems(List.of(item));
//        quotation.setStatus(QuotationStatus.PENDING);
//        quotation.setRequestedAt(LocalDateTime.now());
//        quotation.setSubTotal(30.0);
//        quotation.setDiscount(0.0);
//        quotation.setTotalAmount(30.0);
//        quotation.setDescription("Cotizacion");
//        return quotation;
//    }
//
//    private static byte[] zip(String fileName) throws Exception {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
//            zos.putNextEntry(new ZipEntry(fileName));
//            zos.write("fake-image".getBytes(StandardCharsets.UTF_8));
//            zos.closeEntry();
//        }
//        return baos.toByteArray();
//    }
//}

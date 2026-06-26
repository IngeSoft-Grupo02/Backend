package pe.edu.pucp.kingstore.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.user.CustomerProfileDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.user.RegisterCustomerDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.SystemAdministrator;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
public class UserAccountService extends AbstractCrudService<UserAccount> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÁÉÍÓÚÑÜáéíóúñü'\\-\\s]+$");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9}$");

    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final SystemAdministratorRepository administratorRepository;
    private final StoreRepository storeRepository;
    private final PasswordHashService passwordHashService = new PasswordHashService();

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            CustomerRepository customerRepository,
            MerchantRepository merchantRepository,
            SystemAdministratorRepository administratorRepository,
            StoreRepository storeRepository
    ) {
        super(userAccountRepository, "User account");
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
        this.administratorRepository = administratorRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO authenticate(LoginRequestDTO request) {
        if (request == null) {
            throw new BusinessRuleException("Login request is required");
        }
        requireText(request.getEmail(), "Email");
        requireText(request.getPassword(), "Password");

        UserAccount account = userAccountRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessRuleException("ACCOUNT_NOT_FOUND"));
        if (!Boolean.TRUE.equals(account.getActive())) {
            throw new BusinessRuleException("ACCOUNT_INACTIVE");
        }
        if (!passwordHashService.matches(request.getPassword(), account.getPassword())) {
            throw new BusinessRuleException("BAD_CREDENTIALS");
        }

        LoginResponseDTO response = new LoginResponseDTO();
        response.setId(account.getId());
        response.setEmail(account.getEmail());
        response.setRole(resolveRole(account.getId()));
        return response;
    }

    @Override
    protected void validateForSave(UserAccount account) {
        requireText(account.getEmail(), "Email");
        requireText(account.getPassword(), "Password");
        account.setEmail(normalizeEmail(account.getEmail()));
        account.setPassword(passwordHashService.hash(account.getPassword()));

        userAccountRepository.findByEmail(account.getEmail())
                .filter(existing -> !existing.getId().equals(account.getId()))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Email is already registered");
                });
    }

    private Role resolveRole(Integer userAccountId) {
        // Un UserAccount cliente puede tener varios Customer (uno por tienda): basta con
        // que exista alguno para resolver el rol CUSTOMER (no usar findByUserAccountId,
        // que asume un único perfil y lanzaría con múltiples membresías).
        if (customerRepository.existsByUserAccountId(userAccountId)) {
            return Role.CUSTOMER;
        }
        var merchant = merchantRepository.findByUserAccountId(userAccountId);
        if (merchant.isPresent()) {
            if (!Boolean.TRUE.equals(merchant.get().getActive())) {
                throw new BusinessRuleException("ACCOUNT_INACTIVE");
            }
            return Role.MERCHANT;
        }
        if (administratorRepository.findByUserAccountId(userAccountId).isPresent()) {
            return Role.SYSTEM_ADMIN;
        }
        throw new BusinessRuleException("ROLE_NOT_ASSIGNED");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    /* H-Admin02 */

    @Transactional
    public UserAccount createWithRole(CreateUserDTO dto) {
        if (dto == null) throw new BusinessRuleException("Request is required");
        if (dto.getRole() == null) throw new BusinessRuleException("Role is required");

        // Crear UserAccount
        UserAccount account = new UserAccount();
        account.setEmail(dto.getEmail());
        account.setPassword(dto.getPassword());
        UserAccount created = create(account);

        // Crear perfil segÃºn rol
        switch (dto.getRole()) {
            case CUSTOMER -> {
                if(dto.getStoreId() == null)
                    throw new BusinessRuleException("Store is required for customers");
                Store store = storeRepository.findById(dto.getStoreId())  // necesitas inyectar StoreRepository
                        .orElseThrow(() -> new BusinessRuleException("Store not found"));

                Customer customer = new Customer();
                customer.setUserAccount(created);
                customer.setStore(store);
                customer.setDocumentNumber(dto.getDocumentNumber());
                customer.setDocumentType(dto.getDocumentType());
                customer.setFirstName(dto.getFirstName());
                customer.setPaternalSurname(dto.getPaternalSurname());
                customer.setMaternalSurname(dto.getMaternalSurname());
                customer.setBirthDate(dto.getBirthDate());
                customer.setPhone(dto.getPhone());
                customer.setGender(dto.getGender());
                customer.setActive(true);
                customerRepository.save(customer);
            }
            case MERCHANT -> {
                if (dto.getRuc() == null || dto.getRuc().isBlank())
                    throw new BusinessRuleException("RUC is required for merchants");
                Merchant merchant = new Merchant();
                merchant.setUserAccount(created);
                merchant.setDocumentNumber(dto.getDocumentNumber());
                merchant.setDocumentType(dto.getDocumentType());
                merchant.setFirstName(dto.getFirstName());
                merchant.setPaternalSurname(dto.getPaternalSurname());
                merchant.setMaternalSurname(dto.getMaternalSurname());
                merchant.setBirthDate(dto.getBirthDate());
                merchant.setPhone(dto.getPhone());
                merchant.setGender(dto.getGender());
                merchant.setRuc(dto.getRuc());
                merchant.setActive(true);
                Merchant savedMerchant = merchantRepository.save(merchant);
                if(dto.getStoreId() != null){
                    Store store = storeRepository.findById(dto.getStoreId())
                            .orElseThrow(() -> new BusinessRuleException("Store not found"));
                    store.setMerchant(savedMerchant);
                    storeRepository.save(store);
                }
            }
            case SYSTEM_ADMIN -> {
                SystemAdministrator admin = new SystemAdministrator();
                admin.setUserAccount(created);
                admin.setDocumentNumber(dto.getDocumentNumber());
                admin.setDocumentType(dto.getDocumentType());
                admin.setFirstName(dto.getFirstName());
                admin.setPaternalSurname(dto.getPaternalSurname());
                admin.setMaternalSurname(dto.getMaternalSurname());
                admin.setBirthDate(dto.getBirthDate());
                admin.setPhone(dto.getPhone());
                admin.setGender(dto.getGender());
                admin.setActive(true);
                admin.setPosition("Administrador del Sistema");
                administratorRepository.save(admin);
            }
        }
        return created;
    }


    //Autenticar cliente por tenant
    @Transactional(readOnly = true)
    public LoginResponseDTO authenticateCustomer(String storeSlug, LoginRequestDTO request) {
        if (request == null) throw new BusinessRuleException("Login request is required");
        requireText(storeSlug, "Store slug");
        requireText(request.getEmail(), "Email");
        requireText(request.getPassword(), "Password");

        UserAccount account = userAccountRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .filter(user -> passwordHashService.matches(request.getPassword(), user.getPassword()))
                .orElseThrow(() -> new BusinessRuleException("Invalid credentials"));

        // El correo es identidad global; la membresía es por tienda. Buscar el Customer de
        // ESTA tienda para este UserAccount (login scopeado por tienda).
        Customer customer = customerRepository
                .findByUserAccountIdAndStore_Slug(account.getId(), storeSlug.trim())
                .orElseThrow(() -> new BusinessRuleException("Customer does not belong to this store"));

        String customerStoreSlug = customer.getStore() != null ? customer.getStore().getSlug() : null;

        LoginResponseDTO response = new LoginResponseDTO();
        response.setId(account.getId());
        response.setEmail(account.getEmail());
        response.setRole(Role.CUSTOMER);
        response.setStoreSlug(customerStoreSlug);
        response.setToken(null); // el token lo genera el controller
        return response;
    }

    //Obtener perfil del cliente autenticado, validando que pertenezca a la tienda del slug
    @Transactional(readOnly = true)
    public CustomerProfileDTO getCustomerProfile(Integer userAccountId, String storeSlug) {
        requireText(storeSlug, "Store slug");

        // Perfil del cliente para ESTA tienda (un UserAccount puede tener varias membresías).
        Customer customer = customerRepository
                .findByUserAccountIdAndStore_Slug(userAccountId, storeSlug.trim())
                .orElseThrow(() -> new BusinessRuleException("Customer does not belong to this store"));

        String customerStoreSlug = customer.getStore() != null ? customer.getStore().getSlug() : null;

        UserAccount account = customer.getUserAccount();

        CustomerProfileDTO dto = new CustomerProfileDTO();
        dto.setId(customer.getId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName((customer.getPaternalSurname() + " " + customer.getMaternalSurname()).trim());
        dto.setEmail(account != null ? account.getEmail() : null);
        dto.setPhone(customer.getPhone());
        dto.setDocumentType(customer.getDocumentType());
        dto.setDocumentNumber(customer.getDocumentNumber());
        dto.setRole(Role.CUSTOMER);
        dto.setStoreSlug(customerStoreSlug);
        return dto;
    }

    @Transactional
    public UserAccount updateUser(Integer id, CreateUserDTO dto) {
        UserAccount account = getById(id);

        // Actualizar email y contraseÃ±a en UserAccount
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            account.setEmail(normalizeEmail(dto.getEmail()));
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            account.setPassword(passwordHashService.hash(dto.getPassword()));
        }
        validateForSave(account);
        userAccountRepository.save(account);

        // Actualizar phone en la entidad Person correspondiente
        if (dto.getPhone() != null) {
            merchantRepository.findByUserAccountId(id).ifPresent(m -> {
                m.setPhone(dto.getPhone());
                merchantRepository.save(m);
            });
            customerRepository.findAllByUserAccountId(id).forEach(c -> {
                c.setPhone(dto.getPhone());
                customerRepository.save(c);
            });
            administratorRepository.findByUserAccountId(id).ifPresent(a -> {
                a.setPhone(dto.getPhone());
                administratorRepository.save(a);
            });
        }

        return userAccountRepository.save(account);
    }

    @Transactional
    public UserAccount createWithRole(CreateUserDTO dto, String slug){
        Store store = storeRepository.findBySlug(slug)
                .filter(s -> s.getStoreStatus() == StoreStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("Store not found or inactive"));
        dto.setStoreId(store.getId());
        return createWithRole(dto);
    }

    // Cliente-04: Registro publico de clientes con unicidad POR TIENDA (no global).
    // Regla de negocio: un mismo cliente (correo+DNI) puede registrarse en varias tiendas,
    // pero no duplicarse dentro de la misma tienda. El correo es identidad global
    // (user_account.email único): si ya existe y la contraseña coincide, se reutiliza la
    // cuenta y se crea la membresía (Customer) de esta tienda; si no existe, se crea cuenta.
    @Transactional
    public UserAccount registerCustomer(RegisterCustomerDTO dto, String slug) {
        validateCustomerRegistration(dto);

        Store store = storeRepository.findBySlug(slug)
                .filter(s -> s.getStoreStatus() == StoreStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("Store not found or inactive"));

        String email = normalizeEmail(dto.getEmail());
        String documentNumber = dto.getDocumentNumber();

        // Unicidad por tienda: solo correo y DNI. Teléfono/nombres/fecha/género/contraseña NO bloquean.
        if (customerRepository.existsByStore_IdAndUserAccount_Email(store.getId(), email)) {
            throw new BusinessRuleException("El correo ya está registrado en esta tienda.");
        }
        if (customerRepository.existsByStore_IdAndDocumentNumber(store.getId(), documentNumber)) {
            throw new BusinessRuleException("El DNI ya está registrado en esta tienda.");
        }

        // Identidad global por correo: reutilizar la cuenta existente o crear una nueva.
        UserAccount account = userAccountRepository.findByEmail(email).orElse(null);
        if (account != null) {
            if (!customerRepository.existsByUserAccountId(account.getId())) {
                // El correo pertenece a una cuenta de otro rol (Comerciante/Admin).
                throw new BusinessRuleException("Este correo pertenece a otro tipo de cuenta.");
            }
            if (!passwordHashService.matches(dto.getPassword(), account.getPassword())) {
                throw new BusinessRuleException("La contraseña no coincide con la cuenta existente para este correo.");
            }
        } else {
            UserAccount nuevo = new UserAccount();
            nuevo.setEmail(email);
            nuevo.setPassword(passwordHashService.hash(dto.getPassword()));
            account = create(nuevo); // valida formato y unicidad global de email (no existe → OK)
        }

        // Crear la membresía (Customer) de esta tienda para la cuenta global.
        Customer customer = new Customer();
        customer.setUserAccount(account);
        customer.setStore(store);
        customer.setDocumentNumber(documentNumber);
        customer.setDocumentType(dto.getDocumentType());
        customer.setFirstName(dto.getFirstName());
        customer.setPaternalSurname(dto.getPaternalSurname());
        customer.setMaternalSurname(dto.getMaternalSurname());
        customer.setBirthDate(dto.getBirthDate());
        customer.setPhone(dto.getPhone());
        customer.setGender(dto.getGender());
        customer.setActive(true);
        customerRepository.save(customer);

        return account;
    }

    private void validateCustomerRegistration(RegisterCustomerDTO dto) {
        if (dto == null) {
            throw new BusinessRuleException("Registration data is required");
        }

        requireText(dto.getEmail(), "Email");
        String email = dto.getEmail().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessRuleException("Ingresa un correo electrónico válido");
        }
        dto.setEmail(email);

        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            throw new BusinessRuleException("La contraseña debe tener al menos 8 caracteres");
        }

        validatePersonName(dto.getFirstName(), "El nombre");
        validatePersonName(dto.getPaternalSurname(), "El apellido paterno");
        validatePersonName(dto.getMaternalSurname(), "El apellido materno");

        if (dto.getDocumentType() == null) {
            throw new BusinessRuleException("El tipo de documento es obligatorio");
        }
        requireText(dto.getDocumentNumber(), "El número de documento");
        String documentNumber = dto.getDocumentNumber().trim();
        if (!DIGITS_PATTERN.matcher(documentNumber).matches()) {
            throw new BusinessRuleException("El número de documento solo debe contener dígitos");
        }
        validateDocumentNumberLength(dto.getDocumentType(), documentNumber);
        dto.setDocumentNumber(documentNumber);

        requireText(dto.getPhone(), "El teléfono");
        String phone = dto.getPhone().trim();
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessRuleException("El celular debe tener 9 dígitos");
        }
        dto.setPhone(phone);

        if (dto.getBirthDate() == null) {
            throw new BusinessRuleException("La fecha de nacimiento es obligatoria");
        }
        if (dto.getBirthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de nacimiento no puede ser una fecha futura");
        }

        if (dto.getGender() == null) {
            throw new BusinessRuleException("El género es obligatorio");
        }
    }

    private void validatePersonName(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessRuleException(label + " es obligatorio");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 50) {
            throw new BusinessRuleException(label + " debe tener entre 2 y 50 caracteres");
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessRuleException(label + " solo puede contener letras, espacios, guiones y apóstrofes");
        }
    }

    private void validateDocumentNumberLength(DocumentType documentType, String documentNumber) {
        int len = documentNumber.length();
        switch (documentType) {
            case DNI -> {
                if (len != 8) {
                    throw new BusinessRuleException("El DNI debe tener 8 dígitos");
                }
            }
            case FOREIGN_ID_CARD -> {
                if (len < 9 || len > 15) {
                    throw new BusinessRuleException("El carné de extranjería debe tener entre 9 y 15 dígitos");
                }
            }
            case PASSPORT -> {
                if (len < 6 || len > 20) {
                    throw new BusinessRuleException("El pasaporte debe tener entre 6 y 20 dígitos");
                }
            }
        }
    }
}

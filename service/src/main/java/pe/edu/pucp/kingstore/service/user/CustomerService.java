package pe.edu.pucp.kingstore.service.user;

import org.springframework.stereotype.Service;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

@Service
public class CustomerService extends AbstractCrudService<Customer> {

    public CustomerService(CustomerRepository customerRepository) {
        super(customerRepository, "Customer");
    }

    @Override
    protected void validateForSave(Customer customer) {
        validatePerson(customer);
        if (customer.getUserAccount() == null || customer.getUserAccount().getId() == null) {
            throw new BusinessRuleException("Customer must have a user account");
        }
    }

    static void validatePerson(pe.edu.pucp.kingstore.domain.model.user.Person person) {
        if (person.getDocumentType() == null) {
            throw new BusinessRuleException("Document type is required");
        }
        if (person.getBirthDate() == null) {
            throw new BusinessRuleException("Birth date is required");
        }
        if (person.getGender() == null) {
            throw new BusinessRuleException("Gender is required");
        }
        if (person.getDocumentNumber() == null || person.getDocumentNumber().isBlank()) {
            throw new BusinessRuleException("Document number is required");
        }
        if (person.getFirstName() == null || person.getFirstName().isBlank()) {
            throw new BusinessRuleException("First name is required");
        }
        if (person.getPaternalSurname() == null || person.getPaternalSurname().isBlank()) {
            throw new BusinessRuleException("Paternal surname is required");
        }
        if (person.getMaternalSurname() == null || person.getMaternalSurname().isBlank()) {
            throw new BusinessRuleException("Maternal surname is required");
        }
    }
}

package pe.edu.pucp.kingstore.service.user;

import org.springframework.stereotype.Service;
import pe.edu.pucp.kingstore.domain.model.user.SystemAdministrator;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

@Service
public class SystemAdministratorService extends AbstractCrudService<SystemAdministrator> {

    public SystemAdministratorService(SystemAdministratorRepository administratorRepository) {
        super(administratorRepository, "System administrator");
    }

    @Override
    protected void validateForSave(SystemAdministrator administrator) {
        CustomerService.validatePerson(administrator);
        if (administrator.getUserAccount() == null || administrator.getUserAccount().getId() == null) {
            throw new BusinessRuleException("System administrator must have a user account");
        }
        requireText(administrator.getPosition(), "Position");
    }
}

package pe.edu.pucp.kingstore.service.user;

import org.springframework.stereotype.Service;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

@Service
public class MerchantService extends AbstractCrudService<Merchant> {

    public MerchantService(MerchantRepository merchantRepository) {
        super(merchantRepository, "Merchant");
    }

    @Override
    protected void validateForSave(Merchant merchant) {
        CustomerService.validatePerson(merchant);
        if (merchant.getUserAccount() == null || merchant.getUserAccount().getId() == null) {
            throw new BusinessRuleException("Merchant must have a user account");
        }
        requireText(merchant.getRuc(), "RUC");
    }
}

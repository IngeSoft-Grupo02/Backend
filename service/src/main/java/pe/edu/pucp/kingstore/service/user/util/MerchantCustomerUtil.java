package pe.edu.pucp.kingstore.service.user.util;
import pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import java.util.List;

public final class MerchantCustomerUtil {

    private MerchantCustomerUtil() {}

    public static String customerName(Customer customer) {
        if (customer == null) {
            return "Cliente";
        }
        return fullName(
                customer.getFirstName(),
                customer.getPaternalSurname(),
                customer.getMaternalSurname()
        );
    }

    public static String fullName(String firstName, String paternalSurname, String maternalSurname) {
        return String.join(" ", List.of(
                        MerchantStringUtil.safe(firstName),
                        MerchantStringUtil.safe(paternalSurname),
                        MerchantStringUtil.safe(maternalSurname)))
                .replaceAll("\\s+", " ")
                .trim();
    }
}
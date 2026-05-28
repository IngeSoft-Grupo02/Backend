package pe.edu.pucp.kingstore.service.common;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Integer id) {
        super(resourceName + " with id " + id + " was not found");
    }
}

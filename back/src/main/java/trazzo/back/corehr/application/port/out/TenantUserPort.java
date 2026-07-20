package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.TenantUserState;

import java.util.List;
import java.util.Optional;

public interface TenantUserPort {
    Optional<TenantUserBasicInfo> findBasicInfoById(Long tenantUserId);
    Optional<TenantUserState> findStateById(Long tenantUserId);
    boolean existsById(Long tenantUserId);

    List<TenantUserProfileProjection> findAllProfiles(String search, String status, int page, int size, String sort);
    long countAllProfiles(String search, String status);
    Optional<TenantUserProfileProjection> findProfileById(Long id);

    Long saveTenantUser(java.util.UUID masterUserId);
    void updateState(Long id, TenantUserState state);
    void softDelete(Long id);
    void hardDelete(Long id);

    void assignRole(Long tenantUserId, String roleId);
    void removeRole(Long tenantUserId);
    Optional<String> findRoleIdByTenantUserId(Long tenantUserId);

    Integer savePerson(String documentType, String documentValue, String name, String fatherSurname, String motherSurname);
    void updatePerson(Integer personId, String name, String fatherSurname, String motherSurname);
    Optional<Integer> findPersonIdByDocument(String documentType, String documentValue);

    record TenantUserBasicInfo(Long id, String nombre, String apellidoPaterno,
                                String apellidoMaterno, String email, String phone) {
    }

    record TenantUserProfileProjection(
        Long id,
        String email,
        String phone,
        String estado,
        boolean mustChangePassword,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Integer personId,
        String documentType,
        String documentValue,
        String name,
        String fatherSurname,
        String motherSurname,
        String roleId,
        String roleName
    ) {}
}

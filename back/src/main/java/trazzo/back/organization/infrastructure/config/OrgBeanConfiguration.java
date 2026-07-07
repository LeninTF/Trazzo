package trazzo.back.organization.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import trazzo.back.organization.application.port.in.*;
import trazzo.back.organization.application.port.out.*;
import trazzo.back.organization.application.usecase.*;

@Configuration
public class OrgBeanConfiguration {

    @Bean
    public BranchUseCase branchUseCase(BranchRepositoryPort branchRepository) {
        return new BranchService(branchRepository);
    }

    @Bean
    public AreaUseCase areaUseCase(AreaRepositoryPort areaRepository, BranchRepositoryPort branchRepository) {
        return new AreaService(areaRepository, branchRepository);
    }

    @Bean
    public DepartmentUseCase departmentUseCase(DepartmentRepositoryPort departmentRepository, AreaRepositoryPort areaRepository) {
        return new DepartmentService(departmentRepository, areaRepository);
    }

    @Bean
    public RoleUseCase roleUseCase(RoleRepositoryPort roleRepository) {
        return new RoleService(roleRepository);
    }

    @Bean
    public PermissionUseCase permissionUseCase(PermissionRepositoryPort permissionRepository) {
        return new PermissionService(permissionRepository);
    }

    @Bean
    public RolePermissionsUseCase rolePermissionsUseCase(
            RolePermissionsRepositoryPort rolePermissionsRepository,
            RoleRepositoryPort roleRepository,
            PermissionRepositoryPort permissionRepository
    ) {
        return new RolePermissionsService(rolePermissionsRepository, roleRepository, permissionRepository);
    }

    @Bean
    public UserRoleUseCase userRoleUseCase(UserRoleRepositoryPort userRoleRepository, RoleRepositoryPort roleRepository) {
        return new UserRoleService(userRoleRepository, roleRepository);
    }
}

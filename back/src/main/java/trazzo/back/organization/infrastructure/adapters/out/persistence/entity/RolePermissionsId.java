package trazzo.back.organization.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RolePermissionsId implements Serializable {

    @Column(name = "role_id", length = 36)
    private String roleId;

    @Column(name = "permission_id", length = 36)
    private String permissionId;
}

package trazzo.back.organization.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RolePermissionsId implements Serializable {

    @Column(name = "role_id", columnDefinition = "uuid")
    private UUID roleId;

    @Column(name = "permission_id", columnDefinition = "uuid")
    private UUID permissionId;
}

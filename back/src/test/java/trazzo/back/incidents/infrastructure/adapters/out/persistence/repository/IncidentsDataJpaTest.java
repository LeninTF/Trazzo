package trazzo.back.incidents.infrastructure.adapters.out.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEvidenceEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentPermissionEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentTypeEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
class IncidentsDataJpaTest {

    @Autowired
    private IncidentSpringDataRepository incidentRepository;

    @Autowired
    private IncidentTypeSpringDataRepository incidentTypeRepository;

    @Autowired
    private IncidentEvidenceSpringDataRepository incidentEvidenceRepository;

    @Autowired
    private IncidentPermissionSpringDataRepository incidentPermissionRepository;

    @Test
    void shouldSaveAndFindIncidentType() {
        var type = new IncidentTypeEntity();
        type.setId("type-1");
        type.setNombre("Vacaciones");
        type.setDescripcion("Permiso por vacaciones");
        type.setActivo(true);

        incidentTypeRepository.save(type);

        Optional<IncidentTypeEntity> found = incidentTypeRepository.findByNombre("Vacaciones");
        assertThat(found).isPresent();
        assertThat(found.get().isActivo()).isTrue();

        assertThat(incidentTypeRepository.existsByNombre("Vacaciones")).isTrue();
    }

    @Test
    void shouldSaveAndFindIncident() {
        var type = new IncidentTypeEntity();
        type.setId("type-10");
        type.setNombre("Permiso");
        type.setActivo(true);
        incidentTypeRepository.save(type);

        var incident = new IncidentEntity();
        incident.setId("inc-10");
        incident.setTenantUserId("user-10");
        incident.setIncidentTypeId("type-10");
        incident.setState(IncidentState.PENDIENTE);
        incidentRepository.save(incident);

        Optional<IncidentEntity> found = incidentRepository.findById("inc-10");
        assertThat(found).isPresent();
        assertThat(found.get().getTenantUserId()).isEqualTo("user-10");
        assertThat(found.get().getState()).isEqualTo(IncidentState.PENDIENTE);
    }

    @Test
    void shouldFindIncidentByFilters() {
        var incident = new IncidentEntity();
        incident.setId("inc-20");
        incident.setTenantUserId("user-20");
        incident.setIncidentTypeId("type-20");
        incident.setState(IncidentState.APROBADO);
        incident.setComment("filtro test");
        incidentRepository.save(incident);

        Page<IncidentEntity> result = incidentRepository.findByFilters(
                "user-20", null, "type-20",
                null, null, "filtro", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getComment()).contains("filtro");
    }

    @Test
    void shouldSaveAndFindIncidentEvidence() {
        var incident = new IncidentEntity();
        incident.setId("inc-ev-1");
        incident.setTenantUserId("user-ev");
        incident.setIncidentTypeId("type-ev");
        incident.setState(IncidentState.PENDIENTE);
        incidentRepository.save(incident);

        var evidence = new IncidentEvidenceEntity();
        evidence.setId("ev-10");
        evidence.setIncidentId("inc-ev-1");
        evidence.setFileName("documento.pdf");
        evidence.setFileKey("http://example.com/doc.pdf");
        evidence.setMimeType("application/pdf");
        evidence.setFileSize(2048);
        evidence.setDeleted(false);

        incidentEvidenceRepository.save(evidence);

        List<IncidentEvidenceEntity> found = incidentEvidenceRepository.findByIncidentId("inc-ev-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getFileName()).isEqualTo("documento.pdf");
    }

    @Test
    void shouldDeleteEvidenceByIncidentId() {
        var incident = new IncidentEntity();
        incident.setId("inc-ev-2");
        incident.setTenantUserId("user-ev-2");
        incident.setIncidentTypeId("type-ev-2");
        incident.setState(IncidentState.PENDIENTE);
        incidentRepository.save(incident);

        var evidence = new IncidentEvidenceEntity();
        evidence.setId("ev-20");
        evidence.setIncidentId("inc-ev-2");
        evidence.setFileName("borrar.txt");
        evidence.setFileKey("http://example.com/borrar.txt");
        evidence.setMimeType("text/plain");
        evidence.setFileSize(512);
        evidence.setDeleted(false);
        incidentEvidenceRepository.save(evidence);

        incidentEvidenceRepository.deleteByIncidentId("inc-ev-2");

        List<IncidentEvidenceEntity> found = incidentEvidenceRepository.findByIncidentId("inc-ev-2");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldSaveAndFindIncidentPermission() {
        var permission = new IncidentPermissionEntity();
        permission.setId("perm-10");
        permission.setIncidentId("inc-perm-1");
        permission.setStartDate(LocalDate.of(2026, 6, 1));
        permission.setEndDate(LocalDate.of(2026, 6, 5));
        permission.setDaysGranted(4);

        incidentPermissionRepository.save(permission);

        Optional<IncidentPermissionEntity> found = incidentPermissionRepository.findByIncidentId("inc-perm-1");
        assertThat(found).isPresent();
        assertThat(found.get().getDaysGranted()).isEqualTo(4);
    }

    @Test
    void shouldDeletePermissionByIncidentId() {
        var permission = new IncidentPermissionEntity();
        permission.setId("perm-20");
        permission.setIncidentId("inc-perm-2");
        permission.setStartDate(LocalDate.now());
        permission.setEndDate(LocalDate.now().plusDays(2));
        permission.setDaysGranted(2);
        incidentPermissionRepository.save(permission);

        incidentPermissionRepository.deleteByIncidentId("inc-perm-2");

        Optional<IncidentPermissionEntity> found = incidentPermissionRepository.findByIncidentId("inc-perm-2");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindIncidentTypeByActivo() {
        var type1 = new IncidentTypeEntity();
        type1.setId("type-30");
        type1.setNombre("Activo1");
        type1.setActivo(true);
        incidentTypeRepository.save(type1);

        var type2 = new IncidentTypeEntity();
        type2.setId("type-31");
        type2.setNombre("Inactivo1");
        type2.setActivo(false);
        incidentTypeRepository.save(type2);

        var activos = incidentTypeRepository.findByActivo(true, PageRequest.of(0, 10));
        var inactivos = incidentTypeRepository.findByActivo(false, PageRequest.of(0, 10));

        assertThat(activos.getContent()).isNotEmpty();
        assertThat(inactivos.getContent()).isNotEmpty();
    }
}

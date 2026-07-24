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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentSpecifications.byFilters;

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
        type.setNombre("Vacaciones");
        type.setDescripcion("Permiso por vacaciones");
        type.setActivo(true);

        var saved = incidentTypeRepository.save(type);

        Optional<IncidentTypeEntity> found = incidentTypeRepository.findByNombre("Vacaciones");
        assertThat(found).isPresent();
        assertThat(found.get().isActivo()).isTrue();
        assertThat(found.get().getId()).isNotNull();

        assertThat(incidentTypeRepository.existsByNombre("Vacaciones")).isTrue();
    }

    @Test
    void shouldSaveAndFindIncident() {
        var type = new IncidentTypeEntity();
        type.setNombre("Permiso");
        type.setActivo(true);
        var savedType = incidentTypeRepository.save(type);

        var incident = new IncidentEntity();
        incident.setTenantUserId(10);
        incident.setIncidentTypeId(savedType.getId());
        incident.setState(IncidentState.PENDIENTE);
        var savedIncident = incidentRepository.save(incident);

        Optional<IncidentEntity> found = incidentRepository.findById(savedIncident.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTenantUserId()).isEqualTo(10);
        assertThat(found.get().getState()).isEqualTo(IncidentState.PENDIENTE);
    }

    @Test
    void shouldFindIncidentByFilters() {
        var type = new IncidentTypeEntity();
        type.setNombre("Filtro");
        type.setActivo(true);
        var savedType = incidentTypeRepository.saveAndFlush(type);

        var incident = new IncidentEntity();
        incident.setTenantUserId(20);
        incident.setIncidentTypeId(savedType.getId());
        incident.setState(IncidentState.APROBADO);
        incident.setComment("filtro test");
        incidentRepository.saveAndFlush(incident);

        Page<IncidentEntity> result = incidentRepository.findAll(
                byFilters(20, null, savedType.getId(), null, null, "filtro"),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getComment()).contains("filtro");
    }

    @Test
    void shouldFindIncidentByCommentIgnoringCase() {
        var type = new IncidentTypeEntity();
        type.setNombre("Urgencia");
        type.setActivo(true);
        var savedType = incidentTypeRepository.saveAndFlush(type);

        var incident = new IncidentEntity();
        incident.setTenantUserId(21);
        incident.setIncidentTypeId(savedType.getId());
        incident.setState(IncidentState.PENDIENTE);
        incident.setComment("Permiso Medico Urgente");
        incidentRepository.saveAndFlush(incident);

        Page<IncidentEntity> result = incidentRepository.findAll(
                byFilters(21, IncidentState.PENDIENTE, savedType.getId(), null, null, "mEdIcO"),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getComment()).isEqualTo("Permiso Medico Urgente");
    }

    @Test
    void shouldFindIncidentByFiltersWithoutSearch() {
        var type = new IncidentTypeEntity();
        type.setNombre("SinBusqueda");
        type.setActivo(true);
        var savedType = incidentTypeRepository.saveAndFlush(type);

        var incident = new IncidentEntity();
        incident.setTenantUserId(22);
        incident.setIncidentTypeId(savedType.getId());
        incident.setState(IncidentState.PENDIENTE);
        incident.setComment("sin busqueda");
        incidentRepository.saveAndFlush(incident);

        Page<IncidentEntity> result = incidentRepository.findAll(
                byFilters(22, IncidentState.PENDIENTE, savedType.getId(), null, null, null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getComment()).isEqualTo("sin busqueda");
    }

    @Test
    void shouldFindIncidentByDateFiltersWithoutNullTypingIssues() {
        var type = new IncidentTypeEntity();
        type.setNombre("Fecha");
        type.setActivo(true);
        var savedType = incidentTypeRepository.saveAndFlush(type);

        var incident = new IncidentEntity();
        incident.setTenantUserId(23);
        incident.setIncidentTypeId(savedType.getId());
        incident.setState(IncidentState.PENDIENTE);
        incident.setComment("con fecha");
        incidentRepository.saveAndFlush(incident);

        var start = incident.getCreatedAt().minusMinutes(1);
        var end = incident.getCreatedAt().plusMinutes(1);

        Page<IncidentEntity> result = incidentRepository.findAll(
                byFilters(23, IncidentState.PENDIENTE, savedType.getId(), start, end, null),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getComment()).isEqualTo("con fecha");
    }

    @Test
    void shouldSaveAndFindIncidentEvidence() {
        var incident = new IncidentEntity();
        incident.setTenantUserId(1);
        incident.setIncidentTypeId(1);
        incident.setState(IncidentState.PENDIENTE);
        var savedIncident = incidentRepository.save(incident);

        var evidence = new IncidentEvidenceEntity();
        evidence.setIncidentId(savedIncident.getId());
        evidence.setFileName("documento.pdf");
        evidence.setFileKey("evidences/2/1/uuid/documento.pdf");
        evidence.setMimeType("application/pdf");
        evidence.setFileSize(2048);
        evidence.setDeleted(false);
        incidentEvidenceRepository.save(evidence);

        List<IncidentEvidenceEntity> found = incidentEvidenceRepository.findByIncidentId(savedIncident.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getFileName()).isEqualTo("documento.pdf");
        assertThat(found.get(0).getFileKey()).isEqualTo("evidences/2/1/uuid/documento.pdf");
    }

    @Test
    void shouldDeleteEvidenceByIncidentId() {
        var incident = new IncidentEntity();
        incident.setTenantUserId(1);
        incident.setIncidentTypeId(1);
        incident.setState(IncidentState.PENDIENTE);
        var savedIncident = incidentRepository.save(incident);

        var evidence = new IncidentEvidenceEntity();
        evidence.setIncidentId(savedIncident.getId());
        evidence.setFileName("borrar.txt");
        evidence.setFileKey("evidences/2/1/uuid/borrar.txt");
        evidence.setMimeType("text/plain");
        evidence.setFileSize(512);
        evidence.setDeleted(false);
        incidentEvidenceRepository.save(evidence);

        incidentEvidenceRepository.deleteByIncidentId(savedIncident.getId());

        List<IncidentEvidenceEntity> found = incidentEvidenceRepository.findByIncidentId(savedIncident.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldRejectEvidenceWithNullFileKey() {
        var incident = new IncidentEntity();
        incident.setTenantUserId(1);
        incident.setIncidentTypeId(1);
        incident.setState(IncidentState.PENDIENTE);
        var savedIncident = incidentRepository.save(incident);

        var evidence = new IncidentEvidenceEntity();
        evidence.setIncidentId(savedIncident.getId());
        evidence.setFileName("no-key.pdf");
        evidence.setMimeType("application/pdf");
        evidence.setFileSize(64);
        evidence.setDeleted(false);

        assertThatThrownBy(() -> {
            incidentEvidenceRepository.saveAndFlush(evidence);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void shouldSaveAndFindIncidentPermission() {
        var permission = new IncidentPermissionEntity();
        permission.setIncidentId(1);
        permission.setStartDate(LocalDate.of(2026, 6, 1));
        permission.setEndDate(LocalDate.of(2026, 6, 5));
        permission.setDaysGranted(4);
        incidentPermissionRepository.save(permission);

        Optional<IncidentPermissionEntity> found = incidentPermissionRepository.findByIncidentId(1);
        assertThat(found).isPresent();
        assertThat(found.get().getDaysGranted()).isEqualTo(4);
    }

    @Test
    void shouldDeletePermissionByIncidentId() {
        var permission = new IncidentPermissionEntity();
        permission.setIncidentId(2);
        permission.setStartDate(LocalDate.now());
        permission.setEndDate(LocalDate.now().plusDays(2));
        permission.setDaysGranted(2);
        incidentPermissionRepository.save(permission);

        incidentPermissionRepository.deleteByIncidentId(2);

        Optional<IncidentPermissionEntity> found = incidentPermissionRepository.findByIncidentId(2);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindIncidentTypeByActivo() {
        var type1 = new IncidentTypeEntity();
        type1.setNombre("Activo1");
        type1.setActivo(true);
        incidentTypeRepository.save(type1);

        var type2 = new IncidentTypeEntity();
        type2.setNombre("Inactivo1");
        type2.setActivo(false);
        incidentTypeRepository.save(type2);

        var activos = incidentTypeRepository.findByActivo(true, PageRequest.of(0, 10));
        var inactivos = incidentTypeRepository.findByActivo(false, PageRequest.of(0, 10));

        assertThat(activos.getContent()).isNotEmpty();
        assertThat(inactivos.getContent()).isNotEmpty();
    }
}

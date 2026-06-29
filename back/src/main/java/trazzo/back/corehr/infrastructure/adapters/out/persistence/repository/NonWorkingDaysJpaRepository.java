package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.NonWorkingDaysEntity;

import java.time.LocalDate;

@Repository
public interface NonWorkingDaysJpaRepository extends JpaRepository<NonWorkingDaysEntity, Long> {

    boolean existsByDate(LocalDate date);

    @Query("SELECT n FROM NonWorkingDaysEntity n WHERE " +
           "(:from IS NULL OR n.date >= :from) AND " +
           "(:to IS NULL OR n.date <= :to) AND " +
           "(:isRecurring IS NULL OR n.isRecurring = :isRecurring)")
    Page<NonWorkingDaysEntity> findByDateBetweenOrIsRecurring(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("isRecurring") Boolean isRecurring,
            Pageable pageable);

    @Query("SELECT COUNT(n) FROM NonWorkingDaysEntity n WHERE " +
           "(:from IS NULL OR n.date >= :from) AND " +
           "(:to IS NULL OR n.date <= :to) AND " +
           "(:isRecurring IS NULL OR n.isRecurring = :isRecurring)")
    long countByDateBetweenOrIsRecurring(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("isRecurring") Boolean isRecurring);
}

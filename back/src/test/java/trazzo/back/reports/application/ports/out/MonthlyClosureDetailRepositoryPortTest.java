package trazzo.back.reports.application.ports.out;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

class MonthlyClosureDetailRepositoryPortTest {

    @Test
    void shouldBeInterface() {
        assertTrue(MonthlyClosureDetailRepositoryPort.class.isInterface());
    }

    @Test
    void shouldDefineSaveMethod() throws NoSuchMethodException {
        MonthlyClosureDetailRepositoryPort.class.getDeclaredMethod("save", MonthlyClosureDetail.class);
    }

    @Test
    void shouldDefineSaveAllMethod() throws NoSuchMethodException {
        MonthlyClosureDetailRepositoryPort.class.getDeclaredMethod("saveAll", List.class);
    }

    @Test
    void shouldDefineFindByIdMethod() throws NoSuchMethodException {
        MonthlyClosureDetailRepositoryPort.class.getDeclaredMethod("findById", UUID.class);
    }

    @Test
    void shouldDefineFindByMonthlyClosureIdMethod() throws NoSuchMethodException {
        MonthlyClosureDetailRepositoryPort.class.getDeclaredMethod("findByMonthlyClosureId", UUID.class);
    }

    @Test
    void shouldReturnOptionalFromFindById() throws NoSuchMethodException {
        var method = MonthlyClosureDetailRepositoryPort.class.getDeclaredMethod("findById", UUID.class);
        assertEquals(Optional.class, method.getReturnType());
    }

    @Test
    void shouldReturnListFromFindByMonthlyClosureId() throws NoSuchMethodException {
        var method = MonthlyClosureDetailRepositoryPort.class.getDeclaredMethod("findByMonthlyClosureId", UUID.class);
        assertEquals(List.class, method.getReturnType());
    }
}

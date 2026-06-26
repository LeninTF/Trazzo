package trazzo.back.reports.application.ports.out;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

class MonthlyClosureRepositoryPortTest {

    @Test
    void shouldBeInterface() {
        assertTrue(MonthlyClosureRepositoryPort.class.isInterface());
    }

    @Test
    void shouldDefineSaveMethod() throws NoSuchMethodException {
        MonthlyClosureRepositoryPort.class.getDeclaredMethod("save", MonthlyClosure.class);
    }

    @Test
    void shouldDefineFindByIdMethod() throws NoSuchMethodException {
        MonthlyClosureRepositoryPort.class.getDeclaredMethod("findById", UUID.class);
    }

    @Test
    void shouldDefineFindAllMethod() throws NoSuchMethodException {
        MonthlyClosureRepositoryPort.class.getDeclaredMethod("findAll");
    }

    @Test
    void shouldDefineFindByMonthAndYearMethod() throws NoSuchMethodException {
        MonthlyClosureRepositoryPort.class.getDeclaredMethod("findByMonthAndYear", int.class, int.class);
    }

    @Test
    void shouldReturnOptionalFromFindById() throws NoSuchMethodException {
        var method = MonthlyClosureRepositoryPort.class.getDeclaredMethod("findById", UUID.class);
        assertEquals(Optional.class, method.getReturnType());
    }

    @Test
    void shouldReturnListFromFindAll() throws NoSuchMethodException {
        var method = MonthlyClosureRepositoryPort.class.getDeclaredMethod("findAll");
        assertEquals(List.class, method.getReturnType());
    }
}

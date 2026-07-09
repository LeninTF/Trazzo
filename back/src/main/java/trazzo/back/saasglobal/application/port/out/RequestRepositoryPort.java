package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.request.Request;

public interface RequestRepositoryPort {
    Request save(Request request);
    Optional<Request> findById(Integer id);
    List<Request> findByFilters(Request.Status status, Request.Type type, String search, int page, int size);
    long countByFilters(Request.Status status, Request.Type type, String search);
}

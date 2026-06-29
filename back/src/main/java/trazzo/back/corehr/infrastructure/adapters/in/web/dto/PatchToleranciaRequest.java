package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

public record PatchToleranciaRequest(
        String name,
        Integer minutes,
        String description,
        Boolean activo
) {
}

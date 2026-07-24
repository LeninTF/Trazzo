package trazzo.back.audit.application.dto;

public record PageParams(int page, int size, String sort) {
    public static PageParams of(int page, int size, String sort) {
        return new PageParams(page, size, sort);
    }
}

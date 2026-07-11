package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

public record LoginResponse(String accessToken, String tokenType, UsuarioResponse usuario) {}

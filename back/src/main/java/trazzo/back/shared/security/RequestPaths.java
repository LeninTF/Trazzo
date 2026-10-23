package trazzo.back.shared.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Ruta de la petición dentro de la app, igual que los RequestMatcher de Spring Security: servletPath + pathInfo
 * (con el DispatcherServlet en "/" la ruta llega en el primero; mapeado como "/*" o en MockMvc, en el segundo).
 */
final class RequestPaths {

    private RequestPaths() {
    }

    static String of(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null ? request.getServletPath() : request.getServletPath() + pathInfo;
    }
}

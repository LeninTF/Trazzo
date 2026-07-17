package trazzo.back.saasglobal.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.AddCommentCommand;
import trazzo.back.saasglobal.application.dto.command.ChangeRequestStatusCommand;
import trazzo.back.saasglobal.application.dto.command.SubmitRequestCommand;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.RequestCommentResult;
import trazzo.back.saasglobal.application.dto.result.RequestContactResult;
import trazzo.back.saasglobal.application.dto.result.RequestDetailResult;
import trazzo.back.saasglobal.application.dto.result.RequestRecordResult;
import trazzo.back.saasglobal.application.dto.result.RequestResult;
import trazzo.back.saasglobal.application.port.in.RequestUseCase;
import trazzo.back.saasglobal.application.port.out.EmailService;
import trazzo.back.saasglobal.application.port.out.RequestCommentRepositoryPort;
import trazzo.back.saasglobal.application.port.out.RequestContactRepositoryPort;
import trazzo.back.saasglobal.application.port.out.RequestRecordRepositoryPort;
import trazzo.back.saasglobal.application.port.out.RequestRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRequestCommentRepositoryPort;
import trazzo.back.saasglobal.domain.exception.RequestRateLimitException;
import trazzo.back.saasglobal.domain.model.request.Request;
import trazzo.back.saasglobal.domain.model.request.RequestComments;
import trazzo.back.saasglobal.domain.model.request.RequestContact;
import trazzo.back.saasglobal.domain.model.request.RequestRecord;
import trazzo.back.saasglobal.domain.model.request.UserRequestComment;

@Service
@RequiredArgsConstructor
public class RequestService implements RequestUseCase {

    private static final int RATE_LIMIT_MINUTES = 15;
    private static final int MAX_REQUESTS_PER_TAX_ID = 2;

    private final RequestRepositoryPort requestRepository;
    private final RequestContactRepositoryPort requestContactRepository;
    private final RequestCommentRepositoryPort requestCommentRepository;
    private final UserRequestCommentRepositoryPort userRequestCommentRepository;
    private final RequestRecordRepositoryPort requestRecordRepository;
    private final EmailService emailService;

    @Value("${trazzo.requests.notification-email:solicitudes@trazzo.pe}")
    private String notificationEmail;

    @Override
    public RequestResult submit(SubmitRequestCommand command) {
        String taxId = command.taxId() != null ? command.taxId().trim() : "";
        enforceRateLimit(taxId);

        Request.Type type = Request.Type.valueOf(command.type().trim().toUpperCase());
        String title = "Solicitud de %s - %s".formatted(
                type == Request.Type.TRIAL ? "trial" : "más información", command.companyName());

        Request request = requestRepository.save(Request.create(type, title, command.message()));
        RequestContact contact = requestContactRepository.save(RequestContact.create(
                request.getId(), command.name(), command.lastName(), command.email(),
                command.phoneNumber(), taxId, command.companyName()));

        emailService.send(notificationEmail,
                "Nueva solicitud: " + contact.getCompanyName(),
                "Nueva solicitud de %s recibida.<br>Empresa: %s<br>Contacto: %s %s (%s)<br>Mensaje: %s".formatted(
                        type, escapeHtml(contact.getCompanyName()), escapeHtml(contact.getName()),
                        escapeHtml(contact.getLastName()), escapeHtml(contact.getEmail()),
                        escapeHtml(request.getMessage())));

        return toResult(request, contact);
    }

    private void enforceRateLimit(String taxId) {
        LocalDateTime since = LocalDateTime.now(Clock.systemDefaultZone()).minusMinutes(RATE_LIMIT_MINUTES);
        if (requestContactRepository.existsRecentByTaxId(taxId, since)) {
            throw new RequestRateLimitException(
                    "Ya se envió una solicitud para este RUC en los últimos " + RATE_LIMIT_MINUTES + " minutos");
        }
        if (requestContactRepository.countByTaxId(taxId) >= MAX_REQUESTS_PER_TAX_ID) {
            throw new RequestRateLimitException(
                    "Se alcanzó el máximo de " + MAX_REQUESTS_PER_TAX_ID + " solicitudes para este RUC");
        }
    }

    @Override
    public PaginatedResult<RequestResult> listAll(String status, String type, String search, int page, int size) {
        Request.Status statusFilter = status != null && !status.isBlank() ? Request.Status.valueOf(status.trim().toUpperCase()) : null;
        Request.Type typeFilter = type != null && !type.isBlank() ? Request.Type.valueOf(type.trim().toUpperCase()) : null;

        List<Request> requests = requestRepository.findByFilters(statusFilter, typeFilter, search, page, size);
        long total = requestRepository.countByFilters(statusFilter, typeFilter, search);

        List<RequestResult> results = requests.stream()
                .map(r -> toResult(r, requestContactRepository.findByRequestId(r.getId()).orElse(null)))
                .toList();

        return PaginatedResult.of(results, page, size, total);
    }

    @Override
    public RequestDetailResult getById(Integer id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
        RequestContact contact = requestContactRepository.findByRequestId(id).orElse(null);

        List<RequestCommentResult> comments = requestCommentRepository.findByRequestId(id).stream()
                .map(c -> new RequestCommentResult(
                        c.getId(), c.getComment(),
                        userRequestCommentRepository.findByRequestCommentId(c.getId())
                                .map(UserRequestComment::getUserId).orElse(null),
                        c.getCreatedAt()))
                .toList();

        List<RequestRecordResult> history = requestRecordRepository.findByRequestId(id).stream()
                .map(r -> new RequestRecordResult(r.getId(), r.getStatus(), r.getUserId(), r.getChangeReason(), r.getCreatedAt()))
                .toList();

        return new RequestDetailResult(
                request.getId(), request.getType().name(), request.getTitle(), request.getMessage(),
                request.getStatus().name(), request.getCreatedAt(), request.getUpdatedAt(),
                toContactResult(contact), comments, history);
    }

    @Override
    public RequestResult changeStatus(ChangeRequestStatusCommand command) {
        Request request = requestRepository.findById(command.requestId())
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + command.requestId()));

        Request.Status newStatus = Request.Status.valueOf(command.status().trim().toUpperCase());
        request.transition(newStatus);
        requestRepository.save(request);

        requestRecordRepository.save(RequestRecord.create(
                request.getId(), newStatus.name(), command.adminUserId(), command.comment()));

        if (command.comment() != null && !command.comment().isBlank()) {
            addCommentInternal(request.getId(), command.adminUserId(), command.comment());
        }

        RequestContact contact = requestContactRepository.findByRequestId(request.getId()).orElse(null);
        return toResult(request, contact);
    }

    @Override
    public RequestCommentResult addComment(AddCommentCommand command) {
        RequestComments comment = addCommentInternal(command.requestId(), command.adminUserId(), command.comment());
        return new RequestCommentResult(comment.getId(), comment.getComment(), command.adminUserId(), comment.getCreatedAt());
    }

    private RequestComments addCommentInternal(Integer requestId, String adminUserId, String commentText) {
        RequestContact contact = requestContactRepository.findByRequestId(requestId).orElse(null);
        Integer contactId = contact != null ? contact.getRequestId() : null;

        RequestComments comment = requestCommentRepository.save(RequestComments.create(requestId, contactId, commentText));
        userRequestCommentRepository.save(UserRequestComment.create(adminUserId, comment.getId()));

        if (contact != null) {
            emailService.send(contact.getEmail(),
                    "Nuevo comentario en tu solicitud",
                    "Se agregó un comentario a tu solicitud:<br>" + escapeHtml(commentText));
        }

        return comment;
    }

    private RequestResult toResult(Request request, RequestContact contact) {
        return new RequestResult(
                request.getId(), request.getType().name(), request.getTitle(), request.getMessage(),
                request.getStatus().name(), request.getCreatedAt(), request.getUpdatedAt(),
                toContactResult(contact));
    }

    private RequestContactResult toContactResult(RequestContact contact) {
        if (contact == null) {
            return null;
        }
        return new RequestContactResult(contact.getName(), contact.getLastName(), contact.getEmail(),
                contact.getPhoneNumber(), contact.getTaxId(), contact.getCompanyName());
    }

    // These emails are built as raw HTML (see ResendEmailAdapter), and the interpolated fields
    // come from the public, unauthenticated /requests submit form — without escaping, an
    // attacker-supplied name/message could inject markup (fake links, iframes) into an email
    // read by Trazzo staff or the requester.
    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }
}

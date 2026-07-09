package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.AddCommentCommand;
import trazzo.back.saasglobal.application.dto.command.ChangeRequestStatusCommand;
import trazzo.back.saasglobal.application.dto.command.SubmitRequestCommand;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.RequestCommentResult;
import trazzo.back.saasglobal.application.dto.result.RequestDetailResult;
import trazzo.back.saasglobal.application.dto.result.RequestResult;
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

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock RequestRepositoryPort requestRepository;
    @Mock RequestContactRepositoryPort requestContactRepository;
    @Mock RequestCommentRepositoryPort requestCommentRepository;
    @Mock UserRequestCommentRepositoryPort userRequestCommentRepository;
    @Mock RequestRecordRepositoryPort requestRecordRepository;
    @Mock EmailService emailService;

    @InjectMocks RequestService service;

    private static SubmitRequestCommand submitCommand() {
        return new SubmitRequestCommand("trial", "Ana", "Perez", "ana@example.com",
                "999999999", "20123456789", "Acme SAC", "Quiero una demo");
    }

    private static Request request(int id, Request.Status status) {
        var now = LocalDateTime.now();
        return Request.restore(id, Request.Type.TRIAL, "Solicitud de trial - Acme SAC",
                "Quiero una demo", status, now, now);
    }

    private static RequestContact contact(int requestId) {
        var now = LocalDateTime.now();
        return RequestContact.restore(requestId, "Ana", "Perez", "ana@example.com",
                "999999999", "20123456789", "Acme SAC", now, now);
    }

    @Test
    void submit_savesRequestAndContactAndNotifies() {
        when(requestContactRepository.existsRecentByTaxId(anyString(), any())).thenReturn(false);
        when(requestContactRepository.countByTaxId(anyString())).thenReturn(0L);
        when(requestRepository.save(any())).thenAnswer(inv -> {
            Request r = inv.getArgument(0);
            return Request.restore(1, r.getType(), r.getTitle(), r.getMessage(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
        });
        when(requestContactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequestResult result = service.submit(submitCommand());

        assertEquals(1, result.id());
        assertEquals("TRIAL", result.type());
        assertEquals("ana@example.com", result.contact().email());
        verify(emailService).send(any(), anyString(), anyString());
    }

    @Test
    void submit_withInfoType_buildsTitleWithInfoWording() {
        when(requestContactRepository.existsRecentByTaxId(anyString(), any())).thenReturn(false);
        when(requestContactRepository.countByTaxId(anyString())).thenReturn(0L);
        when(requestRepository.save(any())).thenAnswer(inv -> {
            Request r = inv.getArgument(0);
            return Request.restore(1, r.getType(), r.getTitle(), r.getMessage(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
        });
        when(requestContactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new SubmitRequestCommand("info", "Ana", "Perez", "ana@example.com",
                "999999999", "20123456789", "Acme SAC", "Quiero más información");

        RequestResult result = service.submit(command);

        assertEquals("INFO", result.type());
        assertTrue(result.title().contains("más información"));
    }

    @Test
    void submit_withNullTaxId_treatsAsEmptyStringForRateLimitChecks() {
        when(requestContactRepository.existsRecentByTaxId(eq(""), any())).thenReturn(false);
        when(requestContactRepository.countByTaxId(eq(""))).thenReturn(0L);
        when(requestRepository.save(any())).thenAnswer(inv -> {
            Request r = inv.getArgument(0);
            return Request.restore(1, r.getType(), r.getTitle(), r.getMessage(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
        });

        var command = new SubmitRequestCommand("trial", "Ana", "Perez", "ana@example.com",
                "999999999", null, "Acme SAC", "Quiero una demo");

        // An empty taxId still passes the rate-limit checks below but is rejected by the domain
        // (RequestContact requires a non-blank taxId), so submit ultimately throws; the point of
        // this test is to exercise the null -> "" branch used for the rate-limit lookups.
        assertThrows(IllegalArgumentException.class, () -> service.submit(command));
        verify(requestContactRepository).existsRecentByTaxId(eq(""), any());
    }

    @Test
    void submit_throwsRateLimitWhenRecentRequestExistsForSameTaxId() {
        when(requestContactRepository.existsRecentByTaxId(anyString(), any())).thenReturn(true);

        assertThrows(RequestRateLimitException.class, () -> service.submit(submitCommand()));
        verifyNoInteractions(requestRepository, emailService);
    }

    @Test
    void submit_throwsRateLimitWhenMaxRequestsPerTaxIdReached() {
        when(requestContactRepository.existsRecentByTaxId(anyString(), any())).thenReturn(false);
        when(requestContactRepository.countByTaxId(anyString())).thenReturn(2L);

        assertThrows(RequestRateLimitException.class, () -> service.submit(submitCommand()));
        verifyNoInteractions(requestRepository, emailService);
    }

    @Test
    void listAll_returnsPaginatedResults() {
        when(requestRepository.findByFilters(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(request(1, Request.Status.PENDING)));
        when(requestRepository.countByFilters(any(), any(), any())).thenReturn(1L);
        when(requestContactRepository.findByRequestId(1)).thenReturn(Optional.of(contact(1)));

        PaginatedResult<RequestResult> result = service.listAll(null, null, null, 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(1L, result.totalElements());
    }

    @Test
    void listAll_withStatusAndTypeFilters_parsesEnumsFromStrings() {
        when(requestRepository.findByFilters(eq(Request.Status.APPROVED), eq(Request.Type.TRIAL), any(), anyInt(), anyInt()))
                .thenReturn(List.of(request(1, Request.Status.APPROVED)));
        when(requestRepository.countByFilters(eq(Request.Status.APPROVED), eq(Request.Type.TRIAL), any())).thenReturn(1L);
        when(requestContactRepository.findByRequestId(1)).thenReturn(Optional.of(contact(1)));

        PaginatedResult<RequestResult> result = service.listAll("approved", "trial", "acme", 0, 20);

        assertEquals(1, result.content().size());
    }

    @Test
    void listAll_withBlankStatusAndType_treatsAsNoFilter() {
        when(requestRepository.findByFilters(isNull(), isNull(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(requestRepository.countByFilters(isNull(), isNull(), any())).thenReturn(0L);

        PaginatedResult<RequestResult> result = service.listAll("  ", "  ", null, 0, 20);

        assertTrue(result.content().isEmpty());
    }

    @Test
    void getById_returnsNullContactWhenContactMissing() {
        when(requestRepository.findById(1)).thenReturn(Optional.of(request(1, Request.Status.PENDING)));
        when(requestContactRepository.findByRequestId(1)).thenReturn(Optional.empty());
        when(requestCommentRepository.findByRequestId(1)).thenReturn(List.of());
        when(requestRecordRepository.findByRequestId(1)).thenReturn(List.of());

        RequestDetailResult result = service.getById(1);

        assertNull(result.contact());
    }

    @Test
    void getById_returnsDetailWithContactCommentsAndHistory() {
        when(requestRepository.findById(1)).thenReturn(Optional.of(request(1, Request.Status.PENDING)));
        when(requestContactRepository.findByRequestId(1)).thenReturn(Optional.of(contact(1)));
        var comment = RequestComments.restore(10, 1, null, "hola", LocalDateTime.now());
        when(requestCommentRepository.findByRequestId(1)).thenReturn(List.of(comment));
        when(userRequestCommentRepository.findByRequestCommentId(10))
                .thenReturn(Optional.of(UserRequestComment.restore(1, "admin-1", 10, LocalDateTime.now())));
        when(requestRecordRepository.findByRequestId(1)).thenReturn(List.of(
                RequestRecord.restore(1, 1, "PENDING", "admin-1", null, LocalDateTime.now())));

        RequestDetailResult result = service.getById(1);

        assertEquals(1, result.comments().size());
        assertEquals("admin-1", result.comments().get(0).authorUserId());
        assertEquals(1, result.history().size());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(requestRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(99));
    }

    @Test
    void changeStatus_transitionsAndRecordsHistory() {
        when(requestRepository.findById(1)).thenReturn(Optional.of(request(1, Request.Status.PENDING)));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requestContactRepository.findByRequestId(1)).thenReturn(Optional.of(contact(1)));

        var command = new ChangeRequestStatusCommand(1, "APPROVED", "admin-1", null);
        RequestResult result = service.changeStatus(command);

        assertEquals("APPROVED", result.status());
        verify(requestRecordRepository).save(any());
        verifyNoInteractions(requestCommentRepository);
    }

    @Test
    void changeStatus_withCommentAlsoAddsCommentAndNotifiesContact() {
        when(requestRepository.findById(1)).thenReturn(Optional.of(request(1, Request.Status.PENDING)));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requestContactRepository.findByRequestId(1)).thenReturn(Optional.of(contact(1)));
        when(requestCommentRepository.save(any())).thenAnswer(inv -> {
            RequestComments c = inv.getArgument(0);
            return RequestComments.restore(5, c.getRequestId(), c.getRequestContactId(), c.getComment(), c.getCreatedAt());
        });

        var command = new ChangeRequestStatusCommand(1, "IN_REVIEW", "admin-1", "Falta información");
        service.changeStatus(command);

        verify(requestCommentRepository).save(any());
        verify(userRequestCommentRepository).save(any());
        verify(emailService).send(org.mockito.ArgumentMatchers.eq("ana@example.com"), anyString(), anyString());
    }

    @Test
    void changeStatus_withBlankCommentDoesNotAddComment() {
        when(requestRepository.findById(1)).thenReturn(Optional.of(request(1, Request.Status.PENDING)));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requestContactRepository.findByRequestId(1)).thenReturn(Optional.of(contact(1)));

        var command = new ChangeRequestStatusCommand(1, "APPROVED", "admin-1", "   ");
        service.changeStatus(command);

        verifyNoInteractions(requestCommentRepository);
    }

    @Test
    void changeStatus_throwsWhenNotFound() {
        when(requestRepository.findById(99)).thenReturn(Optional.empty());

        var command = new ChangeRequestStatusCommand(99, "APPROVED", "admin-1", null);
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(command));
    }

    @Test
    void addComment_savesCommentAndAuthorAndNotifiesContact() {
        when(requestCommentRepository.save(any())).thenAnswer(inv -> {
            RequestComments c = inv.getArgument(0);
            return RequestComments.restore(7, c.getRequestId(), c.getRequestContactId(), c.getComment(), c.getCreatedAt());
        });
        when(requestContactRepository.findByRequestId(1)).thenReturn(Optional.of(contact(1)));

        var command = new AddCommentCommand(1, "admin-1", "Todo en orden");
        RequestCommentResult result = service.addComment(command);

        assertEquals("Todo en orden", result.comment());
        assertEquals("admin-1", result.authorUserId());
        verify(userRequestCommentRepository).save(any());
        verify(emailService).send(org.mockito.ArgumentMatchers.eq("ana@example.com"), anyString(), anyString());
    }
}

package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.command.AddCommentCommand;
import trazzo.back.saasglobal.application.dto.command.ChangeRequestStatusCommand;
import trazzo.back.saasglobal.application.dto.command.SubmitRequestCommand;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.RequestCommentResult;
import trazzo.back.saasglobal.application.dto.result.RequestDetailResult;
import trazzo.back.saasglobal.application.dto.result.RequestResult;

public interface RequestUseCase {
    RequestResult submit(SubmitRequestCommand command);
    PaginatedResult<RequestResult> listAll(String status, String type, String search, int page, int size);
    RequestDetailResult getById(Integer id);
    RequestResult changeStatus(ChangeRequestStatusCommand command);
    RequestCommentResult addComment(AddCommentCommand command);
}

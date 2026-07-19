package trazzo.back.saasglobal.application.dto.command;

import java.util.List;

public record AssignSaasUserRolesCommand(String userId, List<Integer> roleIds) {}

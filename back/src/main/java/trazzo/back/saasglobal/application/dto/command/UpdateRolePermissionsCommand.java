package trazzo.back.saasglobal.application.dto.command;

import java.util.List;

public record UpdateRolePermissionsCommand(Integer roleId, List<String> permissions) {}

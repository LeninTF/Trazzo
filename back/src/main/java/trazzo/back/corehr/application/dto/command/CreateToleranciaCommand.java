package trazzo.back.corehr.application.dto.command;

import trazzo.back.corehr.domain.model.ToleranciaType;

public record CreateToleranciaCommand(String name, ToleranciaType type, Integer minutes, String description) {
}

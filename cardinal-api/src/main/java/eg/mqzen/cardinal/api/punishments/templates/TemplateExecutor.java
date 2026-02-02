package eg.mqzen.cardinal.api.punishments.templates;

import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.api.punishments.PunishmentType;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TemplateExecutor {
    CompletableFuture<Punishment<?>> createPunishment(
        Punishable<?> playerId,
        TemplateId templateId,
        PunishmentType type,
        String reason,
        String message,
        Duration duration,
        String issuedBy,
        boolean ipPunishment
    );
    
    CompletableFuture<Void> executeActions(List<TemplateAction> actions, PunishmentContext context);
}
package eg.mqzen.cardinal.api.punishments.templates;

import eg.mqzen.cardinal.api.punishments.Punishable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record PunishmentContext(
    Punishable<?> punishable,
    Optional<String> customReason,
    String issuedBy,
    Map<String, String> variables
) {
    public PunishmentContext {
        Objects.requireNonNull(punishable, "Player ID cannot be null");
        Objects.requireNonNull(customReason, "Custom reason optional cannot be null");
        Objects.requireNonNull(issuedBy, "IssuedBy cannot be null");
        variables = Map.copyOf(Objects.requireNonNull(variables, "Variables cannot be null"));
    }
    
    public static PunishmentContext of(Punishable<?> playerId, String issuedBy) {
        return new PunishmentContext(playerId, Optional.empty(), issuedBy, Map.of());
    }
    
    public static PunishmentContext of(Punishable<?> playerId, String customReason, String issuedBy) {
        return new PunishmentContext(playerId, Optional.of(customReason), issuedBy, Map.of());
    }
    
    public PunishmentContext withVariable(String key, String value) {
        var newVariables = new HashMap<>(variables);
        newVariables.put(key, value);
        return new PunishmentContext(punishable, customReason, issuedBy, newVariables);
    }
}

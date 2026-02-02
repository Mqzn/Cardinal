package eg.mqzen.cardinal.api.punishments.templates;

import eg.mqzen.cardinal.api.punishments.Punishment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public sealed interface TemplateExecutionResult
    permits TemplateExecutionResult.SinglePunishmentResult, TemplateExecutionResult.GroupPunishmentResult {
    
    boolean isSuccess();
    List<Punishment<?>> getPunishments();
    Optional<String> getErrorMessage();
    
    record SinglePunishmentResult(
        Punishment<?> punishment,
        boolean success,
        Optional<String> errorMessage
    ) implements TemplateExecutionResult {
        
        @Override
        public boolean isSuccess() { return success; }
        
        @Override
        public List<Punishment<?>> getPunishments() {
            return success ? List.of(punishment) : List.of();
        }
        
        @Override
        public Optional<String> getErrorMessage() { return errorMessage; }
        
        public static SinglePunishmentResult success(Punishment<?> punishment) {
            return new SinglePunishmentResult(punishment, true, Optional.empty());
        }
        
        public static SinglePunishmentResult failure(String error) {
            return new SinglePunishmentResult(null, false, Optional.of(error));
        }
    }
    
    record GroupPunishmentResult(
        List<Punishment<?>> punishments,
        List<String> errors
    ) implements TemplateExecutionResult {
        
        public GroupPunishmentResult {
            punishments = List.copyOf(Objects.requireNonNull(punishments));
            errors = List.copyOf(Objects.requireNonNull(errors));
        }
        
        @Override
        public boolean isSuccess() { return errors.isEmpty(); }
        
        @Override
        public List<Punishment<?>> getPunishments() { return punishments; }
        
        @Override
        public Optional<String> getErrorMessage() {
            return errors.isEmpty() ? Optional.empty() : Optional.of(String.join("; ", errors));
        }
    }
}
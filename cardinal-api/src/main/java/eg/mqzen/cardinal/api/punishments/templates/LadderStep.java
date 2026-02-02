package eg.mqzen.cardinal.api.punishments.templates;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record LadderStep(
    String name,
    String reason,
    String message,
    Duration duration,
    String permission,
    List<TemplateAction> actions,
    List<String> flags
) {
    public LadderStep {
        Objects.requireNonNull(name, "Ladder step name cannot be null");
        Objects.requireNonNull(reason, "Reason optional cannot be null");
        Objects.requireNonNull(message, "Message optional cannot be null");
        Objects.requireNonNull(duration, "Duration optional cannot be null");
        Objects.requireNonNull(permission, "Permission optional cannot be null");
        actions = List.copyOf(Objects.requireNonNull(actions, "Actions cannot be null"));
        flags = List.copyOf(Objects.requireNonNull(flags, "Flags cannot be null"));
    }
    
    public static Builder builder(String name) {
        return new Builder(name);
    }
    
    public static class Builder {
        private final String name;
        private String reason = null;
        private String message = null;
        private Duration duration = Duration.ZERO;
        private String permission = null;
        private final List<TemplateAction> actions = new ArrayList<>();
        private final List<String> flags = new ArrayList<>();
        
        private Builder(String name) {
            this.name = Objects.requireNonNull(name);
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }
        
        public Builder addAction(TemplateAction action) {
            this.actions.add(Objects.requireNonNull(action));
            return this;
        }
        
        public Builder addFlag(String flag) {
            this.flags.add(Objects.requireNonNull(flag));
            return this;
        }
        
        public LadderStep build() {
            return new LadderStep(name, reason, message, duration, permission, 
                                List.copyOf(actions), List.copyOf(flags));
        }
    }
}
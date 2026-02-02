package eg.mqzen.cardinal.punishments.templates;

import lombok.Getter;
import eg.mqzen.cardinal.api.CardinalProvider;
import eg.mqzen.cardinal.api.punishments.Punishable;
import eg.mqzen.cardinal.api.punishments.PunishmentType;
import eg.mqzen.cardinal.api.punishments.templates.LadderStep;
import eg.mqzen.cardinal.api.punishments.templates.PermissionChecker;
import eg.mqzen.cardinal.api.punishments.templates.PunishmentContext;
import eg.mqzen.cardinal.api.punishments.templates.PunishmentTemplate;
import eg.mqzen.cardinal.api.punishments.templates.TemplateExecutionResult;
import eg.mqzen.cardinal.api.punishments.templates.TemplateExecutor;
import eg.mqzen.cardinal.api.punishments.templates.TemplateId;
import eg.mqzen.cardinal.api.punishments.templates.ValidationResult;
import eg.mqzen.cardinal.api.util.FutureOperation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class PunishmentTemplateGroup implements PunishmentTemplate {
    
    private final TemplateId id;
    private final PunishmentType type;
    // Getters
    @Getter
    private final Map<TemplateId, Double> weights;
    private final LinkedHashMap<String, LadderStep> ladder;
    private final Duration expireLadder;
    private final String requiredPermission;
    private final Map<TemplateId, PunishmentTemplate> childTemplates;
    
    private PunishmentTemplateGroup(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.type = Objects.requireNonNull(builder.type);
        this.weights = Map.copyOf(builder.weights);
        this.ladder = new LinkedHashMap<>(builder.ladder);
        this.expireLadder = Objects.requireNonNull(builder.expireLadder);
        this.requiredPermission = Objects.requireNonNull(builder.requiredPermission);
        this.childTemplates = Map.copyOf(builder.childTemplates);
        
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("Template group must have at least one weighted template");
        }
        
        // Validate all child templates are of the same type
        childTemplates.values().forEach(template -> {
            if (!template.getType().equals(type)) {
                throw new IllegalArgumentException("All templates in group must be of type: " + type.name());
            }
        });
    }
    
    public static Builder builder(TemplateId id, PunishmentType type) {
        return new Builder(id, type);
    }
    
    @Override
    public TemplateId getId() { return id; }
    
    @Override
    public PunishmentType getType() { return type; }
    
    @Override
    public boolean hasPermission(String executor, PermissionChecker permissionChecker) {
        // Check group permission first, then individual template permissions
        boolean hasGroupPermission = this.requiredPermission == null || permissionChecker.hasPermission(executor, requiredPermission);
        
        if (!hasGroupPermission) {
            return false;
        }
        
        // Must have permission for at least one child template
        return childTemplates.values().stream()
            .anyMatch(template -> template.hasPermission(executor, permissionChecker));
    }
    
    @Override
    public FutureOperation<TemplateExecutionResult> execute(PunishmentContext context, TemplateExecutor executor) {
        return calculateLadderPosition(context.punishable())
            .thenCompose(position -> {
                var ladderStep = getLadderStepForPosition(position);
                // Execute individual template based on weights
                // Execute group ladder step
                return ladderStep.map(step -> executeGroupLadderStep(context, executor, step))
                        .orElseGet(() -> executeIndividualTemplate(context, executor).unwrap());
            })
            .onErrorAndReturn(ex -> new TemplateExecutionResult.GroupPunishmentResult(List.of(),
                    List.of("Failed to execute group: " + ex.getMessage())));
    }
    
    private FutureOperation<TemplateExecutionResult> executeIndividualTemplate(PunishmentContext context, TemplateExecutor executor) {
        // For now, execute the first available template
        // In a real implementation, you might want more sophisticated selection logic
        var firstTemplate = childTemplates.values().iterator().next();
        return firstTemplate.execute(context, executor);
    }
    
    private CompletableFuture<TemplateExecutionResult> executeGroupLadderStep(PunishmentContext context, TemplateExecutor executor,
            LadderStep step) {
        return executor.executeActions(step.actions(), context)
                .thenApply(results -> new TemplateExecutionResult.GroupPunishmentResult(List.of(), List.of()));
    }
    
    @Override
    public FutureOperation<Double> calculateLadderPosition(Punishable<?> playerId) {
        var futures = weights.entrySet().stream()
            .collect(HashMap<TemplateId, CompletableFuture<Integer>>::new,
                    (map, entry) ->
                            map.put(entry.getKey(),
                            CardinalProvider.provide().getPunishmentManager()
                                .getHistoryService().getRecentPunishments(Duration.ofDays(3), -1).unwrap()
                                    .thenApply(Deque::size)),
                    HashMap::putAll);
        
        return FutureOperation.of(
                CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    double sum = futures.entrySet().stream()
                        .mapToDouble(entry -> entry.getValue().join() * weights.get(entry.getKey()))
                        .sum();
                    return sum + 1.0; // Add 1 as per specification
                })
        );
    }
    
    @Override
    public FutureOperation<Optional<LadderStep>> getEffectiveLadderStep(Punishable<?> target) {
        return calculateLadderPosition(target)
            .thenApply(this::getLadderStepForPosition);
    }
    
    private Optional<LadderStep> getLadderStepForPosition(double position) {
        var steps = ladder.values().stream().toList();
        if (steps.isEmpty()) {
            return Optional.empty();
        }
        
        int stepIndex = (int) Math.min(position - 1, steps.size() - 1);
        return stepIndex >= 0 ? Optional.of(steps.get(stepIndex)) : Optional.empty();
    }
    
    @Override
    public ValidationResult validate() {
        var errors = new ArrayList<String>();
        
        if (weights.isEmpty()) {
            errors.add("Template group must have at least one weighted template");
        }
        
        // Validate all child templates
        for (var template : childTemplates.values()) {
            var result = template.validate();
            if (result instanceof ValidationResult.ValidationFailure(List<String> errors1)) {
                errors.addAll(errors1);
            }
        }
        
        return errors.isEmpty() ? new ValidationResult.ValidationSuccess() 
                               : new ValidationResult.ValidationFailure(errors);
    }
    
    @Override
    public boolean isIpTemplate() {
        // Group is IP template if any child is IP template
        return childTemplates.values().stream().anyMatch(PunishmentTemplate::isIpTemplate);
    }
    
    @Override
    public List<PunishmentTemplate> getChildTemplates() {
        return List.copyOf(childTemplates.values());
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    public Map<String, LadderStep> getLadder() { return Map.copyOf(ladder); }
    public Optional<Duration> getExpireLadder() { return Optional.ofNullable(expireLadder); }
    public Optional<String> getRequiredPermission() { return Optional.ofNullable(requiredPermission); }
    
    public static class Builder {
        private final TemplateId id;
        private final PunishmentType type;
        private final Map<TemplateId, Double> weights = new HashMap<>();
        private final LinkedHashMap<String, LadderStep> ladder = new LinkedHashMap<>();
        private Duration expireLadder = Duration.ZERO;
        private String requiredPermission = null;
        private final Map<TemplateId, PunishmentTemplate> childTemplates = new HashMap<>();
        
        private Builder(TemplateId id, PunishmentType type) {
            this.id = id;
            this.type = type;
        }
        
        public Builder addTemplate(PunishmentTemplate template, double weight) {
            Objects.requireNonNull(template);
            if (weight < 0) {
                throw new IllegalArgumentException("Weight cannot be negative");
            }
            if (!template.getType().equals(type)) {
                throw new IllegalArgumentException("Template type must match group type: " + type.name());
            }
            
            this.weights.put(template.getId(), weight);
            this.childTemplates.put(template.getId(), template);
            return this;
        }
        
        public Builder addLadderStep(LadderStep step) {
            this.ladder.put(step.name(), step);
            return this;
        }
        
        public Builder expireLadder(Duration duration) {
            this.expireLadder = duration;
            return this;
        }
        
        public Builder requiredPermission(String permission) {
            this.requiredPermission = permission;
            return this;
        }
        
        public PunishmentTemplateGroup build() {
            return new PunishmentTemplateGroup(this);
        }
    }
}

package net.mineabyss.core.punishments.templates;

import lombok.Getter;
import net.mineabyss.cardinal.api.CardinalProvider;
import net.mineabyss.cardinal.api.punishments.Punishable;
import net.mineabyss.cardinal.api.punishments.PunishmentType;
import net.mineabyss.cardinal.api.punishments.templates.LadderStep;
import net.mineabyss.cardinal.api.punishments.templates.PermissionChecker;
import net.mineabyss.cardinal.api.punishments.templates.PunishmentContext;
import net.mineabyss.cardinal.api.punishments.templates.PunishmentTemplate;
import net.mineabyss.cardinal.api.punishments.templates.TemplateAction;
import net.mineabyss.cardinal.api.punishments.templates.TemplateExecutionResult;
import net.mineabyss.cardinal.api.punishments.templates.TemplateExecutor;
import net.mineabyss.cardinal.api.punishments.templates.TemplateId;
import net.mineabyss.cardinal.api.punishments.templates.ValidationResult;
import net.mineabyss.cardinal.api.util.FutureOperation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SinglePunishmentTemplate implements PunishmentTemplate {
    
    private final TemplateId id;
    private final PunishmentType type;
    // Getters for builder pattern
    @Getter private final String defaultReason;
    @Getter private final String defaultMessage;
    private final Duration defaultDuration;
    private final String requiredPermission;
    @Getter private final List<TemplateAction> defaultActions;
    @Getter private final List<String> defaultFlags;
    private final LinkedHashMap<String, LadderStep> ladder;
    @Getter private final Duration expireLadder;
    private final boolean ipTemplate;
    
    private SinglePunishmentTemplate(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.type = Objects.requireNonNull(builder.type);
        this.defaultReason = Objects.requireNonNull(builder.defaultReason);
        this.defaultMessage = Objects.requireNonNull(builder.defaultMessage);
        this.defaultDuration = Objects.requireNonNull(builder.defaultDuration);
        this.requiredPermission = Objects.requireNonNull(builder.requiredPermission);
        this.defaultActions = List.copyOf(builder.defaultActions);
        this.defaultFlags = List.copyOf(builder.defaultFlags);
        this.ladder = new LinkedHashMap<>(builder.ladder);
        this.expireLadder = Objects.requireNonNull(builder.expireLadder);
        this.ipTemplate = builder.ipTemplate;
        
        if (!type.supportsDuration() &&  (!defaultDuration.isZero() && !defaultDuration.isNegative()) ) {
            throw new IllegalArgumentException("Punishment type " + type.name() + " does not support duration");
        }
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
        return requiredPermission == null || permissionChecker.hasPermission(executor, requiredPermission);
    }

    @Override
    public FutureOperation<TemplateExecutionResult> execute(PunishmentContext context, TemplateExecutor executor) {
        return getEffectiveLadderStep(context.punishable())
                .thenCompose(ladderStep -> {
                    // Build the effective punishment configuration
                    var effectiveReason = context.customReason()
                            .or(() -> ladderStep.map(LadderStep::reason))
                            .orElse(defaultReason);

                    var effectiveMessage = ladderStep.map(LadderStep::message)
                            .orElse(defaultMessage);

                    var effectiveDuration = ladderStep.map(LadderStep::duration)
                            .orElse(defaultDuration);

                    var effectiveActions = new ArrayList<>(defaultActions);
                    ladderStep.ifPresent(step -> effectiveActions.addAll(step.actions()));

                    /*  var effectiveFlags = new ArrayList<>(defaultFlags);
                        ladderStep.ifPresent(step -> effectiveFlags.addAll(step.flags()));
                    */
                    // Create punishment record
                    return executor.createPunishment(
                            context.punishable(),
                            id,
                            type,
                            effectiveReason,
                            effectiveMessage,
                            effectiveDuration,
                            context.issuedBy(),
                            ipTemplate
                    ).thenCompose(punishment -> {
                        // Execute actions
                        return executor.executeActions(effectiveActions, context)
                                .thenApply(actionResults -> (TemplateExecutionResult) TemplateExecutionResult.SinglePunishmentResult.success(punishment))
                                .exceptionally(ex -> TemplateExecutionResult.SinglePunishmentResult.failure("Failed to execute actions: " + ex.getMessage()));
                    });
                });
    }
    
    @Override
    public FutureOperation<Double> calculateLadderPosition(Punishable<?> target) {
        return CardinalProvider.provide().getPunishmentManager().getHistoryService()
                .getPunishmentHistory(target, id)
            .thenApply(history -> {
                if (expireLadder.isZero()) {
                    return (double) history.size();
                }
                
                var cutoffTime = Instant.now().minus(expireLadder);
                long validPunishments = history.stream()
                    .filter(p -> p.getIssuedAt().isAfter(cutoffTime))
                    .count();
                
                return (double) validPunishments;
            });
    }
    
    @Override
    public FutureOperation<Optional<LadderStep>> getEffectiveLadderStep(Punishable<?> target) {
        return calculateLadderPosition(target)
            .thenApply(position -> {
                var steps = ladder.values().stream().toList();
                if (steps.isEmpty()) {
                    return Optional.<LadderStep>empty();
                }
                
                int stepIndex = (int) Math.min(position, steps.size() - 1);
                return Optional.of(steps.get(stepIndex));
            });
    }
    
    @Override
    public ValidationResult validate() {
        var errors = new ArrayList<String>();
        
        if (defaultReason.isBlank()) {
            errors.add("Default reason cannot be blank");
        }
        
        if (defaultMessage.isBlank()) {
            errors.add("Default message cannot be blank");
        }
        
        if (!type.supportsDuration() && (!defaultDuration.isZero() && !defaultDuration.isNegative()) ) {
            errors.add("Punishment type " + type.name() + " does not support duration");
        }
        
        return errors.isEmpty() ? new ValidationResult.ValidationSuccess() 
                               : new ValidationResult.ValidationFailure(errors);
    }
    
    @Override
    public boolean isIpTemplate() { return ipTemplate; }
    
    @Override
    public List<PunishmentTemplate> getChildTemplates() { return List.of(this); }

    @Override
    public boolean isGroup() {
        return false;
    }

    public Optional<Duration> getDefaultDuration() { return Optional.ofNullable(defaultDuration); }
    public Optional<String> getRequiredPermission() { return Optional.ofNullable(requiredPermission); }

    public Map<String, LadderStep> getLadder() { return Map.copyOf(ladder); }

    public static class Builder {
        private final TemplateId id;
        private final PunishmentType type;
        private String defaultReason = "";
        private String defaultMessage = "";
        private Duration defaultDuration = Duration.ZERO;
        private String requiredPermission = null;
        private final List<TemplateAction> defaultActions = new ArrayList<>();
        private final List<String> defaultFlags = new ArrayList<>();
        private final LinkedHashMap<String, LadderStep> ladder = new LinkedHashMap<>();
        private Duration expireLadder = Duration.ZERO;
        private boolean ipTemplate = false;
        
        private Builder(TemplateId id, PunishmentType type) {
            this.id = id;
            this.type = type;
        }
        
        public Builder defaultReason(String reason) {
            this.defaultReason = Objects.requireNonNull(reason);
            return this;
        }
        
        public Builder defaultMessage(String message) {
            this.defaultMessage = Objects.requireNonNull(message);
            return this;
        }
        
        public Builder defaultDuration(Duration duration) {
            this.defaultDuration = duration;
            return this;
        }
        
        public Builder requiredPermission(String permission) {
            this.requiredPermission = permission;
            return this;
        }
        
        public Builder addDefaultAction(TemplateAction action) {
            this.defaultActions.add(Objects.requireNonNull(action));
            return this;
        }
        
        public Builder addDefaultFlag(String flag) {
            this.defaultFlags.add(Objects.requireNonNull(flag));
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
        
        public Builder ipTemplate(boolean ipTemplate) {
            this.ipTemplate = ipTemplate;
            return this;
        }
        
        public SinglePunishmentTemplate build() {
            return new SinglePunishmentTemplate(this);
        }
    }
}

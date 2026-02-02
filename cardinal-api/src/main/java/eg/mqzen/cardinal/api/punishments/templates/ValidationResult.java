package eg.mqzen.cardinal.api.punishments.templates;

import java.util.List;
import java.util.Objects;

public sealed interface ValidationResult permits ValidationResult.ValidationSuccess, ValidationResult.ValidationFailure {
    record ValidationSuccess() implements ValidationResult {}
    record ValidationFailure(List<String> errors) implements ValidationResult {
        public ValidationFailure {
            errors = List.copyOf(Objects.requireNonNull(errors));
        }
    }
}

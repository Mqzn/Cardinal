package net.mineabyss.cardinal.api.punishments.templates;

import java.util.Objects;

public record TemplateId(String value) {

    public TemplateId {
        Objects.requireNonNull(value, "Template ID cannot be null");
        if (value.isBlank() || value.contains(".") || value.contains(" ")) {
            throw new IllegalArgumentException("Template ID cannot be blank or contain dots/spaces");
        }
    }

}
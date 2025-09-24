package org.example.model;

import java.time.LocalDate;

public record Patient(
        String name,
        LocalDate birthday,
        String contact
) {
}

package org.example.model;

public record MedicalHistory(
        Integer patientId,
        String diagnosis,
        String treatment,
        String prescriptions
) {
}

package org.example.model;

public record Appointment(
        String appointmentDateTime,
        Integer pacientId,
        Integer doctorId
) {
}

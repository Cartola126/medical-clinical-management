drop table patients cascade;
drop table doctors cascade;
drop table appointments cascade;
drop table medicalhistory cascade;
select * from appointments;
select * from doctors;
select * from patients;
select * from medicalhistory;

-- DATABASE

CREATE TYPE appointment_status AS ENUM (
    'Scheduled',
    'Confirmed',
    'Completed',
    'Cancelled',
    'NoShow'
);

CREATE TABLE Patients (
                          id SERIAL PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          birthday DATE NOT NULL,
                          contact VARCHAR(50)
);

CREATE TABLE Doctors (
                         id SERIAL PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         specialty VARCHAR(100) NOT NULL,
                         office_hours VARCHAR(50)
);

CREATE TABLE Appointments (
                              id SERIAL PRIMARY KEY,
                              appointment_datetime TIMESTAMP NOT NULL,
                              patient_id INT NOT NULL,
                              doctor_id INT NOT NULL,
                              status appointment_status DEFAULT 'Scheduled',
                              FOREIGN KEY (patient_id) REFERENCES Patients(id),
                              FOREIGN KEY (doctor_id) REFERENCES Doctors(id)
);

CREATE TABLE MedicalHistory (
                                id SERIAL PRIMARY KEY,
                                patient_id INT NOT NULL,
                                diagnosis TEXT NOT NULL,
                                prescriptions TEXT,
                                FOREIGN KEY (patient_id) REFERENCES Patients(id)
);

-- INSERT PROCEDURES

CREATE OR REPLACE PROCEDURE CreatePatient_Proc(
    p_name VARCHAR(100),
    p_birthday DATE,
    p_contact VARCHAR(50)
)
LANGUAGE plpgsql
AS $$
BEGIN
INSERT INTO Patients (name, birthday, contact)
VALUES (p_name, p_birthday, p_contact);

RAISE NOTICE 'Patient "%" created successfully.', p_name;
END;
$$;

CREATE OR REPLACE PROCEDURE CreateDoctor_Proc(
    p_name VARCHAR(100),
    p_specialty VARCHAR(100),
    p_office_hours VARCHAR(50)
)
LANGUAGE plpgsql
AS $$
BEGIN
INSERT INTO Doctors (name, specialty, office_hours)
VALUES (p_name, p_specialty, p_office_hours);

RAISE NOTICE 'Doctor "%" created successfully.', p_name;
END;
$$;

CREATE OR REPLACE PROCEDURE CreateMedicalHistory_Proc(
    p_patient_id INT,
    p_diagnosis TEXT,
    p_treatment TEXT,
    p_prescriptions TEXT
)
LANGUAGE plpgsql
AS $$
BEGIN
INSERT INTO MedicalHistory (patient_id, diagnosis, treatment, prescriptions)
VALUES (p_patient_id, p_diagnosis, p_treatment, p_prescriptions);

RAISE NOTICE 'New medical history record added for patient %.', p_patient_id;
END;
$$;

CREATE OR REPLACE PROCEDURE ScheduleAppointment_Proc(
    p_patient_id INT,
    p_doctor_id INT,
    p_datetime TIMESTAMP
)
LANGUAGE plpgsql
AS $$
DECLARE
v_exists INT;
BEGIN
SELECT COUNT(*) INTO v_exists
FROM Appointments
WHERE doctor_id = p_doctor_id
  AND appointment_datetime = p_datetime
  AND status IN ('Scheduled', 'Confirmed');

IF v_exists > 0 THEN
        RAISE NOTICE 'Error: The doctor already has an appointment scheduled for this time.';
        RETURN;
END IF;

INSERT INTO Appointments (appointment_datetime, patient_id, doctor_id, status)
VALUES (p_datetime, p_patient_id, p_doctor_id, 'Scheduled');

RAISE NOTICE 'Appointment scheduled successfully!';
END;
$$;

-- UPDATE PROCEDURES

CREATE OR REPLACE PROCEDURE CancelAppointment_Proc(
    p_appointment_id INT
)
LANGUAGE plpgsql
AS $$
DECLARE
v_current_status appointment_status;
BEGIN
SELECT status INTO v_current_status
FROM Appointments
WHERE id = p_appointment_id;

IF NOT FOUND THEN
        RAISE NOTICE 'Error: Appointment not found.';
        RETURN;
END IF;

    IF v_current_status IN ('Scheduled', 'Confirmed') THEN
UPDATE Appointments
SET status = 'Cancelled'
WHERE id = p_appointment_id;
RAISE NOTICE 'Appointment canceled successfully.';
ELSE
        RAISE NOTICE 'Error: This appointment cannot be canceled (Current status: %)', v_current_status;
END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE UpdateAppointmentStatus_Proc(
    p_appointment_id INT,
    p_new_status TEXT
)
LANGUAGE plpgsql
AS $$
BEGIN
UPDATE Appointments
SET status = p_new_status::appointment_status
WHERE id = p_appointment_id;

IF NOT FOUND THEN
        RAISE NOTICE 'Error: Appointment with ID % not found.', p_appointment_id;
ELSE
        RAISE NOTICE 'Status of appointment % updated to "%".', p_appointment_id, p_new_status;
END IF;

EXCEPTION
    WHEN invalid_text_representation THEN
        RAISE NOTICE 'Error: Invalid status value "%". Please use one of: Scheduled, Confirmed, Completed, Cancelled, NoShow.', p_new_status;
END;
$$;

CREATE OR REPLACE PROCEDURE UpdateMedicalHistory_Proc(
    p_patient_id INT,
    p_diagnosis TEXT,
    p_treatment TEXT,
    p_prescriptions TEXT
)
LANGUAGE plpgsql
AS $$
BEGIN
INSERT INTO MedicalHistory (patient_id, diagnosis, treatment, prescriptions)
VALUES (p_patient_id, p_diagnosis, p_treatment, p_prescriptions);

RAISE NOTICE 'Medical history updated successfully!';
END;
$$;

-- DATA FOR TESTING

CALL CreatePatient_Proc('John Smith', '1985-05-20', '555-0101');
CALL CreatePatient_Proc('Jane Doe', '1992-08-15', '555-0102');
CALL CreateDoctor_Proc('Dr. Alan Grant', 'Cardiology', '09:00-17:00');
CALL CreateDoctor_Proc('Dr. Ellie Sattler', 'Dermatology', '10:00-18:00');

CALL ScheduleAppointment_Proc(1, 1, '2025-12-01 11:00:00');
CALL ScheduleAppointment_Proc(2, 1, '2025-12-01 11:00:00');
CALL CancelAppointment_Proc(1);
CALL CreateAppointment_Proc(1, 1, '2026-02-15 10:00:00');
CALL UpdateAppointmentStatus_Proc(3, 'Confirmed');
CALL UpdateAppointmentStatus_Proc(3, 'Completed');
CALL CreateMedicalHistory_Proc(1, 'Common Cold', 'Rest and fluids', 'None');
CALL UpdateMedicalHistory_Proc(
    1,
    'Seasonal Allergies',
    'Prescribed antihistamines.',
    'Loratadine 10mg, once daily as needed'
);
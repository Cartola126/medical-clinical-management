package org.example;

import net.datafaker.Faker;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class BulkDataInserter {

    private static final int NUM_PATIENTS = 500_000;
    private static final int NUM_DOCTORS = 5_000;
    private static final int NUM_APPOINTMENTS = 1_000_000;
    private static final int NUM_MEDICAL_HISTORIES = 500_000;

    private static final int NUM_THREADS = 6;
    private static final int BATCH_SIZE = 1000;
    private static final Faker FAKER = new Faker();
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {
        DataSource dataSource = DataSourceFactory.getDataSource();
        System.out.println("Iniciando inserção de dados em massa...");
        long totalStartTime = System.nanoTime();

        System.out.println("1. Inserindo Pacientes e Doutores...");
        ExecutorService initialExecutor = Executors.newFixedThreadPool(2);
        Future<?> patientsFuture = initialExecutor.submit(() -> insertData("Patients", NUM_PATIENTS));
        Future<?> doctorsFuture = initialExecutor.submit(() -> insertData("Doctors", NUM_DOCTORS));

        patientsFuture.get();
        doctorsFuture.get();
        initialExecutor.shutdown();

        long totalEndTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toSeconds(totalEndTime - totalStartTime);
        System.out.printf("\nProcesso concluído com sucesso em %d segundos.\n", duration);
    }

    private static void insertData(String tableName, int totalRecords) {
        DataSource dataSource = DataSourceFactory.getDataSource();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        int recordsPerThread = totalRecords / NUM_THREADS;

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                String sql;
                if ("Patients".equals(tableName)) {
                    sql = "INSERT INTO Patients (name, birthday, contact) VALUES (?, ?, ?)";
                } else {
                    sql = "INSERT INTO Doctors (name, specialty, office_hours) VALUES (?, ?, ?)";
                }

                try (Connection conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (int j = 0; j < recordsPerThread; j++) {
                            if ("Patients".equals(tableName)) {
                                ps.setString(1, FAKER.name().fullName());
                                ps.setDate(2, Date.valueOf(FAKER.timeAndDate().birthday(18, 90)));
                                ps.setString(3, FAKER.phoneNumber().cellPhone());
                            } else {
                                ps.setString(1, "Dr. " + FAKER.name().fullName());
                                ps.setString(2, FAKER.careProvider().medicalProfession());
                                ps.setString(3, "09:00-17:00");
                            }
                            ps.addBatch();
                            if ((j + 1) % BATCH_SIZE == 0) {
                                ps.executeBatch();
                                conn.commit();
                            }
                        }
                        ps.executeBatch();
                        conn.commit();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("Inserção na tabela %s concluída.\n", tableName);
    }

    private static List<Integer> fetchIds(DataSource dataSource, String tableName) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM " + tableName;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        }
        return ids;
    }

    private static void insertAppointments(int totalRecords, List<Integer> patientIds, List<Integer> doctorIds) {
        DataSource dataSource = DataSourceFactory.getDataSource();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        int recordsPerThread = totalRecords / NUM_THREADS;

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                String sql = "INSERT INTO Appointments (appointment_datetime, patient_id, doctor_id) VALUES (?, ?, ?)";
                try (Connection conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (int j = 0; j < recordsPerThread; j++) {
                            ps.setTimestamp(1, Timestamp.valueOf(FAKER.timeAndDate().future(365, TimeUnit.DAYS, "yyyy-MM-dd hh:mm:ss")));
                            ps.setInt(2, patientIds.get(RANDOM.nextInt(patientIds.size())));
                            ps.setInt(3, doctorIds.get(RANDOM.nextInt(doctorIds.size())));
                            ps.addBatch();
                            if ((j + 1) % BATCH_SIZE == 0) {
                                ps.executeBatch();
                                conn.commit();
                            }
                        }
                        ps.executeBatch();
                        conn.commit();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("   - Inserção na tabela Appointments concluída.");
    }
}
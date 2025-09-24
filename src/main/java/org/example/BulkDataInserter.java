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

        // TODO: Lógica de inserção de dados

        long totalEndTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toSeconds(totalEndTime - totalStartTime);
        System.out.printf("\nProcesso concluído com sucesso em %d segundos.\n", duration);
    }
}
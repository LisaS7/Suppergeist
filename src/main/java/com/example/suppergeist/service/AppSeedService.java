package com.example.suppergeist.service;

import com.example.suppergeist.database.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class AppSeedService {
    private final DatabaseManager dbManager;
    private static final Logger log = Logger.getLogger(AppSeedService.class.getName());
    private static final String CSV_RESOURCE = "/data/ingredient_mapping.csv";

    public AppSeedService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void seedIfEmpty() throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM ingredients LIMIT 1")) {
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                log.info("Ingredients already present, skipping seed.");
                return;
            }

            try (InputStream inputStream = AppSeedService.class.getResourceAsStream(CSV_RESOURCE)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Could not find resource: " + CSV_RESOURCE);
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                     CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);) {

                    List<String> headers = csvParser.getHeaderNames();
                    if (headers.isEmpty()) {
                        throw new IllegalStateException("CSV is empty: " + CSV_RESOURCE);
                    }

                    log.info("Seeding ingredients from CSV...");
                    String columnList = String.join(", ", headers);
                    String placeholders = String.join(", ", Collections.nCopies(headers.size(), "?"));

                    String sql = "INSERT INTO ingredients (" + columnList + ") VALUES (" + placeholders + ")";
                    try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                        for (CSVRecord record : csvParser) {
                            if (record.size() != headers.size()) {
                                throw new IllegalStateException("CSV row does not match header length. Expected " + headers.size() + " but got " + record.size() + ". Row: " + record);
                            }

                            for (int i = 0; i < headers.size(); i++) {
                                String value = record.get(i).trim();
                                insertStmt.setString(i + 1, value.isEmpty() ? null : value);
                            }
                            insertStmt.addBatch();
                        }
                        int[] results = insertStmt.executeBatch();
                        log.info("Inserted " + results.length + " ingredients.");
                    }
                }
            }
        }
    }
}

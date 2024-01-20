package com.loader.csv;

import com.loader.database.ConnectionManager;
import com.loader.database.TableMetadata;
import com.loader.util.DataFormatter;
import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVParser {
    private final String csvFilePath;
    private final String columnSeparator;
    private final String enclosureCharacter;
    private final TableMetadata tableMetadata;
    private final DataFormatter dataFormatter;
    private final int sampleSize;
    private final String dataDetectionMode;
    private final ConnectionManager connectionManager;
    private final Charset csvCharset; // Charset do CSV
    private BufferedWriter badFileWriter; // Adicionado para escrever no arquivo .bad
    private BufferedReader br;// Leitor do arquivo CSV
    private int totalRows;
    private File csvFile; // Defina csvFile como um membro da classe
    private boolean headerProcessed;

    public CSVParser(String csvFilePath, String columnSeparator, String enclosureCharacter, TableMetadata tableMetadata, DataFormatter dataFormatter, int sampleSize, String dataDetectionMode, ConnectionManager connectionManager, String csvCharset) throws IOException {
        this.csvFilePath = null;
		this.columnSeparator = columnSeparator;
        this.enclosureCharacter = enclosureCharacter;
        this.tableMetadata = tableMetadata;
        this.dataFormatter = dataFormatter;
        this.sampleSize = sampleSize;
        this.dataDetectionMode = dataDetectionMode;
        this.connectionManager = connectionManager;
        this.csvCharset = Charset.forName(csvCharset); // Converter string para Charset
        this.csvFile = new File(csvFilePath);
        this.br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), csvCharset));
        this.totalRows = countTotalRows(csvFilePath);
        if (!this.csvFile.exists()) {
            throw new FileNotFoundException("O arquivo CSV não foi encontrado no caminho especificado: " + csvFilePath);
        }
        this.br = new BufferedReader(new InputStreamReader(new FileInputStream(this.csvFile), csvCharset));
        this.headerProcessed = false;
    }

    public List<String[]> parseCSV() throws IOException, SQLException {
        File csvFile = new File(csvFilePath);
        File badFile = new File(csvFilePath + ".bad");

        if (!csvFile.exists()) {
            System.err.println("CSV file does not exist: " + csvFilePath);
            return new ArrayList<>();
        }

        analyzeSampleForFormat();

        List<String[]> data = new ArrayList<>();
        BufferedWriter bwBad = null;
        try {
            bwBad = new BufferedWriter(new FileWriter(badFile));
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), csvCharset));
            
            boolean isFirstRow = true;
            String line;
            while ((line = br.readLine()) != null) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue; // Skip the first row (header)
                }

                String[] values = parseLine(line);
                if (values != null) {
                    // Tratar os valores com base no tipo de dado
                    for (int i = 0; i < values.length; i++) {
                        Object formattedValue = dataFormatter.formatData(values[i], tableMetadata.getColumns().get(i).getType());
                        if (formattedValue instanceof Double) {
                            // Tratar como número
                            values[i] = String.valueOf(formattedValue);
                        } else {
                            // Tratar como String
                            values[i] = (String) formattedValue;
                        }
                    }
                    /**/System.out.println("Linha processada: " + Arrays.toString(values));
                    data.add(values);
                } else {
                    bwBad.write(line);
                    bwBad.newLine();
                    System.out.println("Linha com erro gravada no .bad: " + line);
                }
            }
            br.close();
        } finally {
            if (bwBad != null) {
                bwBad.close();
            }
        }
        return data;
    }

    private void analyzeSampleForFormat() throws IOException, SQLException {
        if (dataDetectionMode.equals("DirectTable")) {
            analyzeSampleUsingDirectTable();
        } else {
            analyzeSampleUsingMetadata();
        }
    }

    private void analyzeSampleUsingMetadata() throws IOException, SQLException {

        // Lendo uma amostra do arquivo CSV
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFilePath), csvCharset))) {
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null && lineCount < sampleSize) {
                String[] values = parseLine(line);
                if (lineCount == 0) { // Pular a primeira linha (cabeçalho)
                    lineCount++;
                    continue;
                }

                // Iterar sobre cada valor da linha e determinar o formato
                for (int i = 0; i < values.length; i++) {
                    String columnType = tableMetadata.getColumns().get(i).getType();
                    values[i] = formatValueBasedOnMetadata(values[i], columnType);
                }
                lineCount++;
            }
        }
    }
    
    private String formatValueBasedOnMetadata(String value, String columnType) {
        return dataFormatter.formatData(value, columnType);
    }

    private void analyzeSampleUsingDirectTable() throws SQLException, IOException {
        Map<Integer, String> columnFormats = new HashMap<>();

        // Incluindo o esquema no nome da tabela, se disponível
        String schemaTableName = tableMetadata.getSchema() != null ? tableMetadata.getSchema() + "." + tableMetadata.getTableName() : tableMetadata.getTableName();
        String sql = "SELECT * FROM " + schemaTableName + " WHERE ROWNUM <= " + sampleSize;

        try (Connection conn = connectionManager.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columnCount = rsMetaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnTypeName = rsMetaData.getColumnTypeName(i);
                columnFormats.put(i - 1, convertSqlTypeToFormat(columnTypeName));
            }

            // Log dos formatos das colunas para diagnóstico
           /**/columnFormats.forEach((index, format) -> System.out.println("Coluna " + (index + 1) + " formato: " + format));
        }
    }


    private String convertSqlTypeToFormat(String sqlType) {
        switch (sqlType) {
            case "VARCHAR":
            case "CHAR":
                return "STRING";
            case "NUMERIC":
            case "NUMBER":
            case "DECIMAL":
                return "DECIMAL";
            case "DATE":
            case "TIMESTAMP":
                return "DATE";
            // Adicione mais casos conforme necessário
            default:
                return "UNKNOWN";
        }
    }

    
    private String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean insideField = false;

        for (char c : line.toCharArray()) {
            if (c == enclosureCharacter.charAt(0) && !insideField) {
                insideField = true;
                continue;
            }

            if (c == enclosureCharacter.charAt(0) && insideField) {
                insideField = false;
                continue;
            }

            if (c == columnSeparator.charAt(0) && !insideField) {
                fields.add(field.toString());
                field.setLength(0); // Limpa o StringBuilder para o próximo campo
                continue;
            }

            field.append(c);
        }

        if (field.length() > 0) { // Adiciona o último campo
            fields.add(field.toString());
        }

        return fields.toArray(new String[0]);
    }
    
    public void writeBadRecord(String[] badData) throws IOException {
        if (badFileWriter == null) {
            File badFile = new File(csvFilePath + ".bad");
            badFileWriter = new BufferedWriter(new FileWriter(badFile, true));
        }

        String badRecord = String.join(columnSeparator, badData);
        badFileWriter.write(badRecord);
        badFileWriter.newLine();
    }

    public void closeBadFileWriter() throws IOException {
        if (badFileWriter != null) {
            badFileWriter.close();
        }
    }
    
    public List<String[]> parseNextBatch(int batchSize) throws IOException {
        if (br == null) {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), csvCharset));
        }

        // Pula a primeira linha se for o cabeçalho e ainda não tiver sido processado
        if (!headerProcessed) {
            br.readLine();
            headerProcessed = true;
        }

        List<String[]> batchData = new ArrayList<>();
        String line;
        int count = 0;
        while (count < batchSize && (line = br.readLine()) != null) {
            String[] values = parseLine(line);
            if (values != null) {
                batchData.add(values);
                count++;
            }
        }
        return batchData;
    }


    private int countTotalRows(String csvFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            int lines = 0;
            while (reader.readLine() != null) lines++;
            return lines;
        }
    }
    
    public void closeResources() throws IOException {
        if (br != null) {
            br.close();
        }
    }

    public int getTotalRows() {
        return this.totalRows;
    }
}
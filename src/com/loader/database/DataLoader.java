package com.loader.database;

import com.loader.csv.CSVParser;
import com.loader.util.DataFormatter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.StringJoiner;

public class DataLoader {
    private final int batchSize; // Adicione um membro para armazenar batchSize
    private final ConnectionManager connectionManager;
    private final TableMetadata tableMetadata;
    private final DataFormatter dataFormatter;

    public DataLoader(ConnectionManager connectionManager, TableMetadata tableMetadata, DataFormatter dataFormatter, int batchSize) {
        this.connectionManager = connectionManager;
        this.tableMetadata = tableMetadata;
        this.dataFormatter = dataFormatter;
        this.batchSize = batchSize;
    }

    private String generateInsertSql() {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        // Incluindo o esquema na string SQL, se disponível
        if (tableMetadata.getSchema() != null && !tableMetadata.getSchema().isEmpty()) {
            sql.append(tableMetadata.getSchema()).append(".");
        }
        sql.append(tableMetadata.getTableName()).append(" (");

        // Adding column names
        StringJoiner columnNames = new StringJoiner(", ");
        for (TableMetadata.ColumnMetadata column : tableMetadata.getColumns()) {
            columnNames.add(column.getName());
        }
        sql.append(columnNames.toString()).append(") VALUES (");

        // Adding placeholders
        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < tableMetadata.getColumns().size(); i++) {
            placeholders.add("?");
        }
        sql.append(placeholders.toString()).append(")");
        return sql.toString();
    }

    public void loadData(CSVParser parser) throws IOException, SQLException {
        String insertSql = generateInsertSql();
        int totalRows = parser.getTotalRows() - 1;
        int processedRows = 0;

        Connection conn = null;
        try {
            conn = connectionManager.openConnection();
            PreparedStatement pstmt = conn.prepareStatement(insertSql);
            Statement countStatement = conn.createStatement();
            
            int initialCount = countTableRows(countStatement);
            System.out.println("Registros iniciais na tabela: " + initialCount);

            while (processedRows < totalRows) {
                List<String[]> batchData = parser.parseNextBatch(batchSize);
                for (String[] rowData : batchData) {
                    for (int i = 0; i < rowData.length; i++) {
                        String formattedData = dataFormatter.formatData(rowData[i], tableMetadata.getColumns().get(i).getType());
                        pstmt.setString(i + 1, formattedData);
                    }
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                processedRows += batchData.size();
            }
            conn.commit(); // Commit só acontece se todas as linhas forem processadas sem erro
            System.out.println("Total de " + processedRows + " linhas inseridas com sucesso.");
            
            int finalCount = countTableRows(countStatement);
            System.out.println("Quantidade final de registros na tabela: " + finalCount);
            
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Rollback em qualquer exceção
            }
            System.err.println("Erro de SQL: " + e.getMessage());
            System.err.println("Código SQLState: " + e.getSQLState());
            System.err.println("Código de erro do banco de dados: " + e.getErrorCode());
        } finally {
            if (conn != null) {
                conn.close(); // Fecha a conexão no bloco finally
            }
            parser.closeResources(); // Fecha os recursos do parser
        }
    }

    
    private int countTableRows(Statement statement) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ";
        if (tableMetadata.getSchema() != null && !tableMetadata.getSchema().isEmpty()) {
            sql += tableMetadata.getSchema() + ".";
        }
        sql += tableMetadata.getTableName();
        try (ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
}

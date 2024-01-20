package com.loader.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableMetadata {
    private final ConnectionManager connectionManager;
    private final String schema; // Esquema ou owner da tabela
    private final String tableName; // Nome da tabela
    private List<ColumnMetadata> columns;

    public TableMetadata(ConnectionManager connectionManager, String fullTableName) {
        this.connectionManager = connectionManager;
        String[] parts = fullTableName.split("\\.", 2);
        this.schema = parts.length > 1 ? parts[0].toUpperCase() : null;
        this.tableName = parts[parts.length - 1].toUpperCase();
        this.columns = new ArrayList<>();
        loadColumnMetadata();
    }

    private void loadColumnMetadata() {
        try (Connection conn = connectionManager.openConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, schema, tableName, null)) {
            if (!rs.next()) {
                System.err.println("Nenhum metadado encontrado para a tabela: " + schema + "." + tableName);
                return;
            }

            do {
                String columnName = rs.getString("COLUMN_NAME");
                String columnType = rs.getString("TYPE_NAME");
                columns.add(new ColumnMetadata(columnName, columnType));
                System.out.println("Coluna carregada: " + columnName + ", Tipo: " + columnType);
            } while (rs.next());
        } catch (SQLException e) {
            System.err.println("Erro de SQL: " + e.getMessage());
            System.err.println("Código SQLState: " + e.getSQLState());
            System.err.println("Código de erro do banco de dados: " + e.getErrorCode());
	        System.err.println("Erro ao carregar metadados da coluna para a tabela: " + schema + "." + tableName);
        }
    }

    public List<ColumnMetadata> getColumns() {
        return columns;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSchema() {
        return schema;
    }

    public static class ColumnMetadata {
        private final String name;
        private final String type;

        public ColumnMetadata(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }
}


package com.loader.main;

import java.util.Map;

import javax.swing.JOptionPane;

import com.loader.config.ConfigurationManager;
import com.loader.database.ConnectionManager;
import com.loader.database.TableMetadata;
import com.loader.csv.CSVParser;
import com.loader.database.NlsParametersFetcher;
import com.loader.util.DataFormatter;
import com.loader.util.DateUtils;
import com.loader.database.DataLoader;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar loader.jar <Full path to config file>");
            return;
        }

        String configFilePath = args[0];
        try {
            System.out.println(DateUtils.getCurrentTimestamp() + " - Início de execução.");
            
            ConfigurationManager configManager = new ConfigurationManager(configFilePath);

            // Database connection configuration
            String dbUser = System.getenv(configManager.getProperty("DB_USER_ENV"));
            String dbPassword = System.getenv(configManager.getProperty("DB_PASSWORD_ENV"));
            String dbInstance = configManager.getProperty("DB_INSTANCE");
            String dbUrl = "jdbc:oracle:thin:@" + dbInstance;

            if (dbUser == null || dbUser.trim().isEmpty()) {
                System.err.println("Usuário da base de dados não definido nas variáveis de ambiente ou parametrizado incorretamente no config.txt.");
                System.exit(1);
            }    
            if (dbPassword == null || dbPassword.trim().isEmpty()) {
                System.err.println("Senha da base de dados não definida nas variáveis de ambiente ou parametrizada incorretamente no config.txt.");
                System.exit(1);
            }
                       
            System.out.println("DB User: " + dbUser);
            System.out.println("DB Password: [PROTECTED]");
            System.out.println("DB Instance: " + dbInstance);
            System.out.println("DB URL: " + dbUrl);

            // CSV loader configuration
            String targetTable = configManager.getProperty("TARGET_TABLE");
            String csvFilePath = configManager.getProperty("CSV_FILE_PATH");
            String columnSeparator = configManager.getProperty("COLUMN_SEPARATOR");
            String enclosureCharacter = configManager.getProperty("ENCLOSURE_CHARACTER");
            int batchSize = Integer.parseInt(configManager.getProperty("BATCH_SIZE"));
            String dataDetectionMode = configManager.getProperty("DATA_DETECTION_MODE");
            String csvCharset = configManager.getProperty("CHARSET"); // Lê o charset do CSV do arquivo de configuração
            

            System.out.println("Target Table: " + targetTable);
            System.out.println("CSV File Path: " + csvFilePath);
            System.out.println("Column Separator: " + columnSeparator);
            System.out.println("Enclosure Character: " + enclosureCharacter);
            System.out.println("Batch Size: " + batchSize);
            System.out.println("Data Detection Mode: " + dataDetectionMode);

            // Establish database connection
            ConnectionManager connectionManager = new ConnectionManager(dbUrl, dbUser, dbPassword);
            
            TableMetadata tableMetadata = new TableMetadata(connectionManager, targetTable);
            // Fetch NLS session parameters
            NlsParametersFetcher nlsFetcher = new NlsParametersFetcher(connectionManager);
            Map<String, String> nlsParameters = nlsFetcher.fetchNlsParameters();

            // Instantiate DataFormatter with NLS parameters
            DataFormatter dataFormatter = new DataFormatter(nlsParameters);

            // Instantiate CSVParser with the new parameters
            CSVParser parser = new CSVParser(csvFilePath, columnSeparator, enclosureCharacter, tableMetadata, dataFormatter, batchSize, dataDetectionMode, connectionManager, csvCharset);

            // DataLoader
            DataLoader loader = new DataLoader(connectionManager, tableMetadata, dataFormatter, batchSize); // Passando dataFormatter como parâmetro
            loader.loadData(parser);

            System.out.println(DateUtils.getCurrentTimestamp() + " - Fim de execução.");
        } catch (Exception e) {
            e.printStackTrace(); // Para detalhes no console

            // Exibindo uma mensagem de erro em uma janela de diálogo
            JOptionPane.showMessageDialog(null, 
                "Ocorreu um erro durante a execução do programa: " + e.getMessage(),
                "Erro Crítico",
                JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        }
    }
}

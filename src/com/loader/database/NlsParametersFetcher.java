package com.loader.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class NlsParametersFetcher {
    private final ConnectionManager connectionManager;

    public NlsParametersFetcher(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public Map<String, String> fetchNlsParameters() {
        Map<String, String> nlsParameters = new HashMap<>();
        String sql = "SELECT PARAMETER, VALUE FROM NLS_SESSION_PARAMETERS";

        try (Connection conn = connectionManager.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String parameter = rs.getString("PARAMETER");
                String value = rs.getString("VALUE");
                nlsParameters.put(parameter, value);
                /**/System.out.println("NLS Parameter: " + parameter + ", Value: " + value); // Log dos parâmetros NLS
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nlsParameters;
    }
    
    public String getNLSCharset() {
        return fetchNlsParameters().getOrDefault("NLS_CHARACTERSET", "WE8ISO8859P1"); // Valor padrão WE8ISO8859P1
    }
}

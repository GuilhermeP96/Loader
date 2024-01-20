package com.loader.util;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class DataFormatter {

    private Map<String, String> nlsParameters;

    public DataFormatter(Map<String, String> nlsParameters) {
        this.nlsParameters = nlsParameters;
    }

    public String formatString(String value) {
        // Perform any necessary string formatting
        return value;
    }

    public String formatDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Identifica se é uma data, uma hora ou uma data com hora
        String formatPattern;
        SimpleDateFormat outputFormat;

        if (value.contains("/") && value.contains(":")) {
            // Assume formato de data e hora, por exemplo, "dd/MM/yyyy HH:mm:ss"
            formatPattern = "dd/MM/yyyy HH:mm:ss";
            outputFormat = new SimpleDateFormat(getJavaDateTimeFormat());
        } else if (value.contains(":")) {
            // Assume formato de hora, por exemplo, "HH:mm:ss"
            formatPattern = "HH:mm:ss";
            outputFormat = new SimpleDateFormat(getJavaTimeFormat());
        } else {
            // Assume formato de data, por exemplo, "dd/MM/yyyy"
            formatPattern = "dd/MM/yyyy";
            outputFormat = new SimpleDateFormat(getJavaDateFormat());
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(formatPattern);
            Date date = inputFormat.parse(value);
            return outputFormat.format(date);
        } catch (ParseException e) {
            System.err.println("Date parsing error: " + e.getMessage());
            return null;
        }
    }

    private String getJavaDateFormat() {
        String nlsDateFormat = nlsParameters.getOrDefault("NLS_DATE_FORMAT", "yyyy-MM-dd");
        return convertToJavaDateFormat(nlsDateFormat);
    }

    private String getJavaTimeFormat() {
        String nlsTimeFormat = nlsParameters.getOrDefault("NLS_TIME_FORMAT", "HH:mm:ss");
        return convertToJavaDateFormat(nlsTimeFormat);
    }

    private String getJavaDateTimeFormat() {
        String nlsTimestampFormat = nlsParameters.getOrDefault("NLS_TIMESTAMP_FORMAT", "yyyy-MM-dd HH:mm:ss");
        return convertToJavaDateFormat(nlsTimestampFormat);
    }


    private String convertToJavaDateFormat(String oracleFormat) {
        // Replace Oracle's RR with Java's yy (note: this does not replicate Oracle's exact century logic)
        oracleFormat = oracleFormat.replace("RRRR", "yyyy");
        oracleFormat = oracleFormat.replace("RR", "yy");
        oracleFormat = oracleFormat.replace("DD", "dd");
        oracleFormat = oracleFormat.replace("MM", "MM");
        oracleFormat = oracleFormat.replace("YYYY", "yyyy");
        oracleFormat = oracleFormat.replace("HH24", "HH");
        oracleFormat = oracleFormat.replace("HH12", "hh");
        oracleFormat = oracleFormat.replace("MI", "mm");
        oracleFormat = oracleFormat.replace("SS", "ss");
        oracleFormat = oracleFormat.replace("FF3", "SSS");
        oracleFormat = oracleFormat.replace("TZO", "XXX"); 
        // Add more replacements based on Oracle format specifics as needed
        return oracleFormat;
    }
    
    private String convertToJavaDecimalFormat(String oracleValue) {
        // Oracle uses "," as decimal separator, so we need to replace it with "."
        return oracleValue.replace(",", ".");
    }
    
    public String formatDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String nlsDecimalFormat = nlsParameters.getOrDefault("NLS_NUMERIC_CHARACTERS", ".,").substring(0, 1);
        if (!nlsDecimalFormat.equals(".")) {
            value = convertToJavaDecimalFormat(value);
        }

        // Verifica se o valor tem casas decimais
        if (value.contains(".")) {
            DecimalFormat df = new DecimalFormat("0.##"); // Use "." as decimal separator for Java
            return df.format(Double.parseDouble(value));
        } else {
            // Não há casas decimais, então formate como número inteiro
            return value;
        }
    }

    // Method to determine and apply the correct formatting based on the data type
    public String formatData(String value, String dataType) {
        switch (dataType) {
            case "DATE":
            case "TIMESTAMP":
                return formatDate(value);
            case "NUMBER":
                return formatDecimal(value);
            default:
                return formatString(value);
        }
    }
}

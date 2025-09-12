package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.*;

public class SQLParser {
    static class TableInfo {
        String tableName;
        String tableComment;
        List<ColumnInfo> columns = new ArrayList<>();
    }

    static class ColumnInfo {
        String columnName;
        String dataType;
        String length;
        String comment;
        boolean isNullable;
        String defaultValue;  // 添加默认值字段
    }

    public static void main(String[] args) {
        try {
            // Read SQL file with GBK encoding
            List<TableInfo> tables = parseSQLFile("src/main/resources/zhihui2qi/AQUSERT_ALL_DDL.sql");
            // Generate Excel
            generateExcel(tables, "table_structure.xlsx");
            System.out.println("Excel文件生成成功！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<TableInfo> parseSQLFile(String filename) throws IOException {
        List<TableInfo> tables = new ArrayList<>();
        TableInfo currentTable = null;

        // Use GBK encoding
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), Charset.forName("GBK"))
        );

        String line;
        StringBuilder createTableStatement = new StringBuilder();
        boolean inCreateTable = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }

            // Parse CREATE TABLE
            if (line.startsWith("CREATE TABLE")) {
                inCreateTable = true;
                currentTable = new TableInfo();
                createTableStatement = new StringBuilder();

                // Extract table name
                Pattern pattern = Pattern.compile("CREATE TABLE \"?(\\w+)\"?\\.\"?(\\w+)\"?");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    currentTable.tableName = matcher.group(2).replace("\"", "");
                }
            }

            if (inCreateTable) {
                createTableStatement.append(line).append("\n");
                if (line.contains(";")) {
                    inCreateTable = false;
                    parseColumns(createTableStatement.toString(), currentTable);
                    tables.add(currentTable);
                }
            }

            // Parse table comment
            if (line.startsWith("COMMENT ON TABLE")) {
                Pattern pattern = Pattern.compile("COMMENT ON TABLE .+? IS '(.+?)';");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String tableName = extractTableName(line);
                    String comment = matcher.group(1);
                    for (TableInfo table : tables) {
                        if (table.tableName.equals(tableName)) {
                            table.tableComment = comment;
                            break;
                        }
                    }
                }
            }

            // Parse column comment
            if (line.startsWith("COMMENT ON COLUMN")) {
                Pattern pattern = Pattern.compile("COMMENT ON COLUMN .+? IS '(.+?)';");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String[] parts = extractTableAndColumnName(line);
                    String tableName = parts[0];
                    String columnName = parts[1];
                    String comment = matcher.group(1);

                    for (TableInfo table : tables) {
                        if (table.tableName.equals(tableName)) {
                            for (ColumnInfo column : table.columns) {
                                if (column.columnName.equals(columnName)) {
                                    column.comment = comment;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        reader.close();
        return tables;
    }

    private static void parseColumns(String createTableStatement, TableInfo table) {
        // Split the statement into lines
        String[] lines = createTableStatement.split("\n");
        Pattern columnPattern = Pattern.compile("\"(\\w+)\"\\s+(\\w+(?:\\([^)]+\\))?)[^,)]*");
        Pattern defaultPattern = Pattern.compile("DEFAULT\\s+([^,\\s]+)");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("\"")) {  // Column definition line
                Matcher matcher = columnPattern.matcher(line);
                if (matcher.find()) {
                    ColumnInfo column = new ColumnInfo();
                    column.columnName = matcher.group(1);

                    String fullDataType = matcher.group(2);
                    parseDataTypeAndLength(fullDataType, column);

                    // Parse DEFAULT value
                    Matcher defaultMatcher = defaultPattern.matcher(line);
                    if (defaultMatcher.find()) {
                        column.defaultValue = defaultMatcher.group(1);
                    }

                    column.isNullable = !line.contains("NOT NULL");
                    table.columns.add(column);
                }
            }
        }
    }

    private static void parseDataTypeAndLength(String fullDataType, ColumnInfo column) {
        Pattern lengthPattern = Pattern.compile("(\\w+)\\(([^)]+)\\)");
        Matcher lengthMatcher = lengthPattern.matcher(fullDataType);

        if (lengthMatcher.find()) {
            column.dataType = lengthMatcher.group(1);
            column.length = lengthMatcher.group(2);
            // Remove BYTE keyword if present
            column.length = column.length.replaceAll("\\s+BYTE", "");
        } else {
            column.dataType = fullDataType;
            column.length = "";
        }
    }

    private static String extractTableName(String line) {
        Pattern pattern = Pattern.compile("\"?(\\w+)\"?\\.\"?(\\w+)\"?");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(2).replace("\"", "");
        }
        return "";
    }

    private static String[] extractTableAndColumnName(String line) {
        Pattern pattern = Pattern.compile("\"?(\\w+)\"?\\.\"?(\\w+)\"?\\.\"?(\\w+)\"?");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return new String[]{matcher.group(2).replace("\"", ""),
                    matcher.group(3).replace("\"", "")};
        }
        return new String[]{"", ""};
    }

    private static void generateExcel(List<TableInfo> tables, String filename) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("表结构");

        // Create header row with Chinese headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"表名", "表注释", "字段名", "数据类型", "长度", "是否可空", "默认值", "字段注释"};

        // Create cell style for headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create cell style for data
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setWrapText(true);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Fill data
        int rowNum = 1;
        for (TableInfo table : tables) {
            for (ColumnInfo column : table.columns) {
                Row row = sheet.createRow(rowNum++);

                Cell[] cells = new Cell[8];  // Changed to 8 for the new default value column
                for (int i = 0; i < cells.length; i++) {
                    cells[i] = row.createCell(i);
                    cells[i].setCellStyle(dataStyle);
                }

                cells[0].setCellValue(table.tableName);
                cells[1].setCellValue(table.tableComment != null ? table.tableComment : "");
                cells[2].setCellValue(column.columnName);
                cells[3].setCellValue(column.dataType);
                cells[4].setCellValue(column.length);
                cells[5].setCellValue(column.isNullable ? "是" : "否");
                cells[6].setCellValue(column.defaultValue != null ? column.defaultValue : "");
                cells[7].setCellValue(column.comment != null ? column.comment : "");
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Set a maximum column width
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth > 15000) {
                sheet.setColumnWidth(i, 15000);
            }
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(filename)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}
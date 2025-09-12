package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DocxToMysqlDDL {
    private static final Logger logger = LoggerFactory.getLogger(DocxToMysqlDDL.class);

    public static void main(String[] args) {
        String directoryPath = "src/main/resources/DHR";
        String outPath = "src/main/resources/DDLS/";
        File directory = new File(directoryPath);

        // 用于存储所有表的字段信息
        Map<String, List<Field>> allTableFields = new HashMap<>();

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".docx"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String tableName = file.getName().replace(".docx", "");
                        List<Field> fields = readDocxFile(file);
                        String ddl = generateDDL(tableName.toLowerCase(), fields);
                        logger.info(ddl);
                        write(ddl, outPath, file.getName().toLowerCase());

                        // 将表名和字段信息存储到Map中
                        allTableFields.put(tableName, fields);

                    } catch (IOException e) {
                        logger.error("Error processing file: {}", file.getName(), e);
                    }
                }

                // 生成包含所有表信息的Excel文件
                try {
                    String excelPath = outPath + "all_tables_info.xlsx";
                    generateExcelForAllTables(allTableFields, excelPath);
                } catch (IOException e) {
                    logger.error("Error generating Excel file", e);
                }
            }
        } else {
            logger.error("Directory does not exist: {}", directoryPath);
        }
    }
    private static void generateExcelForAllTables(Map<String, List<Field>> allTableFields, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("所有表结构信息");

            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 创建表格样式
            CellStyle tableStyle = workbook.createCellStyle();
            tableStyle.setAlignment(HorizontalAlignment.LEFT);
            tableStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"表名", "字段名", "字段描述", "字段类型", "长度", "精度", "是否必填", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 设置列宽
            sheet.setColumnWidth(0, 5000); // 表名
            sheet.setColumnWidth(1, 4000); // 字段名
            sheet.setColumnWidth(2, 6000); // 字段描述
            sheet.setColumnWidth(3, 4000); // 字段类型
            sheet.setColumnWidth(4, 3000); // 长度
            sheet.setColumnWidth(5, 3000); // 精度
            sheet.setColumnWidth(6, 3000); // 是否必填
            sheet.setColumnWidth(7, 8000); // 备注

            // 添加数据
            int rowNum = 1;
            for (Map.Entry<String, List<Field>> entry : allTableFields.entrySet()) {
                String tableName = entry.getKey();
                List<Field> fields = entry.getValue();

                for (Field field : fields) {
                    if (field.getKey() != null) {
                        Row row = sheet.createRow(rowNum++);

                        // 设置单元格样式
                        for (int i = 0; i < 8; i++) {
                            Cell cell = row.createCell(i);
                            cell.setCellStyle(tableStyle);
                        }

                        // 填充数据
                        row.getCell(0).setCellValue(tableName); // 表名
                        row.getCell(1).setCellValue(field.getKey()); // 字段名
                        row.getCell(2).setCellValue(field.getName() != null ? field.getName() : ""); // 字段描述
                        row.getCell(3).setCellValue(field.getType() != null ? field.getType() : ""); // 字段类型
                        row.getCell(4).setCellValue(field.getLength() != null ? field.getLength().toString() : ""); // 长度
                        row.getCell(5).setCellValue(field.getPrecision() != null ? field.getPrecision().toString() : ""); // 精度
                        row.getCell(6).setCellValue(field.getIs_required() != null && field.getIs_required() ? "是" : "否"); // 是否必填
                        row.getCell(7).setCellValue(field.getDescription() != null ? field.getDescription() : ""); // 备注
                    }
                }
            }

            // 添加汇总信息
            rowNum += 2;
            Row summaryRow = sheet.createRow(rowNum);
            summaryRow.createCell(0).setCellValue("总表数：" + allTableFields.size());
            summaryRow.createCell(1).setCellValue("总字段数：" +
                    allTableFields.values().stream()
                            .mapToLong(fields -> fields.stream().filter(f -> f.getKey() != null).count())
                            .sum()
            );

            // 创建自动筛选器
            sheet.setAutoFilter(CellRangeAddress.valueOf("A1:H" + (rowNum - 2)));

            // 冻结首行
            sheet.createFreezePane(0, 1);

            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            logger.info("Combined Excel file generated successfully: {}", filePath);
        }
    }
    private static String generateDDL(String tableName, List<Field> fields) throws IOException {
        StringBuilder ddlBuilder = new StringBuilder();
        ddlBuilder.append("CREATE TABLE `").append(tableName).append("` (\n");

        List<String> fieldDefinitions = fields.stream()
                .filter(field -> field.getKey() != null)
                .map(field -> {
                    StringBuilder fieldDef = new StringBuilder();
                    fieldDef.append("  `").append(field.getKey()).append("` ")
                            .append(getMysqlType(field));

                    if (field.getIs_required() != null && field.getIs_required()) {
                        fieldDef.append(" NOT NULL");
                    }

                    if (field.getDescription() != null) {
                        fieldDef.append(" COMMENT '").append(field.getDescription().replace("'", "\\'")).append("'");
                    } else if (field.getName() != null) {
                        fieldDef.append(" COMMENT '").append(field.getName().replace("'", "\\'")).append("'");
                    }

                    return fieldDef.toString();
                })
                .collect(Collectors.toList());

        ddlBuilder.append(String.join(",\n", fieldDefinitions));
        ddlBuilder.append(",\n  PRIMARY KEY (`id`)");
        ddlBuilder.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        if (tableName != null && !tableName.isEmpty()) {
            ddlBuilder.append(" COMMENT='").append(tableName).append("'");
        }

        ddlBuilder.append(";");

        return ddlBuilder.toString();
    }
    private static void generateExcel(List<Field> fields, String tableName, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("表结构信息");

            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"字段名", "字段描述", "字段类型", "长度", "精度", "是否必填", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 设置列宽
            sheet.setColumnWidth(0, 4000); // 字段名
            sheet.setColumnWidth(1, 6000); // 字段描述
            sheet.setColumnWidth(2, 4000); // 字段类型
            sheet.setColumnWidth(3, 3000); // 长度
            sheet.setColumnWidth(4, 3000); // 精度
            sheet.setColumnWidth(5, 3000); // 是否必填
            sheet.setColumnWidth(6, 8000); // 备注

            // 添加表数据
            int rowNum = 1;
            for (Field field : fields) {
                if (field.getKey() != null) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(field.getKey());
                    row.createCell(1).setCellValue(field.getName());
                    row.createCell(2).setCellValue(field.getType());
                    row.createCell(3).setCellValue(field.getLength() != null ? field.getLength().toString() : "");
                    row.createCell(4).setCellValue(field.getPrecision() != null ? field.getPrecision().toString() : "");
                    row.createCell(5).setCellValue(field.getIs_required() != null && field.getIs_required() ? "是" : "否");
                    row.createCell(6).setCellValue(field.getDescription() != null ? field.getDescription() : "");
                }
            }

            // 添加表信息摘要
            sheet.createRow(rowNum + 1).createCell(0).setCellValue("表名：" + tableName);
            sheet.createRow(rowNum + 2).createCell(0).setCellValue("总字段数：" + fields.stream().filter(f -> f.getKey() != null).count());

            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            logger.info("Excel file generated successfully: {}", filePath);
        }
    }
    private static String getMysqlType(Field field) {
        if (field.getType() == null) return "VARCHAR(255)";

        switch (field.getType().toUpperCase()) {
            case "STRING":
                return field.getLength() != null ? "VARCHAR(" + field.getLength() + ")" : "VARCHAR(255)";
            case "INTEGER":
                return "INT";
            case "BIGINT":
                return "BIGINT";
            case "DECIMAL":
            case "FLOAT":
                return field.getPrecision() != null ?
                        "DECIMAL(" + (field.getLength() != null ? field.getLength() : 10) + "," + field.getPrecision() + ")" :
                        "DECIMAL(10,2)";
            case "BOOLEAN":
                return "TINYINT(1)";
            case "DATE":
                return "DATE";
            case "DATETIME":
                return "DATETIME";
            case "JSON":
                return "JSON";
            default:
                return "VARCHAR(255)";
        }
    }
    private static List<Field> readDocxFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String content = extractor.getText()
                    .replaceAll("(?m)^\\s*$[\n\r]{1,}", "") // 删除空行
                    .trim();

            // 将内容包装成JSON数组
            if (!content.startsWith("[")) {
                content = "[" + content + "]";
            }

            // 清理可能的多余逗号
            content = content.replaceAll(",\\s*\\]", "]");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            List<Field> fields = new ArrayList<>();
            try {
                // 直接解析为Field对象数组
                Field[] fieldArray = mapper.readValue(content, Field[].class);
                for (Field field : fieldArray) {
                    if (field.getKey() != null) {
                        fields.add(field);
                    }
                }
            } catch (Exception e) {
                logger.error("Error parsing JSON content: {}", e.getMessage());
                // 如果整体解析失败，尝试逐个对象解析
                String[] jsonObjects = content.split("\\},\\s*\\{");
                for (int i = 0; i < jsonObjects.length; i++) {
                    String jsonObject = jsonObjects[i];
                    if (i == 0) {
                        jsonObject = jsonObject.replaceFirst("\\[\\s*\\{", "{");
                    }
                    if (i == jsonObjects.length - 1) {
                        jsonObject = jsonObject.replaceFirst("\\}\\s*\\]", "}");
                    }
                    try {
                        Field field = mapper.readValue(jsonObject, Field.class);
                        if (field.getKey() != null) {
                            fields.add(field);
                        }
                    } catch (Exception ex) {
                        logger.error("Error parsing individual JSON object: {}", ex.getMessage());
                    }
                }
            }

            return fields;
        }
    }
//    private static List<Field> readDocxFile(File file) throws IOException {
//        try (FileInputStream fis = new FileInputStream(file);
//             XWPFDocument document = new XWPFDocument(fis);
//             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
//
//            String content = extractor.getText()
//                    .replaceAll("(?m)^\\s*$[\n\r]{1,}", "") // 删除空行
//                    .replaceAll(",\\s*\\}", "}")  // 删除对象末尾多余的逗号
//                    .trim();
//
//            // 确保内容是一个JSON数组
//            if (!content.startsWith("[")) content = "[" + content;
//            if (!content.endsWith("]")) content = content + "]";
//
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//            List<Field> fields = new ArrayList<>();
//            String[] jsonObjects = content.substring(1, content.length() - 1).split("\\},\\s*\\{");
//
//            for (int i = 0; i < jsonObjects.length; i++) {
//                String jsonStr = jsonObjects[i];
//                if (!jsonStr.startsWith("{")) jsonStr = "{" + jsonStr;
//                if (!jsonStr.endsWith("}")) jsonStr = jsonStr + "}";
//
//                try {
//                    Field field = mapper.readValue(jsonStr, Field.class);
//                    if (field.getKey() != null) {
//                        fields.add(field);
//                    }
//                } catch (Exception e) {
//                    logger.error("Error parsing JSON object: " + jsonStr, e);
//                }
//            }
//
//            return fields;
//        }
//    }

    public static void write(String sql, String filePath, String file) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(filePath + file + "_ddl.sql"),
                        StandardCharsets.UTF_8
                ))) {
            writer.write(sql);
            writer.newLine();
            System.out.println("DDL statement written to file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error writing DDL statement to file.");
        }
    }

    static class Field {
        private String key;
        private String name;
        private String type;
        private String description;
        private Integer length;
        private Integer precision;
        private Boolean is_required;
        private String format;

        // Getters and Setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Integer getLength() { return length; }
        public void setLength(Integer length) { this.length = length; }

        public Integer getPrecision() { return precision; }
        public void setPrecision(Integer precision) { this.precision = precision; }

        public Boolean getIs_required() { return is_required; }
        public void setIs_required(Boolean is_required) { this.is_required = is_required; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
}
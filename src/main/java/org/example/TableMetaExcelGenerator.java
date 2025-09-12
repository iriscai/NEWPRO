package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class TableMetaExcelGenerator {
    public static void main(String[] args) {
        try {
            String directoryPath = "src/main/resources/1126"; // 指定文件夹路径
            List<FieldInfo> fieldList = new ArrayList<>();

            // 读取文件夹中的所有文件
            File folder = new File(directoryPath);
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt")); // 仅处理.txt文件
            if (files != null) {
                for (File file : files) {
                    readFieldInfoFromFile(file, fieldList);
                }
            }

            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("字段信息");

            // 创建标题样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle cellStyle = createCellStyle(workbook);

            // 创建表头
            String[] headers = {"字段名称", "数据类型", "是否主键", "字段说明", "字段长度", "格式", "默认值"};
            createHeaderRow(sheet, headers, headerStyle);

            // 填充数据
            fillDataRows(sheet, fieldList, cellStyle);

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存Excel文件
            try (FileOutputStream fileOut = new FileOutputStream("字段信息表.xlsx")) {
                workbook.write(fileOut);
            }

            workbook.close();
            System.out.println("Excel文件已生成: 字段信息表.xlsx");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readFieldInfoFromFile(File file, List<FieldInfo> fieldList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFieldSection = false;

            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("#Field")) {
                    isFieldSection = true;
                    // 跳过字段头部说明行
                    reader.readLine();
                    // 跳过分隔符行
                    reader.readLine();
                    continue;
                }

                if (isFieldSection && !line.trim().isEmpty() && !line.startsWith("-")) {
                    String[] fields = line.split("\\s+");
                    if (fields.length >= 7) {
                        FieldInfo fieldInfo = new FieldInfo(
                                fields[0].trim(),    // name
                                fields[1].trim(),    // dataType
                                fields[2].trim(),    // isKey
                                fields[3].trim(),    // description
                                fields[4].trim(),    // width
                                fields[5].trim(),    // format
                                fields[6].trim()     // default
                        );
                        fieldList.add(fieldInfo);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件 " + file.getName() + " 时出错: " + e.getMessage());
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        return headerStyle;
    }

    private static CellStyle createCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        return cellStyle;
    }

    private static void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static void fillDataRows(Sheet sheet, List<FieldInfo> fieldList, CellStyle cellStyle) {
        for (int i = 0; i < fieldList.size(); i++) {
            Row row = sheet.createRow(i + 1);
            FieldInfo field = fieldList.get(i);

            row.createCell(0).setCellValue(field.getName());
            row.createCell(1).setCellValue(field.getDataType());
            row.createCell(2).setCellValue(field.getIsKey());
            row.createCell(3).setCellValue(field.getDescription());
            row.createCell(4).setCellValue(field.getWidth());
            row.createCell(5).setCellValue(field.getFormat());
            row.createCell(6).setCellValue(field.getDefaultValue());

            for (int j = 0; j < 7; j++) {
                row.getCell(j).setCellStyle(cellStyle);
            }
        }
    }
}

// 字段信息类
class FieldInfo {
    private String name;
    private String dataType;
    private String isKey;
    private String description;
    private String width;
    private String format;
    private String defaultValue;

    public FieldInfo(String name, String dataType, String isKey,
                     String description, String width, String format,
                     String defaultValue) {
        this.name = name;
        this.dataType = dataType;
        this.isKey = isKey;
        this.description = description;
        this.width = width;
        this.format = format;
        this.defaultValue = defaultValue;
    }

    // Getters
    public String getName() { return name; }
    public String getDataType() { return dataType; }
    public String getIsKey() { return isKey; }
    public String getDescription() { return description; }
    public String getWidth() { return width; }
    public String getFormat() { return format; }
    public String getDefaultValue() { return defaultValue; }
}

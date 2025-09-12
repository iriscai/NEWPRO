package com.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExcelDDLProcessor {
    public static void main(String[] args) throws IOException {
        // Excel文件路径
        String excelPath = "src/main/resources/0812/明细脚本内容.xlsx";
        // DDL文件所在文件夹路径
        String ddlFolderPath = "src/main/resources/0812/in/";
        // 输出结果日志
        StringBuilder log = new StringBuilder();

        // 读取Excel文件
        FileInputStream fis = new FileInputStream(new File(excelPath));
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        // 确保“是否脚本内容”列存在，如果没有则创建
        int scriptColumnIndex = 3; // 假设“是否脚本内容”是第4列（索引为3）
        if (sheet.getRow(0).getCell(scriptColumnIndex) == null) {
            sheet.getRow(0).createCell(scriptColumnIndex).setCellValue("是否脚本内容");
        }

        // 遍历Excel行，从第2行开始（假设第1行为标题）
        for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                log.append("行 ").append(rowNum + 1).append(" 为空，跳过\n");
                continue;
            }

            try {
                String chineseDesc = row.getCell(0) != null ? row.getCell(0).getStringCellValue().trim() : "";
                String sourceTable = row.getCell(1) != null ? row.getCell(1).getStringCellValue().trim() : "";
                String targetTable = row.getCell(2) != null ? row.getCell(2).getStringCellValue().trim() : "";

                if (chineseDesc.isEmpty() || sourceTable.isEmpty() || targetTable.isEmpty()) {
                    log.append("行 ").append(rowNum + 1).append(" 数据不完整，跳过\n");
                    continue;
                }

                // 读取对应的DDL文件
                String ddlFilePath = ddlFolderPath + sourceTable + ".txt";
                String ddlContent = readDDLFile(ddlFilePath);

                if (ddlContent.isEmpty()) {
                    log.append("行 ").append(rowNum + 1).append(" 无法找到DDL文件：").append(ddlFilePath).append("，跳过\n");
                    continue;
                }

                // 提取字段定义和注释，保持顺序
                List<FieldDefinition> fieldDefinitions = extractFieldDefinitions(ddlContent);

                if (fieldDefinitions.isEmpty()) {
                    log.append("行 ").append(rowNum + 1).append(" DDL文件解析失败：").append(ddlFilePath).append("，跳过\n");
                    continue;
                }

                // 生成脚本内容
                String scriptContent = generateScriptContent(chineseDesc, targetTable, sourceTable, fieldDefinitions).replace("`","");

                // 写入脚本内容到Excel的“是否脚本内容”列
                Cell scriptCell = row.getCell(scriptColumnIndex);
                if (scriptCell == null) {
                    scriptCell = row.createCell(scriptColumnIndex);
                } else {
                    scriptCell.setCellValue(""); // 清除现有内容
                }
                scriptCell.setCellValue(scriptContent);

                log.append("行 ").append(rowNum + 1).append(" 处理成功：").append(chineseDesc).append("\n");

            } catch (Exception e) {
                log.append("行 ").append(rowNum + 1).append(" 处理失败：").append(e.getMessage()).append("，跳过\n");
                continue;
            }
        }

        // 保存修改后的Excel文件
        FileOutputStream fos = new FileOutputStream(excelPath);
        workbook.write(fos);

        // 关闭资源
        workbook.close();
        fis.close();
        fos.close();

        // 输出日志到控制台或文件
        System.out.println(log.toString());
        // 可选：将日志写入文件
        // writeToFile(log.toString(), "path/to/log.txt");
    }

    // 读取DDL文件内容，使用UTF-8编码
    private static String readDDLFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("读取DDL文件失败：" + filePath);
        }
        return content.toString();
    }

    // 定义字段类以保持顺序
    private static class FieldDefinition {
        String definition;
        String name;
        String comment;

        FieldDefinition(String definition, String name, String comment) {
            this.definition = definition;
            this.name = name;
            this.comment = comment;
        }
    }

    // 提取字段定义和注释，保持顺序，兼容没有COMMENT的情况，并将DECIMAL统一设置为DECIMAL(32,6)
    private static List<FieldDefinition> extractFieldDefinitions(String ddlContent) {
        List<FieldDefinition> fieldDefinitions = new ArrayList<>();
        String[] lines = ddlContent.split("\n");
        boolean inFieldSection = false;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("CREATE TABLE")) {
                inFieldSection = true;
                continue;
            }
            if (inFieldSection && line.startsWith(")")) {
                inFieldSection = false;
                break;
            }
            if (inFieldSection && !line.isEmpty()) {
                String fieldDef;
                String fieldName;
                String comment = "";

                if (line.contains(" COMMENT ")) {
                    String[] parts = line.split(" COMMENT ");
                    fieldDef = parts[0].trim();
                    fieldName = fieldDef.split(" ")[0].replace("`", "");
                    comment = parts[1].trim().replace("'", "").replace(",", "");
                } else {
                    // 没有COMMENT的情况
                    fieldDef = line.trim().replace(",", "");
                    fieldName = fieldDef.split(" ")[0].replace("`", "");
                }

                // 将DECIMAL类型统一设置为DECIMAL(32,6)
                if (fieldDef.toUpperCase().contains("DECIMAL")) {
                    String[] defParts = fieldDef.split(" ");
                    fieldDef = defParts[0] + " DECIMAL(32,6)";
                }

                fieldDefinitions.add(new FieldDefinition(fieldDef, fieldName, comment));
            }
        }
        return fieldDefinitions;
    }

    // 生成脚本内容
    private static String generateScriptContent(String chineseDesc, String targetTable, String sourceTable, List<FieldDefinition> fieldDefinitions) {
        StringBuilder script = new StringBuilder();
        script.append("-- ******************************************************************** --\n");
        script.append("-- author: jh_caiyi\n");
        script.append("-- create time: 2025-07-15\n");
        script.append("-- descr:廉政八项-").append(chineseDesc).append("\n");
        script.append("-- ").append(targetTable).append("\n");
        script.append("-- ******************************************************************** --\n");
        script.append("DROP TABLE IF EXISTS dwi_eqp.dwi_eqp_lz_").append(targetTable).append(";\n");
        script.append("CREATE TABLE dwi_eqp.dwi_eqp_lz_").append(targetTable).append(" (\n");

        // 添加字段定义，保持顺序，排除ext_src_sys_id和ext_etl_dt
        for (FieldDefinition field : fieldDefinitions) {
            if (!field.name.equals("ext_src_sys_id") && !field.name.equals("ext_etl_dt")) {
                script.append("    ").append(field.definition);
                if (!field.comment.isEmpty()) {
                    script.append(" COMMENT '").append(field.comment).append("'");
                }
                script.append(",\n");
            }
        }
        // 添加etl_date字段
        script.append("    etl_date DATE COMMENT 'ETL时间'\n");
        script.append(")\n");
        script.append("COMMENT '廉政八项-").append(chineseDesc).append("'\n");
        script.append("STORED AS ORC;\n\n");

        // 生成INSERT语句
        script.append("INSERT OVERWRITE TABLE dwi_eqp.dwi_eqp_lz_").append(targetTable).append("\n");
        script.append("SELECT \n");
        for (FieldDefinition field : fieldDefinitions) {
            if (!field.name.equals("ext_src_sys_id") && !field.name.equals("ext_etl_dt")) {
                script.append("    ").append(field.name).append(",\n");
            }
        }
        // 添加etl_date字段的默认值
        script.append("    CURRENT_DATE() AS etl_date\n");
        script.append("FROM \n");
        script.append("    dwi_eqp.").append(sourceTable).append(";\n");

        return script.toString();
    }

    // 将结果写入文件（可选），使用UTF-8编码
    private static void writeToFile(String content, String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) {
            bw.write(content);
        }
    }
}
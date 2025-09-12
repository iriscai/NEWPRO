package com.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileParser {

    public static void main(String[] args) {
        String folderPath = "src/main/resources/mr/in"; // 替换为你的文件夹路径
        String outputFilePath = "src/main/resources/mr/out/output-2.xlsx";

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet validDataSheet = workbook.createSheet("Parsed Data");
            Sheet errorLogSheet = workbook.createSheet("Error Log");

            // 创建有效数据表头
            String[] validHeaders = {"所属文件名", "项目", "中文表名", "表名(英文)", "字段(英文)", "字段类型", "长度", "是否主键", "字段注释"};
            Row validHeaderRow = validDataSheet.createRow(0);
            for (int i = 0; i < validHeaders.length; i++) {
                Cell cell = validHeaderRow.createCell(i);
                cell.setCellValue(validHeaders[i]);
            }

            // 创建错误日志表头
            String[] errorHeaders = {"文件名", "错误信息"};
            Row errorHeaderRow = errorLogSheet.createRow(0);
            for (int i = 0; i < errorHeaders.length; i++) {
                Cell cell = errorHeaderRow.createCell(i);
                cell.setCellValue(errorHeaders[i]);
            }

            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                System.out.println("指定的路径不存在或不是一个目录: " + folderPath);
                return;
            }

            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".dao"));
            if (files == null || files.length == 0) {
                System.out.println("没有找到符合条件的文件.");
                return;
            }

            int validRowNum = 1;
            int errorRowNum = 1;
            for (File file : files) {
                String fileNameWithoutExtension = file.getName().replace(".dao", ""); // 去掉 .txt 后缀
                String project = ""; // 项目
                String descript = ""; // 中文表名
                String tableNameEn = ""; // 英文表名

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"))) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("project")) {
                            project = line.split(":")[1].trim(); // 获取项目
                        } else if (line.startsWith("descript")) {
                            descript = line.split(":")[1].trim(); // 获取中文表名
                        } else if (line.startsWith("table")) {
                            tableNameEn = line.split(":")[1].trim(); // 获取英文表名
                        } else if (line.startsWith("#Field")) {
                            continue; // 跳过字段头部
                        }

                        // 解析字段信息
                        if (!line.startsWith("#") && !line.trim().isEmpty()) {
                            String[] fields = line.split("\\s+");
                            // 确保 fields 数组长度足够
                            if (fields.length >= 6) {
                                Row dataRow = validDataSheet.createRow(validRowNum++);
                                dataRow.createCell(0).setCellValue(fileNameWithoutExtension); // 所属文件名
                                dataRow.createCell(1).setCellValue(project); // 项目
                                dataRow.createCell(2).setCellValue(descript); // 中文表名
                                dataRow.createCell(3).setCellValue(tableNameEn); // 英文表名
                                dataRow.createCell(4).setCellValue(fields[0]); // 字段英文
                                dataRow.createCell(5).setCellValue(fields[1]); // 字段类型
                                dataRow.createCell(6).setCellValue(fields[3]); // 长度
                                dataRow.createCell(7).setCellValue(fields[2].equals("Y") ? "是" : "否"); // 是否主键
                                dataRow.createCell(8).setCellValue(fields[4]); // 字段注释
                            } else {
                                logError(errorLogSheet, file.getName(), "字段信息不完整: " + line, errorRowNum++);
                            }
                        }
                    }
                } catch (IOException e) {
                    logError(errorLogSheet, file.getName(), "读取文件失败: " + e.getMessage(), errorRowNum++);
                } catch (Exception e) {
                    logError(errorLogSheet, file.getName(), "未知错误: " + e.getMessage(), errorRowNum++);
                }
            }

            // 写入Excel文件
            try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
                workbook.write(fileOut);
            }

            System.out.println("数据已成功写入到 " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void logError(Sheet errorLogSheet, String fileName, String errorMessage, int rowNum) {
        Row errorRow = errorLogSheet.createRow(rowNum);
        errorRow.createCell(0).setCellValue(fileName);
        errorRow.createCell(1).setCellValue(errorMessage);
    }
}

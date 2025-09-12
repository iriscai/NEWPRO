package com.icsc.mr.util;

import com.icsc.mr.model.FieldInfo;
import com.icsc.mr.model.TableInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public class ExcelExporter {

    public void exportToExcel(List<TableInfo> tableInfoList, String outputPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建表信息工作表
            Sheet tableSheet = workbook.createSheet("表信息");
            createTableInfoSheet(tableSheet, tableInfoList);

            // 创建字段信息工作表
            Sheet fieldSheet = workbook.createSheet("字段信息");
            createFieldInfoSheet(fieldSheet, tableInfoList);

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                workbook.write(outputStream);
            }

            log.info("Excel文件已成功导出到: {}", outputPath);
        } catch (IOException e) {
            log.error("导出Excel失败", e);
        }
    }

    private void createTableInfoSheet(Sheet sheet, List<TableInfo> tableInfoList) {
        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"表名", "表中文名", "项目名", "类名", "包名", "实体名", "作者", "描述"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            sheet.setColumnWidth(i, 4000);
        }

        // 填充数据
        int rowNum = 1;
        for (TableInfo tableInfo : tableInfoList) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(tableInfo.getTableName());
            row.createCell(1).setCellValue(tableInfo.getTableNameCn());
            row.createCell(2).setCellValue(tableInfo.getProjectName());
            row.createCell(3).setCellValue(tableInfo.getClassName());
            row.createCell(4).setCellValue(tableInfo.getPackageName());
            row.createCell(5).setCellValue(tableInfo.getEntityName());
            row.createCell(6).setCellValue(tableInfo.getAuthor());
            row.createCell(7).setCellValue(tableInfo.getDescription());
        }
    }

    private void createFieldInfoSheet(Sheet sheet, List<TableInfo> tableInfoList) {
        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"表名", "字段名", "数据类型", "是否主键", "是否可为空", "描述", "字段宽度", "格式", "默认值"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            sheet.setColumnWidth(i, 4000);
        }

        // 填充数据
        int rowNum = 1;
        for (TableInfo tableInfo : tableInfoList) {
            for (FieldInfo fieldInfo : tableInfo.getFields()) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(tableInfo.getTableName());
                row.createCell(1).setCellValue(fieldInfo.getName());
                row.createCell(2).setCellValue(fieldInfo.getDataType());
                row.createCell(3).setCellValue(fieldInfo.isPrimaryKey() ? "是" : "否");
                row.createCell(4).setCellValue(fieldInfo.isNullable() ? "是" : "否");
                row.createCell(5).setCellValue(fieldInfo.getDescription());
                row.createCell(6).setCellValue(fieldInfo.getWidth());
                row.createCell(7).setCellValue(fieldInfo.getFormat());
                row.createCell(8).setCellValue(fieldInfo.getDefaultValue());
            }
        }
    }
}

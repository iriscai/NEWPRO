package com.icsc.mr;

import com.icsc.mr.model.TableInfo;
import com.icsc.mr.parser.FileParser;
import com.icsc.mr.util.ExcelExporter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Application {

    public static void main(String[] args) {
        // 获取输入目录和输出文件路径
        String inputDir = getInputDirectory(args);
        String outputFile = getOutputFilePath(args);

        log.info("开始处理目录: {}", inputDir);
        log.info("输出文件路径: {}", outputFile);

        // 创建解析器和导出器
        FileParser parser = new FileParser();
        ExcelExporter exporter = new ExcelExporter();
        List<TableInfo> tableInfoList = new ArrayList<>();

        // 获取目录下的所有文件
        File directory = new File(inputDir);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (files == null || files.length == 0) {
            log.warn("目录中没有找到.txt文件: {}", inputDir);
            return;
        }

        // 解析每个文件
        for (File file : files) {
            log.info("正在解析文件: {}", file.getName());
            TableInfo tableInfo = parser.parseFile(file);
            if (tableInfo != null && tableInfo.getTableName() != null) {
                tableInfoList.add(tableInfo);
                log.info("成功解析表: {}, 字段数: {}", tableInfo.getTableName(), tableInfo.getFields().size());
            }
        }

        // 导出到Excel
        if (!tableInfoList.isEmpty()) {
            exporter.exportToExcel(tableInfoList, outputFile);
        } else {
            log.warn("没有找到有效的表信息，Excel未生成");
        }
    }

    private static String getInputDirectory(String[] args) {
        if (args.length > 0 && !args[0].isEmpty()) {
            return args[0];
        }
        // 默认使用当前目录下的input文件夹
        return Paths.get("input").toAbsolutePath().toString();
    }

    private static String getOutputFilePath(String[] args) {
        if (args.length > 1 && !args[1].isEmpty()) {
            return args[1];
        }
        // 默认输出到当前目录下的output.xlsx
        return Paths.get("table_info.xlsx").toAbsolutePath().toString();
    }
}

package org.example;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptParserToExcel {
    public static void main(String[] args) {
        // 指定脚本文件所在的目录路径（请根据实际情况修改路径）
        String directoryPath = "src/main/resources/廉政专项"; // 替换为实际的脚本目录路径
        File directory = new File(directoryPath);

        // 检查目录是否存在
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("指定的目录不存在或不是目录: " + directoryPath);
            return;
        }

        // 存储解析结果的列表
        List<String[]> parsedData = new ArrayList<>();
        parsedData.add(new String[]{"文件名", "文件路径", "第七级目录", "第八级目录", "第九级目录", "当前数据库", "目录", "名称", "描述", "表名", "数据库", "表注释", "DDL内容"});

        // 递归遍历目录并解析文件
        List<File> scriptFiles = new ArrayList<>();
        collectScriptFiles(directory, scriptFiles);

        if (scriptFiles.isEmpty()) {
            System.out.println("目录及其子目录下没有找到脚本文件 (.script 扩展名): " + directoryPath);
            return;
        }

        // 遍历并解析每个脚本文件
        for (File scriptFile : scriptFiles) {
            try {
                String scriptContent = readFileContent(scriptFile);
                String[] parsedRow = parseScriptContent(scriptFile.getName(), scriptFile.getAbsolutePath(), directoryPath, scriptContent);
                parsedData.add(parsedRow);
                System.out.println("已解析文件: " + scriptFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("读取文件出错: " + scriptFile.getAbsolutePath() + ", 错误信息: " + e.getMessage());
            }
        }

        // 创建 Excel 文件
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("脚本分析");

            // 写入数据
            for (int i = 0; i < parsedData.size(); i++) {
                Row row = sheet.createRow(i);
                String[] rowData = parsedData.get(i);
                for (int j = 0; j < rowData.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(rowData[j] != null ? rowData[j] : "");
                }
            }

            // 自动调整列宽
            for (int i = 0; i < parsedData.get(0).length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存到文件
            String outputPath = directoryPath + "/ScriptAnalysis.xlsx";
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
                System.out.println("Excel 文件已生成: " + outputPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 递归收集目录及其子目录下的所有 .script 文件
    private static void collectScriptFiles(File directory, List<File> scriptFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectScriptFiles(file, scriptFiles); // 递归遍历子目录
                } else if (file.getName().endsWith(".script")) {
                    scriptFiles.add(file); // 收集 .script 文件
                }
            }
        }
    }

    // 读取文件内容的辅助方法，使用 UTF-8 编码
    private static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    // 解析脚本内容的辅助方法
    private static String[] parseScriptContent(String fileName, String filePath, String rootPath, String scriptContent) {
        // 提取元数据
        String currentDatabase = extractField(scriptContent, "\"currentDatabase\":\"(.*?)\"");
        String directory = extractField(scriptContent, "\"directory\":\"(.*?)\"");
        String name = extractField(scriptContent, "\"name\":\"(.*?)\"");

        // 提取 content 内容
        String content = extractField(scriptContent, "\"content\":\"(.*?)\"");
        content = content.replace("\\n", "\n"); // 替换换行符

        // 提取 descr
        String descr = extractField(content, "-- descr:(.*?)\n");

        // 提取表名和数据库
        String tableFullName = extractField(content, "DROP TABLE IF EXISTS (.*?);");
        String database = "";
        String tableName = "";
        if (!tableFullName.isEmpty()) {
            String[] tableParts = tableFullName.split("\\.");
            if (tableParts.length == 2) {
                database = tableParts[0];
                tableName = tableParts[1];
            }
        }

        // 提取表注释（确保提取的是表注释而非字段注释）
        String tableComment = "";
        Pattern commentPattern = Pattern.compile("CREATE TABLE\\s*.*?\\s*\\(\\s*.*?\\s*\\)\\s*COMMENT\\s*'(.*?)'", Pattern.DOTALL);
        Matcher commentMatcher = commentPattern.matcher(content);
        if (commentMatcher.find()) {
            tableComment = commentMatcher.group(1);
        }

        // 提取 DDL 建表语句 (注释掉的部分)
        String ddlContent = "";
        String ddlStart = "-- DROP TABLE IF EXISTS";
        String ddlEnd = "-- STORED AS ORC;";
        int startIndex = content.indexOf(ddlStart);
        int endIndex = content.indexOf(ddlEnd);
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            ddlContent = content.substring(startIndex, endIndex + ddlEnd.length()).replace("-- ", "");
        }

        // 拆分文件路径为多层目录，并提取第7、8、9级目录
        String level7 = "";
        String level8 = "";
        String level9 = "";
        String relativePath = filePath.replace(rootPath, "").replace("\\", "/").trim();
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        String[] pathParts = relativePath.split("/");
        if (pathParts.length > 7) { // 第7级目录（索引为6）
            level7 = pathParts[6];
        }
        if (pathParts.length > 8) { // 第8级目录（索引为7）
            level8 = pathParts[7];
        }
        if (pathParts.length > 9) { // 第9级目录（索引为8）
            level9 = pathParts[8];
        }

        // 返回解析结果
        return new String[]{
                fileName,
                filePath,
                level7,
                level8,
                level9,
                currentDatabase,
                directory,
                name,
                descr,
                tableName,
                database,
                tableComment,
                ddlContent
        };
    }

    // 提取字段内容的辅助方法
    private static String extractField(String content, String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }
}


package com.icsc.mr.parser;

import com.icsc.mr.model.FieldInfo;
import com.icsc.mr.model.TableInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FileParser {

    public TableInfo parseFile(File file) {
        TableInfo tableInfo = new TableInfo();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            boolean isMetaSection = false;
            boolean isFieldSection = false;
            boolean isHeaderParsed = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }

                // 检测区域
                if (line.startsWith("#Meta")) {
                    isMetaSection = true;
                    isFieldSection = false;
                    continue;
                } else if (line.startsWith("#Field")) {
                    isMetaSection = false;
                    isFieldSection = true;
                    continue;
                }

                // 处理Meta区域
                if (isMetaSection && line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();

                        switch (key.toLowerCase()) {
                            case "project":
                                tableInfo.setProjectName(value);
                                break;
                            case "table":
                                tableInfo.setTableName(extractTableName(value));
                                break;
                            case "class":
                                tableInfo.setClassName(value);
                                break;
                            case "package":
                                tableInfo.setPackageName(value);
                                break;
                            case "entity":
                                tableInfo.setEntityName(value);
                                break;
                            case "author":
                                tableInfo.setAuthor(value);
                                break;
                            case "descript":
                                tableInfo.setTableNameCn(value);
                                tableInfo.setDescription(value);
                                break;
                        }
                    }
                }

                // 处理Field区域
                if (isFieldSection) {
                    // 跳过表头行
                    if (line.startsWith("#") || line.contains("-------")) {
                        isHeaderParsed = true;
                        continue;
                    }

                    if (isHeaderParsed && !line.isEmpty()) {
                        FieldInfo fieldInfo = parseFieldLine(line);
                        if (fieldInfo != null) {
                            tableInfo.getFields().add(fieldInfo);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("解析文件失败: {}", file.getName(), e);
        }

        return tableInfo;
    }

    private String extractTableName(String value) {
        // 提取表名，例如从 "DB.TBMRAA01" 提取 "TBMRAA01"
        Pattern pattern = Pattern.compile("[\\w]+\\.(\\w+)");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return value;
    }

    private FieldInfo parseFieldLine(String line) {
        // 使用制表符分割字段
        String[] parts = line.split("\\t");
        if (parts.length < 3) {
            return null;
        }

        FieldInfo fieldInfo = new FieldInfo();

        try {
            int index = 0;
            fieldInfo.setName(parts[index++].trim());

            if (index < parts.length) fieldInfo.setDataType(parts[index++].trim());

            if (index < parts.length) {
                String isPrimaryKey = parts[index++].trim();
                fieldInfo.setPrimaryKey("Y".equalsIgnoreCase(isPrimaryKey) || "y".equalsIgnoreCase(isPrimaryKey));
                fieldInfo.setNullable(!fieldInfo.isPrimaryKey()); // 假设主键不可为空
            }

            if (index < parts.length) fieldInfo.setDescription(parts[index++].trim());

            if (index < parts.length) fieldInfo.setWidth(parts[index++].trim());

            if (index < parts.length) fieldInfo.setFormat(parts[index++].trim());

            if (index < parts.length) fieldInfo.setDefaultValue(parts[index].trim());
        } catch (Exception e) {
            log.warn("解析字段行失败: {}", line, e);
        }

        return fieldInfo;
    }
}

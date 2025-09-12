package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class JsonToExcelConverter {
    public static void main(String[] args) {
        String directoryPath = "src/main/resources/job_json"; // Replace with your directory path
        String outputPath = "src/main/resources/result/output.xlsx"; // Output Excel file path

        try {
            List<JobInfo> jobInfoList = processJsonFiles(directoryPath);
            createExcel(jobInfoList, outputPath);
            System.out.println("Excel file created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<JobInfo> processJsonFiles(String directoryPath) throws IOException {
        List<JobInfo> jobInfoList = new ArrayList<>();
        File directory = new File(directoryPath);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".job"));

        if (files != null) {
            ObjectMapper mapper = new ObjectMapper();

            for (File file : files) {
                JsonNode rootNode = mapper.readTree(file);
                String fileName = rootNode.get("name").asText();
                JsonNode nodesArray = rootNode.get("nodes");

                for (JsonNode node : nodesArray) {
                    JobInfo jobInfo = new JobInfo();
                    jobInfo.setFileName(fileName);
                    jobInfo.setNodeName(node.get("name").asText());
                    jobInfo.setType(node.get("type").asText());

                    // Extract properties
                    JsonNode propertiesNode = node.get("properties");
                    Map<String, String> properties = new HashMap<>();
                    if (propertiesNode != null) {
                        for (JsonNode prop : propertiesNode) {
                            properties.put(
                                    prop.get("name").asText(),
                                    prop.get("value").asText()
                            );
                        }
                    }
                    jobInfo.setProperties(properties);
                    jobInfoList.add(jobInfo);
                }
            }
        }
        return jobInfoList;
    }

    private static void createExcel(List<JobInfo> jobInfoList, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Job Information");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"File Name", "Node Name", "Type", "Properties"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Create data rows
        int rowNum = 1;
        for (JobInfo jobInfo : jobInfoList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(jobInfo.getFileName());
            row.createCell(1).setCellValue(jobInfo.getNodeName());
            row.createCell(2).setCellValue(jobInfo.getType());
            row.createCell(3).setCellValue(formatProperties(jobInfo.getProperties()));
        }

        // Autosize columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            workbook.write(outputStream);
        }
        workbook.close();
    }

    private static String formatProperties(Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }
        return sb.toString();
    }
}

class JobInfo {
    private String fileName;
    private String nodeName;
    private String type;
    private Map<String, String> properties;

    // Getters and Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) { this.properties = properties; }
}

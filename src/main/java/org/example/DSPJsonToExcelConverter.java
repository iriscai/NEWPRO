package org.example;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DSPJsonToExcelConverter {

    // 定义一个类来存储解析后的数据
    static class DatabaseInfo {
        String dataName;
        String dataNameZh;
        String dataType;
        String dataVersion;
        String jdbcUrl;
        String ip;
        String port;

        public DatabaseInfo(String dataName, String dataNameZh, String dataType, String dataVersion, String jdbcUrl, String ip, String port) {
            this.dataName = dataName;
            this.dataNameZh = dataNameZh;
            this.dataType = dataType;
            this.dataVersion = dataVersion;
            this.jdbcUrl = jdbcUrl;
            this.ip = ip;
            this.port = port;
        }
    }

    public static void main(String[] args) {
        // 指定JSON文件所在的文件夹路径
        String folderPath = "src/main/resources/DSPSOURCES/in"; // 替换为您的文件夹路径
        // 指定输出的Excel文件路径
        String excelPath = "src/main/resources/DSPSOURCES/out/DatabaseInfo.xlsx"; // 替换为您的输出路径



        // 读取并解析JSON文件
        List<DatabaseInfo> databaseInfos = parseJsonFilesInFolder(folderPath);

        // 生成Excel文件
        createExcelFile(databaseInfos, excelPath);
    }

    // 读取文件夹中的所有JSON文件并解析
    public static List<DatabaseInfo> parseJsonFilesInFolder(String folderPath) {
        List<DatabaseInfo> databaseInfos = new ArrayList<>();
        File folder = new File(folderPath);

        // 检查文件夹是否存在
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("指定的文件夹不存在或不是目录: " + folderPath);
            return databaseInfos;
        }

        // 遍历文件夹中的所有文件
        for (File file : folder.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                try {
                    // 读取JSON文件内容
                    String jsonContent = readFile(file);
                    JSONObject jsonObject = new JSONObject(jsonContent);

                    // 提取顶层字段
                    String dataName = jsonObject.optString("dataName", "");
                    String dataNameZh = jsonObject.optString("dataNameZh", "");
                    String dataType = jsonObject.optString("dataType", "");
                    String dataVersion = jsonObject.optString("dataVersion", "");

                    // 提取dataJson字段并解析为嵌套JSON
                    String dataJsonStr = jsonObject.optString("dataJson", "{}");
                    JSONObject dataJsonObject = new JSONObject(dataJsonStr);

                    // 从嵌套JSON中提取jdbcUrl
                    String jdbcUrl = dataJsonObject.optString("jdbcUrl", "");

                    // 从jdbcUrl中提取IP和端口
                    String ip = "";
                    String port = "";
                    if (jdbcUrl.contains("@")) {
                        String[] parts = jdbcUrl.split("@");
                        if (parts.length > 1) {
                            String addressPart = parts[1];
                            // 处理IP和端口，端口号可能后面还有其他内容（如服务名）
                            String[] addressParts = addressPart.split(":");
                            if (addressParts.length >= 2) {
                                ip = addressParts[0]; // 获取IP地址
                                String portPart = addressParts[1];
                                // 端口号可能包含其他内容（如服务名），通过'/'或':'分隔
                                if (portPart.contains("/")) {
                                    port = portPart.split("/")[0];
                                } else if (portPart.contains(":")) {
                                    port = portPart.split(":")[0];
                                } else {
                                    port = portPart;
                                }
                            }
                        }
                    }

                    // 将解析的数据存储到对象中
                    DatabaseInfo info = new DatabaseInfo(dataName, dataNameZh, dataType, dataVersion, jdbcUrl, ip, port);
                    databaseInfos.add(info);
                    System.out.println("成功解析文件: " + file.getName());
                } catch (Exception e) {
                    System.err.println("解析文件出错: " + file.getName() + ", 错误信息: " + e.getMessage());
                }
            }
        }

        return databaseInfos;
    }

    // 读取文件内容为字符串，确保使用UTF-8编码以支持中文
    public static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    // 创建Excel文件，处理中文乱码问题
    public static void createExcelFile(List<DatabaseInfo> databaseInfos, String excelPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Database Info");

            // 创建字体样式，设置为支持中文的字体
            Font font = workbook.createFont();
            font.setFontName("宋体"); // 使用宋体，支持中文显示
            font.setFontHeightInPoints((short) 11);

            // 创建单元格样式并应用字体
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"数据源名称", "中文名称", "数据库类型", "版本", "JDBC URL", "IP地址", "端口"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(cellStyle); // 应用支持中文的样式
            }

            // 填充数据
            for (int i = 0; i < databaseInfos.size(); i++) {
                DatabaseInfo info = databaseInfos.get(i);
                Row row = sheet.createRow(i + 1);
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(info.dataName);
                cell0.setCellStyle(cellStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(info.dataNameZh);
                cell1.setCellStyle(cellStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(info.dataType);
                cell2.setCellStyle(cellStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(info.dataVersion);
                cell3.setCellStyle(cellStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(info.jdbcUrl);
                cell4.setCellStyle(cellStyle);

                Cell cell5 = row.createCell(5);
                cell5.setCellValue(info.ip);
                cell5.setCellStyle(cellStyle);

                Cell cell6 = row.createCell(6);
                cell6.setCellValue(info.port);
                cell6.setCellStyle(cellStyle);
            }

            // 自动调整列宽，并为中文内容设置最小宽度
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最小宽度，确保中文内容不会被截断
                int columnWidth = sheet.getColumnWidth(i) / 256;
                if (columnWidth < 20) {
                    sheet.setColumnWidth(i, 20 * 256); // 设置最小宽度为20个字符
                }
            }

            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(excelPath)) {
                workbook.write(fileOut);
                System.out.println("Excel文件已成功生成: " + excelPath);
            }
        } catch (IOException e) {
            System.err.println("生成Excel文件时出错: " + e.getMessage());
        }
    }
}

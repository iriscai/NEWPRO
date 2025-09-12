/**
 * 读取CDM json文件，获取任务信息
 */
package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JobConfigToExcel {

    public static void main(String[] args) {
        try {
            // 读取文件内容
            String jsonString = new String(Files.readAllBytes(Paths.get("src/main/resources/CDMJSONS/IN/cdm_20250905101652.json")));
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray jobs = jsonObject.getJSONArray("jobs");

            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Job Configurations");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "作业名", "来源库", "来源表名", "写入库", "写入表名",
                    "来源连接器", "目标连接器", "数据覆盖方式", "组名",
                    "来源链接名", "目标链接名"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 填充数据
            for (int i = 0; i < jobs.length(); i++) {
                JSONObject job = jobs.getJSONObject(i);
                Row row = sheet.createRow(i + 1);

                // 作业名
                row.createCell(0).setCellValue(job.optString("name", "N/A"));

                // 来源库和表名
                JSONObject fromConfigValues = job.getJSONObject("from-config-values");
                JSONArray fromConfigs = fromConfigValues.getJSONArray("configs");
                JSONObject fromJobConfig = fromConfigs.getJSONObject(0);
                JSONArray fromInputs = fromJobConfig.getJSONArray("inputs");
                String fromDatabase = "N/A";
                String fromTable = "N/A";
                for (int j = 0; j < fromInputs.length(); j++) {
                    JSONObject input = fromInputs.getJSONObject(j);
                    String name = input.getString("name");
                    if (name.equals("fromJobConfig.database")) {
                        fromDatabase = input.getString("value");
                    } else if (name.equals("fromJobConfig.table")) {
                        fromTable = input.getString("value");
                    }
                }
                row.createCell(1).setCellValue(fromDatabase);
                row.createCell(2).setCellValue(fromTable);

                // 写入库和表名
                JSONObject toConfigValues = job.getJSONObject("to-config-values");
                JSONArray toConfigs = toConfigValues.getJSONArray("configs");
                JSONObject toJobConfig = toConfigs.getJSONObject(0);
                JSONArray toInputs = toJobConfig.getJSONArray("inputs");
                String toSchema = "N/A";
                String toTable = "N/A";
                String beforeImportType = "N/A";
                for (int j = 0; j < toInputs.length(); j++) {
                    JSONObject input = toInputs.getJSONObject(j);
                    String name = input.getString("name");
                    if (name.equals("toJobConfig.schemaName")) {
                        toSchema = input.getString("value");
                    } else if (name.equals("toJobConfig.tableName")) {
                        toTable = input.getString("value");
                    } else if (name.equals("toJobConfig.beforeImportType")) {
                        beforeImportType = input.getString("value");
                    }
                }
                row.createCell(3).setCellValue(toSchema);
                row.createCell(4).setCellValue(toTable);
                row.createCell(7).setCellValue(beforeImportType);

                // 连接器
                row.createCell(5).setCellValue(job.optString("from-connector-name", "N/A"));
                row.createCell(6).setCellValue(job.optString("to-connector-name", "N/A"));

                // 组名
                JSONObject driverConfigValues = job.getJSONObject("driver-config-values");
                JSONArray driverConfigs = driverConfigValues.getJSONArray("configs");
                String groupName = "N/A";
                for (int j = 0; j < driverConfigs.length(); j++) {
                    JSONObject config = driverConfigs.getJSONObject(j);
                    if (config.getString("name").equals("groupJobConfig")) {
                        JSONArray inputs = config.getJSONArray("inputs");
                        for (int k = 0; k < inputs.length(); k++) {
                            JSONObject input = inputs.getJSONObject(k);
                            if (input.getString("name").equals("groupJobConfig.groupName")) {
                                groupName = input.getString("value");
                                break;
                            }
                        }
                    }
                }
                row.createCell(8).setCellValue(groupName);

                // 来源链接名和目标链接名
                row.createCell(9).setCellValue(job.optString("from-link-name", "N/A"));
                row.createCell(10).setCellValue(job.optString("to-link-name", "N/A"));
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存Excel文件
            try (FileOutputStream fileOut = new FileOutputStream("src/main/resources/CDMJSONS/OUT/output.xlsx")) {
                workbook.write(fileOut);
            }

            // 关闭工作簿
            workbook.close();
            System.out.println("Excel文件已生成：JobConfigurations.xlsx");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("读取文件或生成Excel时出错：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("解析JSON时出错：" + e.getMessage());
        }
    }
}

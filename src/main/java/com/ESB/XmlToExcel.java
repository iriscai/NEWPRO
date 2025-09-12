package com.ESB;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class XmlToExcel {
    public static void main(String[] args) {
        try {
            // 读取 XML 文件
            File xmlFile = new File("src/main/resources/DSPZIPS/ALLLIBS/component.desc"); // 请替换为实际文件路径
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // 获取所有 brobject 节点
            NodeList brobjectList = doc.getElementsByTagName("brobject");

            // 创建 Excel 工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Brobject Data");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Category", "Description", "Nickname"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 填充数据
            for (int i = 0; i < brobjectList.getLength(); i++) {
                Element brobject = (Element) brobjectList.item(i);
                Row row = sheet.createRow(i + 1);

                // 提取 CDATA 内容
                String id = getCDataContent(brobject, "id");
                String category = getCDataContent(brobject, "category");
                String description = getCDataContent(brobject, "description");
                String nickname = getCDataContent(brobject, "nickname");

                // 写入 Excel 行
                row.createCell(0).setCellValue(id);
                row.createCell(1).setCellValue(category);
                row.createCell(2).setCellValue(description);
                row.createCell(3).setCellValue(nickname);
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存 Excel 文件
            try (FileOutputStream fileOut = new FileOutputStream("src/main/resources/DSPZIPS/RESOURCES/brobject_data.xlsx")) {
                workbook.write(fileOut);
                System.out.println("Excel 文件已生成：brobject_data.xlsx");
            }

            // 关闭工作簿
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 辅助方法：提取 CDATA 内容
    private static String getCDataContent(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }
}
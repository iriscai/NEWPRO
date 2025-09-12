package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class Brobject {
    String id;
    String category;
    String description;
    String nickname;

    public Brobject(String id, String category, String description, String nickname) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.nickname = nickname;
    }
}

public class XmlParser {
    public static void main(String[] args) {
        List<Brobject> brobjects = new ArrayList<>();
        String outputFilePath = "src/main/resources/DSPFILES/brobjects_table.xlsx"; // 指定输出文件路径
        try {
            File inputFile = new File("src/main/resources/DSPFILES/component.desc");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("brobject");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String id = getCDataContent(eElement, "id");
                    String category = getCDataContent(eElement, "category");
                    String description = getCDataContent(eElement, "description");
                    String nickname = getCDataContent(eElement, "nickname");

                    brobjects.add(new Brobject(id, category, description, nickname));
                }
            }

            // 创建输出目录
            File outputDir = new File("output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // 创建 Excel 工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Brobjects");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Category");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Nickname");

            // 写入每一行数据
            int rowNum = 1;
            for (Brobject brobject : brobjects) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(brobject.id);
                row.createCell(1).setCellValue(brobject.category);
                row.createCell(2).setCellValue(brobject.description);
                row.createCell(3).setCellValue(brobject.nickname);
            }

            // 写入 Excel 文件
            try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
                workbook.write(fileOut);
            }
            workbook.close();

            System.out.println("表格已成功写入到: " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取CDATA内容的辅助方法
    private static String getCDataContent(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                // 获取CDATA内容
                return node.getTextContent();
            }
        }
        return "";
    }
}

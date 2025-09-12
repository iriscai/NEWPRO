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

class ServiceInfo {
    String serviceSystemName;
    String serviceSystemDesc;
    String status;
    String protocol;
    String serviceName;

    public ServiceInfo(String serviceSystemName, String serviceSystemDesc, String status, String protocol, String serviceName) {
        this.serviceSystemName = serviceSystemName;
        this.serviceSystemDesc = serviceSystemDesc;
        this.status = status;
        this.protocol = protocol;
        this.serviceName = serviceName;
    }
}

public class XmlToExcelParser {
    public static void main(String[] args) {
        List<ServiceInfo> services = new ArrayList<>();
        String inputDirectoryPath = "src/main/resources/DSPZIPS/ALLLIBS/system/service"; // 输入目录
        String outputFilePath = "src/main/resources/DSPFILES/services_info.xlsx"; // 输出文件路径

        // 创建输出目录
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 读取指定目录下的所有 XML 文件
        File inputDir = new File(inputDirectoryPath);
        File[] xmlFiles = inputDir.listFiles((dir, name) -> name.endsWith(".xml"));

        if (xmlFiles != null) {
            for (File xmlFile : xmlFiles) {
                parseXmlFile(xmlFile, services);
            }
        }

        // 创建 Excel 工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Services Info");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Service System Name");
            headerRow.createCell(1).setCellValue("Service System Desc");
            headerRow.createCell(2).setCellValue("Status");
            headerRow.createCell(3).setCellValue("Protocol");
            headerRow.createCell(4).setCellValue("Service Name");

            // 写入每一行数据
            int rowNum = 1;
            for (ServiceInfo service : services) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(service.serviceSystemName);
                row.createCell(1).setCellValue(service.serviceSystemDesc);
                row.createCell(2).setCellValue(service.status);
                row.createCell(3).setCellValue(service.protocol);
                row.createCell(4).setCellValue(service.serviceName);
            }

            // 写入 Excel 文件
            try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
                workbook.write(fileOut);
            }

            System.out.println("表格已成功写入到: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseXmlFile(File xmlFile, List<ServiceInfo> services) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList serviceSystemList = doc.getElementsByTagName("servicesystem");

            for (int i = 0; i < serviceSystemList.getLength(); i++) {
                Element serviceSystem = (Element) serviceSystemList.item(i);
                String serviceSystemName = getElementValue(serviceSystem, "serviceSystemName");
                String serviceSystemDesc = getElementValue(serviceSystem, "serviceSystemDesc");
                String status = getElementValue(serviceSystem, "status");

                NodeList protocols = serviceSystem.getElementsByTagName("protocol");
                NodeList servicesList = serviceSystem.getElementsByTagName("service");

                for (int j = 0; j < servicesList.getLength(); j++) {
                    Element service = (Element) servicesList.item(j);
                    String serviceName = getElementValue(service, "serivcename");
                    String protocol = getElementValue(service, "protocol");

                    services.add(new ServiceInfo(serviceSystemName, serviceSystemDesc, status, protocol, serviceName));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getElementValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return "";
    }
}

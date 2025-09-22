package com.ESB;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.OutputKeys;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.transform.dom.DOMSource;


public class XmlExcelAggregator {

    // 定义协议数据的Excel表头（更新后，增加更多参数）
    private static final String[] PROTOCOL_HEADERS = {
            "protocolName", "protocol.http id", "ioDirection", "uri", "request method",
            "request encoding", "request action", "request connectTimeout", "response action",
            "response encoding", "response readTimeout", "serverType", "mode", "side",
            "security bidirectional", "security protocol", "keyStore path", "keyStore type",
            "keyStore keyPass", "keyStore keyMainPass", "trustStore path", "trustStore type",
            "trustStore keyPass", "advanced connPerHostCount", "advanced connectionTimeout",
            "advanced maxConnCount", "advanced readBufferSize", "advanced readTimeout",
            "advanced reuseAddress", "advanced soLinger", "advanced tcpNoDelay",
            "advanced threadCount", "advanced threshold", "advanced writeBufferSize","advanced truststoreDisable",
            "根文件名", "节点属性值校验", "校验URI列规范性"
    };

    // 定义规则数据的Excel表头
    private static final String[] RULE_HEADERS = {
            "服务ID", "中文描述", "A适配器"
    };

    // 存储解压目录与ZIP文件名的映射关系
    private static Map<String, String> unzipDirToZipName = new HashMap<>();

    public static void main(String[] args) {
        // 指定目录路径（请根据实际情况修改）
        String baseDirPath = "src/main/resources/DSPZIPS/DEVZIPS";
        String outputExcelPath = "src/main/resources/DSPZIPS/OUT/parseZipFiles.xlsx";
        String unzipBaseDir = "src/main/resources/DSPZIPS/DEVUNZIPS"; // 解压文件的根目录
        String allUnzipsDir = "src/main/resources/DSPZIPS/ALLUNZIPS"; // 聚合文件的目录
        String masterUnzipBaseDir = "src/main/resources/DSPZIPS/MASTERUNZIPS"; // 修改后文件解压根目录
        String masterZipBaseDir = "src/main/resources/DSPZIPS/MASTERSZIPS"; // 修改后压缩文件目录
        String allLibsDir = "src/main/resources/DSPZIPS/ALLLIBS"; // ALLLIBS目录，包含lib、cssystem和system文件夹
        String groupsExcelPath = "src/main/resources/DSPZIPS/RESOURCES/groups.xlsx"; // groups.xlsx文件路径
        String zipEncoding = "GBK"; // 指定ZIP文件编码，解决中文乱码问题

        try {
            // 解压指定目录下的所有ZIP文件，并记录映射关系
            System.out.println("开始解压ZIP文件...");
            unzipAllFiles(baseDirPath, unzipBaseDir, zipEncoding);
            System.out.println("ZIP文件解压完成。");

            // 获取所有符合条件的协议XML文件（上一层目录为client）
            List<File> protocolFiles = findXmlFilesInClientDirectory(unzipBaseDir, "client");
            // 获取所有符合条件的规则XML文件（路径中包含console/service/）
            List<File> ruleFiles = findXmlFilesInConsoleServiceDirectory(unzipBaseDir);

            if (protocolFiles.isEmpty() && ruleFiles.isEmpty()) {
                System.out.println("未找到符合条件的XML文件。");
                return;
            }

            // 存储协议数据
            List<String[]> protocolData = new ArrayList<>();
            // 存储规则数据
            List<String[]> ruleData = new ArrayList<>();

            // 解析协议XML文件并提取数据
            parseProtocolXmlFiles(protocolFiles, protocolData, unzipBaseDir);
            // 解析规则XML文件并提取数据
            parseRuleXmlFiles(ruleFiles, ruleData);

            // 生成Excel文件，分别写入不同Sheet
            createExcelFile(protocolData, ruleData, outputExcelPath);
            System.out.println("Excel文件已生成：" + outputExcelPath);

            // 复制所有文件到MASTERUNZIPS目录，并修改协议XML文件中的参数
            copyAndModifyFiles(unzipBaseDir, masterUnzipBaseDir, protocolFiles);

            // 复制ALLLIBS下的lib文件夹到MASTERUNZIPS下每个根文件的console目录和根目录
            // 复制ALLLIBS下的cssystem文件夹到MASTERUNZIPS下每个根文件的dev目录
            // 复制ALLLIBS下的system文件夹到MASTERUNZIPS下每个根文件的console目录
            copyAllLibsFolders(allLibsDir, masterUnzipBaseDir);

            // 重新压缩文件到MASTERSZIPS目录
            createMasterZipFiles(masterUnzipBaseDir, masterZipBaseDir);
            System.out.println("修改后的文件已压缩到：" + masterZipBaseDir);

            // 聚合MASTERUNZIPS目录下的文件到ALLUNZIPS目录下
            System.out.println("开始文件聚合到ALLUNZIPS目录...");
            aggregateFilesToAllUnzips(masterUnzipBaseDir, allUnzipsDir);
            System.out.println("文件聚合完成：" + allUnzipsDir);

            // 处理ALLUNZIPS目录下的deploy_info.xml文件
            System.out.println("开始处理deploy_info.xml文件...");
            processDeployInfoFiles(allUnzipsDir, outputExcelPath, groupsExcelPath);
            System.out.println("deploy_info.xml文件处理完成。");

            // 处理ALLUNZIPS目录下的projectDesc.xml文件
            System.out.println("开始处理projectDesc.xml文件...");
            processProjectDescFiles(allUnzipsDir, outputExcelPath, groupsExcelPath);
            System.out.println("projectDesc.xml文件处理完成。");

            // 新增：更新ALLUNZIPS/YYYY-MM-DD/console/system/service目录下的XML文件
            System.out.println("开始更新console/system/service目录下的XML文件...");
            updateServiceXmlFiles(allUnzipsDir, outputExcelPath);
            System.out.println("console/system/service目录下的XML文件更新完成。");
            // 新增：更新ALLUNZIPS/YYYY-MM-DD/patch_info.xml文件
            System.out.println("开始更新patch_info.xml文件...");
            updatePatchInfoXml(allUnzipsDir, outputExcelPath);
            System.out.println("patch_info.xml文件更新完成。");
            // 新增：更新 ALLUNZIPS/YYYY-MM-DD/component.desc 文件
            System.out.println("开始更新 component.desc 文件...");
            updateComponentDescFile(allUnzipsDir, "src/main/resources/DSPZIPS/RESOURCES/brobject_data.xlsx", outputExcelPath);
            System.out.println("component.desc 文件更新完成。");
            // 新增：更新ALLUNZIPS/YYYY-MM-DD/console/deploy_info.xml文件
            System.out.println("开始更新console/deploy_info.xml文件...");
            updateDeployInfoXml(allUnzipsDir, outputExcelPath);
            System.out.println("console/deploy_info.xml文件更新完成。");
            // 新增：压缩 ALLUNZIPS/YYYY-MM-DD 文件夹
            System.out.println("开始压缩 ALLUNZIPS/YYYY-MM-DD 文件夹...");
            compressAllUnzipsFolder(allUnzipsDir);
            System.out.println("压缩完成。");
// 新增：复制 console/system/service 文件夹到 ALLLIBS/system/service
            System.out.println("开始复制 console/system/service 文件夹...");
            copyServiceFolderToAllLibs(allUnzipsDir, allLibsDir);
            System.out.println("复制完成。");

        } catch (Exception e) {
            System.err.println("程序执行过程中发生错误：");
            e.printStackTrace();
        }
    }

    // 解压指定目录下的所有ZIP文件，每个ZIP解压到对应的目录下
    private static void unzipAllFiles(String sourceDir, String unzipBaseDir, String zipEncoding) throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        Path unzipPath = Paths.get(unzipBaseDir);
        if (!Files.exists(unzipPath)) {
            Files.createDirectories(unzipPath);
        }

        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().toLowerCase().endsWith(".zip")) {
                    String zipFileName = file.getFileName().toString();
                    String unzipDirName = zipFileName.substring(0, zipFileName.lastIndexOf("."));
                    Path unzipDir = unzipPath.resolve(unzipDirName);
                    if (!Files.exists(unzipDir)) {
                        Files.createDirectories(unzipDir);
                    }
                    // 记录解压目录与ZIP文件名的映射关系，使用规范化的路径
                    String normalizedUnzipDir = unzipDir.toFile().getCanonicalPath();
                    unzipDirToZipName.put(normalizedUnzipDir, zipFileName);
                    unzipFile(file.toFile(), unzipDir.toFile(), zipEncoding);
                    System.out.println("解压文件: " + zipFileName + " 到目录: " + unzipDir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // 解压单个ZIP文件到指定目录，支持指定编码
    private static void unzipFile(File zipFile, File destDir, String encoding) throws IOException {
        byte[] buffer = new byte[1024];
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis, java.nio.charset.Charset.forName(encoding))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // 确保父目录存在
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    // 写入文件内容
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    // 创建新文件，确保路径安全
    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    // 遍历目录，查找上一层目录为client的XML文件
    private static List<File> findXmlFilesInClientDirectory(String directoryPath, String parentDirName) throws IOException {
        return Files.walk(Paths.get(directoryPath))
                .filter(path -> {
                    String parentDir = path.getParent().getFileName().toString();
                    String fileName = path.getFileName().toString();
                    return parentDir.equals(parentDirName) && fileName.endsWith(".xml");
                })
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    // 遍历目录，查找路径中包含console/service/的XML文件
    private static List<File> findXmlFilesInConsoleServiceDirectory(String directoryPath) throws IOException {
        return Files.walk(Paths.get(directoryPath))
                .filter(path -> {
                    String pathStr = path.toString().replace(File.separator, "/");
                    String fileName = path.getFileName().toString();
                    return pathStr.contains("console/service/") && fileName.endsWith(".xml");
                })
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    // 聚合MASTERUNZIPS目录下的文件到ALLUNZIPS目录下，将第一层文件夹内容直接放入一个以日期命名的文件夹
    private static void aggregateFilesToAllUnzips(String sourceBaseDir, String targetBaseDir) throws IOException {
        Path sourcePath = Paths.get(sourceBaseDir);
        Path targetPath = Paths.get(targetBaseDir);
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
        }

        // 获取当前日期作为文件夹名称（格式：yyyy-MM-dd）
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Path dateFolderPath = targetPath.resolve(dateFolderName);
        if (!Files.exists(dateFolderPath)) {
            Files.createDirectories(dateFolderPath);
        }
        System.out.println("开始文件聚合到：" + dateFolderPath);

        // 遍历MASTERUNZIPS目录下的所有第一层文件夹
        Files.list(sourcePath).filter(Files::isDirectory).forEach(firstLevelDir -> {
            try {
                Files.walkFileTree(firstLevelDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        // 计算相对路径，排除第一层文件夹名称
                        Path relativePath = firstLevelDir.relativize(dir);
                        Path targetDir = dateFolderPath.resolve(relativePath);
                        if (!Files.exists(targetDir)) {
                            Files.createDirectories(targetDir);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        // 计算相对路径，排除第一层文件夹名称
                        Path relativePath = firstLevelDir.relativize(file);
                        Path targetFile = dateFolderPath.resolve(relativePath);
                        try {
                            Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            System.err.println("复制文件失败：" + file + "，错误信息：" + e.getMessage());
                            throw e;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        System.err.println("访问文件失败：" + file + "，错误信息：" + exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                System.err.println("处理文件夹失败：" + firstLevelDir + "，错误信息：" + e.getMessage());
            }
        });
        System.out.println("文件聚合操作完成。");

        // 新增：读取 parseZipFiles.xlsx 的 Category Sheet 页，并更新 servicesystem.xml 文件
        String excelFilePath = "src/main/resources/DSPZIPS/OUT/parseZipFiles.xlsx"; // Excel 文件路径
        // 更新路径为 ALLUNZIPS/YYYY-MM-DD/dev/cssystem/service/servicesystem.xml
        String xmlFilePath = dateFolderPath.resolve("dev/cssystem/service/servicesystem.xml").toString(); // 目标 XML 文件路径
        try {
            System.out.println("正在读取 Excel 文件：" + excelFilePath);
            List<Map<String, String>> categoryData = readCategorySheet(excelFilePath);
            if (categoryData.isEmpty()) {
                System.out.println("未从 Category Sheet 页读取到数据。");
            } else {
                System.out.println("从 Category Sheet 页读取到 " + categoryData.size() + " 条数据。");
            }
            System.out.println("正在更新 servicesystem.xml 文件：" + xmlFilePath);
            updateServicesystemXml(xmlFilePath, categoryData);
            System.out.println("servicesystem.xml 文件已成功更新：" + xmlFilePath);
        } catch (Exception e) {
            System.err.println("更新 servicesystem.xml 文件时发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 辅助方法：读取 Category Sheet 页数据（修复：过滤 null/空 channelId）
    // 辅助方法：读取 Category Sheet 页数据
    private static List<Map<String, String>> readCategorySheet(String excelFilePath) throws IOException {
        List<Map<String, String>> categoryData = new ArrayList<>();
        File excelFile = new File(excelFilePath);
        if (!excelFile.exists()) {
            System.err.println("Excel 文件不存在：" + excelFilePath);
            return categoryData;
        }
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Category");
            if (sheet == null) {
                System.out.println("未找到 Category Sheet 页");
                return categoryData;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                rowIterator.next(); // 跳过表头行
            }
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String channelId = getCellValue(row.getCell(0)); // 渠道ID (Category)
                String serviceId = getCellValue(row.getCell(1)); // 服务ID (ID 和 Nickname)
                String protocolId = getCellValue(row.getCell(2)); // 协议ID (可选)

                // 过滤：如果 serviceId 为 null 或空，跳过
                if (serviceId == null || serviceId.trim().isEmpty()) {
                    System.out.println("跳过无效行：serviceId 为空");
                    continue;
                }

                Map<String, String> entry = new HashMap<>();
                entry.put("channelId", channelId);
                entry.put("serviceId", serviceId);
                entry.put("protocolId", protocolId);
                entry.put("Category", channelId != null ? channelId : ""); // Category = channelId
                entry.put("ID", serviceId); // ID = serviceId
                entry.put("Nickname", serviceId); // Nickname = serviceId
                categoryData.add(entry);
                System.out.println("读取 Category 数据：Category=" + entry.get("Category") + ", ID=" + entry.get("ID") + ", Nickname=" + entry.get("Nickname"));
            }
        }
        return categoryData;
    }


    // 辅助方法：获取单元格值
    private static String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue()).trim();
            default:
                return null;
        }
    }
    private static void updateComponentDesc(String allUnzipsDir, String excelFilePath, String baseComponentDescPath) throws Exception {
        // 获取当前日期文件夹路径，例如 ALLUNZIPS/YYYY-MM-DD
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Path dateFolderPath = Paths.get(allUnzipsDir, dateFolderName);
        if (!Files.exists(dateFolderPath)) {
            System.err.println("日期文件夹不存在：" + dateFolderPath);
            return;
        }

        // 目标文件：ALLUNZIPS/YYYY-MM-DD/component.desc
        Path componentDescPath = dateFolderPath.resolve("component.desc");
        if (!Files.exists(componentDescPath)) {
            // 如果目标文件不存在，从基础文件复制
            Path basePath = Paths.get(baseComponentDescPath);
            if (Files.exists(basePath)) {
                Files.copy(basePath, componentDescPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("从基础文件复制 component.desc 到：" + componentDescPath);
            } else {
                System.err.println("基础文件 component.desc 不存在：" + baseComponentDescPath);
                return;
            }
        }
        System.out.println("开始更新 component.desc 文件：" + componentDescPath);

        // 读取 Category Sheet 页数据，获取 serviceId 和 channelId
        List<Map<String, String>> categoryData = readCategorySheet(excelFilePath);
        if (categoryData.isEmpty()) {
            System.out.println("未从 Category Sheet 页读取到数据。");
            return;
        }
        System.out.println("从 Category Sheet 页读取到 " + categoryData.size() + " 条数据。");

        // 读取 Rule Data Sheet 页数据，获取 serviceId 和 description
        List<Map<String, String>> ruleData = readRuleDataSheet(excelFilePath);
        if (ruleData.isEmpty()) {
            System.out.println("未从 Rule Data Sheet 页读取到数据。");
            return;
        }
        System.out.println("从 Rule Data Sheet 页读取到 " + ruleData.size() + " 条数据。");

        // 通过 serviceId 关联 Category 和 Rule Data 的数据
        Map<String, Map<String, String>> serviceMap = new HashMap<>();
        for (Map<String, String> categoryEntry : categoryData) {
            String serviceId = categoryEntry.get("serviceId");
            if (serviceId != null && !serviceId.trim().isEmpty()) {
                serviceMap.putIfAbsent(serviceId, new HashMap<>());
                serviceMap.get(serviceId).put("channelId", categoryEntry.getOrDefault("channelId", "DEFAULT"));
            }
        }
        for (Map<String, String> ruleEntry : ruleData) {
            String serviceId = ruleEntry.get("接口ID"); // 假设字段名为"接口ID"
            if (serviceId != null && !serviceId.trim().isEmpty()) {
                serviceMap.putIfAbsent(serviceId, new HashMap<>());
                serviceMap.get(serviceId).put("description", ruleEntry.getOrDefault("中文描述", serviceId + "描述"));
            }
        }

        // 过滤掉 serviceId 为空或无效的数据
        serviceMap.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().trim().isEmpty());

        // 解析 component.desc 文件
        File xmlFile = componentDescPath.toFile();
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc;
        try {
            doc = docBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            System.out.println("成功解析现有 component.desc 文件。");
        } catch (Exception e) {
            System.err.println("解析 component.desc 文件失败：" + e.getMessage());
            // 创建一个新的 XML 文档
            doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("root");
            rootElement.setAttribute("project", "NGESB");
            doc.appendChild(rootElement);
            System.out.println("创建新的 component.desc 文档。");
        }

        // 获取 root 节点
        Element rootElement = doc.getDocumentElement();
        if (rootElement == null) {
            System.err.println("component.desc 中未找到 root 节点。");
            return;
        }

        // 获取现有 brobject 节点的 id，用于去重
        Set<String> existingIds = new HashSet<>();
        NodeList brobjectList = rootElement.getElementsByTagName("brobject");
        for (int i = 0; i < brobjectList.getLength(); i++) {
            Element brobject = (Element) brobjectList.item(i);
            NodeList idNodes = brobject.getElementsByTagName("id");
            if (idNodes.getLength() > 0) {
                Element idElement = (Element) idNodes.item(0);
                String id = idElement.getTextContent().trim();
                if (!id.isEmpty()) {
                    existingIds.add(id);
                }
            }
        }
        System.out.println("现有 component.desc 文件中找到 " + existingIds.size() + " 个 brobject 节点。");

        // 仅追加新的 brobject 节点，不修改原有内容
        int addedCount = 0;
        for (Map.Entry<String, Map<String, String>> entry : serviceMap.entrySet()) {
            String serviceId = entry.getKey().trim();
            if (serviceId.isEmpty()) continue; // 跳过空 ID

            // 如果 ID 已存在，则跳过，不修改原有内容
            if (existingIds.contains(serviceId)) {
                System.out.println("跳过已有 brobject 节点：" + serviceId);
                continue;
            }

            Map<String, String> data = entry.getValue();
            String channelId = data.getOrDefault("channelId", "DEFAULT");
            String description = data.getOrDefault("description", serviceId + "描述");

            // 创建新节点并追加到 root 节点
            Element brobject = doc.createElement("brobject");
            rootElement.appendChild(brobject);

            Element idElement = doc.createElement("id");
            idElement.appendChild(doc.createCDATASection(serviceId));
            brobject.appendChild(idElement);

            Element categoryElement = doc.createElement("category");
            categoryElement.appendChild(doc.createCDATASection(channelId));
            brobject.appendChild(categoryElement);

            Element descElement = doc.createElement("description");
            descElement.appendChild(doc.createCDATASection(description));
            brobject.appendChild(descElement);

            Element nicknameElement = doc.createElement("nickname");
            nicknameElement.appendChild(doc.createCDATASection(serviceId));
            brobject.appendChild(nicknameElement);

            existingIds.add(serviceId);
            addedCount++;
            System.out.println("追加 brobject 节点：" + serviceId + "，渠道：" + channelId + "，描述：" + description);
        }

        // 保存 XML 文件
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlFile);
        transformer.transform(source, result);

        System.out.println("component.desc 文件保存成功：" + componentDescPath + "，新增节点数：" + addedCount);
    }
    // 新增方法：更新 ALLUNZIPS/YYYY-MM-DD/component.desc 文件
    private static void updateComponentDescFile(String allUnzipsDir, String brobjectDataPath, String parseZipFilesPath) {
        try {
            // 获取当前日期文件夹路径，例如 ALLUNZIPS/YYYY-MM-DD
            String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Path dateFolderPath = Paths.get(allUnzipsDir, dateFolderName);
            if (!Files.exists(dateFolderPath)) {
                System.err.println("日期文件夹不存在：" + dateFolderPath);
                return;
            }

            // 目标文件：ALLUNZIPS/YYYY-MM-DD/component.desc
            Path componentDescPath = dateFolderPath.resolve("component.desc");
            if (!Files.exists(componentDescPath)) {
                System.err.println("component.desc 文件不存在：" + componentDescPath);
                return;
            }
            System.out.println("开始更新 component.desc 文件：" + componentDescPath);

            // 读取基础数据（从 brobject_data.xlsx 或 .txt）
            List<Map<String, String>> baseData = readBrobjectData(brobjectDataPath);

            // 读取 parseZipFiles.xlsx 的 Rule Data sheet（获取 Description）
            List<Map<String, String>> ruleData = readRuleDataSheet(parseZipFilesPath);

            // 读取 parseZipFiles.xlsx 的 Category sheet（获取 Category）
            List<Map<String, String>> categoryData = readCategorySheet(parseZipFilesPath);
            // 关联数据：通过服务ID（ID/Nickname）匹配 Description 和 Category（修复：过滤 null）
            Map<String, String> descriptionMap = ruleData.stream()
                    .filter(data -> data.get("ID") != null && !data.get("ID").trim().isEmpty()
                            && data.get("Description") != null && !data.get("Description").trim().isEmpty())
                    .collect(Collectors.toMap(data -> data.get("ID"), data -> data.get("Description")));

            Map<String, String> categoryMap = categoryData.stream()
                    .filter(data -> data.get("Nickname") != null && !data.get("Nickname").trim().isEmpty()
                            && data.get("Category") != null && !data.get("Category").trim().isEmpty())
                    .collect(Collectors.toMap(data -> data.get("Nickname"), data -> data.get("Category")));

            // 合并基础数据和新增数据
            List<Map<String, String>> mergedData = new ArrayList<>();
            for (Map<String, String> base : baseData) {
                Map<String, String> merged = new HashMap<>(base);
                String id = base.get("ID");
                if (descriptionMap.containsKey(id)) {
                    merged.put("Description", descriptionMap.get(id)); // 更新 Description
                }
                if (categoryMap.containsKey(id)) {
                    merged.put("Category", categoryMap.get(id)); // 更新 Category
                }
                mergedData.add(merged);
            }

            // 解析 component.desc XML 文件
            File xmlFile = componentDescPath.toFile();
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // 获取 root 节点
            Element root = doc.getDocumentElement();

            // 获取现有 brobject 元素，用于去重
            Set<String> existingIds = new HashSet<>();
            NodeList existingBrobjects = root.getElementsByTagName("brobject");
            for (int i = 0; i < existingBrobjects.getLength(); i++) {
                Element brobject = (Element) existingBrobjects.item(i);
                String id = brobject.getElementsByTagName("id").item(0).getTextContent();
                existingIds.add(id);
            }

            // 新增 brobject 元素（去重）
            for (Map<String, String> data : mergedData) {
                String id = data.get("ID");
                if (!existingIds.contains(id)) {
                    Element brobject = doc.createElement("brobject");

                    Element idElement = doc.createElement("id");
                    idElement.setTextContent(id);
                    brobject.appendChild(idElement);

                    Element categoryElement = doc.createElement("category");
                    categoryElement.setTextContent(data.getOrDefault("Category", ""));
                    brobject.appendChild(categoryElement);

                    Element descriptionElement = doc.createElement("description");
                    descriptionElement.setTextContent(data.getOrDefault("Description", ""));
                    brobject.appendChild(descriptionElement);

                    Element nicknameElement = doc.createElement("nickname");
                    nicknameElement.setTextContent(data.getOrDefault("Nickname", ""));
                    brobject.appendChild(nicknameElement);

                    root.appendChild(brobject);
                    existingIds.add(id);
                    System.out.println("新增 brobject：" + id);
                }
            }

            // 保存 XML 文件
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xmlFile);
            transformer.transform(source, result);
            System.out.println("component.desc 文件更新成功：" + componentDescPath);

        } catch (Exception e) {
            System.err.println("更新 component.desc 文件时发生错误：" + e.getMessage());
            e.printStackTrace();
        }
        // 新增：读取 parseZipFiles.xlsx 的数据并追加到 brobject_data.xlsx
        try {
            // 读取 parseZipFiles.xlsx 的 Rule Data 和 Category sheet
            List<Map<String, String>> ruleData = readRuleDataSheet(parseZipFilesPath);
            List<Map<String, String>> categoryData = readCategorySheet(parseZipFilesPath);

            // 合并数据：通过 ID (serviceId) 匹配
            Map<String, Map<String, String>> mergedDataMap = new HashMap<>();
            for (Map<String, String> rule : ruleData) {
                String id = rule.get("ID");
                if (id != null && !id.trim().isEmpty()) {
                    mergedDataMap.putIfAbsent(id, new HashMap<>());
                    mergedDataMap.get(id).put("ID", id);
                    mergedDataMap.get(id).put("Description", rule.getOrDefault("Description", ""));
                }
            }
            for (Map<String, String> category : categoryData) {
                String serviceId = category.get("serviceId");
                if (serviceId != null && !serviceId.trim().isEmpty()) {
                    mergedDataMap.putIfAbsent(serviceId, new HashMap<>());
                    mergedDataMap.get(serviceId).put("ID", serviceId);
                    mergedDataMap.get(serviceId).put("Category", category.getOrDefault("Category", ""));
                    mergedDataMap.get(serviceId).put("Nickname", category.getOrDefault("Nickname", ""));
                    // 合并 Description（如果 ruleData 中有相同 ID）
                    if (!mergedDataMap.get(serviceId).containsKey("Description")) {
                        mergedDataMap.get(serviceId).put("Description", "");
                    }
                }
            }

            // 追加到 brobject_data.xlsx
            appendToBrobjectDataExcel(brobjectDataPath, new ArrayList<>(mergedDataMap.values()));
            System.out.println("数据追加到 brobject_data.xlsx 成功。");

        } catch (Exception e) {
            System.err.println("追加数据到 brobject_data.xlsx 时发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
    // 新增辅助方法：追加数据到 brobject_data.xlsx
    private static void appendToBrobjectDataExcel(String excelPath, List<Map<String, String>> dataToAppend) throws IOException {
        File excelFile = new File(excelPath);
        Workbook workbook;
        Sheet sheet;

        if (excelFile.exists()) {
            try (FileInputStream fis = new FileInputStream(excelFile)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0); // 假设数据在第一个 sheet
            }
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Data");
            // 添加表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Category");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Nickname");
        }

        // 获取现有 ID 集合，用于去重
        Set<String> existingIds = new HashSet<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 从第1行开始（跳过表头）
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null) {
                existingIds.add(getCellValue(row.getCell(0)));
            }
        }

        // 追加新数据（去重）
        int rowIndex = sheet.getLastRowNum() + 1;
        for (Map<String, String> data : dataToAppend) {
            String id = data.get("ID");
            if (id != null && !existingIds.contains(id)) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(id);
                row.createCell(1).setCellValue(data.getOrDefault("Category", ""));
                row.createCell(2).setCellValue(data.getOrDefault("Description", ""));
                row.createCell(3).setCellValue(data.getOrDefault("Nickname", ""));
                existingIds.add(id);
            }
        }

        // 保存文件
        try (FileOutputStream fos = new FileOutputStream(excelFile)) {
            workbook.write(fos);
        }
        workbook.close();
        System.out.println("追加 " + (rowIndex - sheet.getLastRowNum() - 1) + " 行数据到 " + excelPath);
    }
    // 辅助方法：读取基础数据（支持 .xlsx 或 .txt）
    private static List<Map<String, String>> readBrobjectData(String filePath) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();
        if (filePath.endsWith(".xlsx")) {
            // 读取 Excel 文件
            try (FileInputStream fis = new FileInputStream(filePath);
                 Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0); // 假设数据在第一个 sheet
                Iterator<Row> rowIterator = sheet.iterator();
                if (rowIterator.hasNext()) rowIterator.next(); // 跳过表头
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Map<String, String> entry = new HashMap<>();
                    entry.put("ID", getCellValue(row.getCell(0)));
                    entry.put("Category", getCellValue(row.getCell(1)));
                    entry.put("Description", getCellValue(row.getCell(2)));
                    entry.put("Nickname", getCellValue(row.getCell(3)));
                    data.add(entry);
                }
            }
        } else if (filePath.endsWith(".txt")) {
            // 解析 tab 分隔的 .txt 文件
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                String[] parts = line.split("\t");
                if (parts.length >= 4) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("ID", parts[0]);
                    entry.put("Category", parts[1]);
                    entry.put("Description", parts[2]);
                    entry.put("Nickname", parts[3]);
                    data.add(entry);
                }
            }
        }
        return data;
    }
    // 读取 Rule Data Sheet 页数据

    // 辅助方法：读取 Rule Data sheet
    private static List<Map<String, String>> readRuleDataSheet(String excelPath) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Rule Data");
            if (sheet == null) return data;
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // 跳过表头
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> entry = new HashMap<>();
                entry.put("ID", getCellValue(row.getCell(0))); // 服务ID
                entry.put("Description", getCellValue(row.getCell(1))); // 中文描述
                data.add(entry);
            }
        }
        return data;
    }
    private static void updatePatchInfoXml(String allUnzipsDir, String excelFilePath) throws Exception {
        // 获取当前日期文件夹路径，例如 ALLUNZIPS/YYYY-MM-DD
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Path dateFolderPath = Paths.get(allUnzipsDir, dateFolderName);
        if (!Files.exists(dateFolderPath)) {
            System.err.println("日期文件夹不存在：" + dateFolderPath);
            return;
        }

        // 目标文件：ALLUNZIPS/YYYY-MM-DD/patch_info.xml
        Path patchInfoPath = dateFolderPath.resolve("patch_info.xml");
        if (!Files.exists(patchInfoPath)) {
            System.err.println("patch_info.xml 文件不存在：" + patchInfoPath);
            return;
        }
        System.out.println("开始更新 patch_info.xml 文件：" + patchInfoPath);

        // 读取 Category Sheet 页数据
        List<Map<String, String>> categoryData = readCategorySheet(excelFilePath);
        if (categoryData.isEmpty()) {
            System.out.println("未从 Category Sheet 页读取到数据。");
            return;
        }
        System.out.println("从 Category Sheet 页读取到 " + categoryData.size() + " 条数据。");

        // 按 channelId 分组 categoryData
        Map<String, List<Map<String, String>>> groupedData = categoryData.stream()
                .collect(Collectors.groupingBy(data -> data.get("channelId")));

        // 解析 patch_info.xml 文件
        File xmlFile = patchInfoPath.toFile();
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // 获取 components 节点
        Element componentsElement = (Element) doc.getElementsByTagName("components").item(0);
        if (componentsElement == null) {
            System.err.println("patch_info.xml 中未找到 components 节点。");
            return;
        }

        // 获取 business 和 protocol_client 的 component 节点
        NodeList componentList = componentsElement.getElementsByTagName("component");
        Element businessComponent = null;
        Element protocolComponent = null;
        for (int i = 0; i < componentList.getLength(); i++) {
            Element component = (Element) componentList.item(i);
            String type = component.getAttribute("type");
            if ("business".equals(type)) {
                businessComponent = component;
            } else if ("protocol_client".equals(type)) {
                protocolComponent = component;
            }
        }

        if (businessComponent == null) {
            System.out.println("未找到 business component 节点，正在创建...");
            businessComponent = doc.createElement("component");
            businessComponent.setAttribute("type", "business");
            componentsElement.appendChild(businessComponent);
        }

        if (protocolComponent == null) {
            System.out.println("未找到 protocol_client component 节点，正在创建...");
            protocolComponent = doc.createElement("component");
            protocolComponent.setAttribute("type", "protocol_client");
            componentsElement.appendChild(protocolComponent);
        }

        // 更新 business 和 protocol_client 节点
        for (Map.Entry<String, List<Map<String, String>>> entry : groupedData.entrySet()) {
            String channelId = entry.getKey();
            List<Map<String, String>> systemData = entry.getValue();

            // 更新 business component 中的 category
            Element businessCategory = findOrCreateCategory(doc, businessComponent, channelId, "/"+channelId);
            Set<String> existingBusinessItems = getExistingItems(businessCategory);
            for (Map<String, String> data : systemData) {
                String serviceId = data.get("serviceId");
                if (!existingBusinessItems.contains(serviceId)) {
                    Element itemElement = doc.createElement("item");
                    itemElement.setAttribute("id", serviceId);
                    itemElement.setAttribute("location", "2");
                    itemElement.setAttribute("path", "\\dev\\services\\business\\" + channelId + "\\" + serviceId + ".biz");
                    businessCategory.appendChild(itemElement);
                    System.out.println("添加业务服务：" + serviceId + " 到渠道：" + channelId);
                    existingBusinessItems.add(serviceId);
                }
            }

            // 更新 protocol_client component 中的 category
            Element protocolCategory = findOrCreateCategory(doc, protocolComponent, channelId, "/DEFAULT");
            Set<String> existingProtocolItems = getExistingItems(protocolCategory);
            for (Map<String, String> data : systemData) {
                String protocolId = data.get("protocolId");
                String itemId = protocolId; // 假设 protocolId 格式为 CHANNELID_SERVICEID
                if (!existingProtocolItems.contains(itemId)) {
                    Element itemElement = doc.createElement("item");
                    itemElement.setAttribute("id", itemId);
                    itemElement.setAttribute("location", "2");
                    itemElement.setAttribute("path", "\\dev\\protocol\\client\\" + protocolId + ".HTTPServiceConnector");
                    protocolCategory.appendChild(itemElement);
                    System.out.println("添加协议客户端：" + itemId + " 到渠道：" + channelId);
                    existingProtocolItems.add(itemId);
                }
            }
        }

        // 保存 XML 文件，避免使用 Transformer 可能导致的问题
        StringWriter writer = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        // 将内容写入文件，确保使用 UTF-8 编码
        String xmlContent = writer.toString();
        Files.write(patchInfoPath, xmlContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("patch_info.xml 文件保存成功（使用直接字符串写入方式）：" + patchInfoPath);
    }

    // 查找或创建 category 节点
    private static Element findOrCreateCategory(Document doc, Element component, String categoryId, String path) {
        NodeList categoryList = component.getElementsByTagName("category");
        for (int i = 0; i < categoryList.getLength(); i++) {
            Element category = (Element) categoryList.item(i);
            if (categoryId.equals(category.getAttribute("id"))) {
                return category;
            }
        }
        // 如果未找到，创建新的 category
        Element newCategory = doc.createElement("category");
        newCategory.setAttribute("id", categoryId);
        newCategory.setAttribute("path", path);
        component.appendChild(newCategory);
        System.out.println("创建新的 category 节点：" + categoryId);
        return newCategory;
    }

    // 获取 category 中已存在的 item id 列表，用于去重
    private static Set<String> getExistingItems(Element category) {
        Set<String> existingItems = new HashSet<>();
        if (category != null) {
            NodeList itemList = category.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);
                existingItems.add(item.getAttribute("id"));
            }
        }
        return existingItems;
    }
    private static void updateDeployInfoXml(String allUnzipsDir, String excelFilePath) throws Exception {
        // 获取当前日期文件夹路径，例如 ALLUNZIPS/YYYY-MM-DD
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Path dateFolderPath = Paths.get(allUnzipsDir, dateFolderName);
        if (!Files.exists(dateFolderPath)) {
            System.err.println("日期文件夹不存在：" + dateFolderPath);
            return;
        }

        // 目标文件：ALLUNZIPS/YYYY-MM-DD/console/deploy_info.xml
        Path deployInfoPath = dateFolderPath.resolve("console/deploy_info.xml");
        if (!Files.exists(deployInfoPath)) {
            System.err.println("deploy_info.xml 文件不存在：" + deployInfoPath);
            return;
        }
        System.out.println("开始更新 deploy_info.xml 文件：" + deployInfoPath);

        // 读取 Category Sheet 页数据
        List<Map<String, String>> categoryData = readCategorySheet(excelFilePath);
        if (categoryData.isEmpty()) {
            System.out.println("未从 Category Sheet 页读取到数据。");
            return;
        }
        System.out.println("从 Category Sheet 页读取到 " + categoryData.size() + " 条数据。");

        // 解析 deploy_info.xml 文件
        File xmlFile = deployInfoPath.toFile();
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // 获取 objs 节点
        Element objsElement = (Element) doc.getElementsByTagName("objs").item(0);
        if (objsElement == null) {
            System.err.println("deploy_info.xml 中未找到 objs 节点。");
            return;
        }

        // 获取 protocol.client, service.buss, service.definition 的 obj 节点
        NodeList objList = objsElement.getElementsByTagName("obj");
        Element protocolObj = null;
        Element serviceBussObj = null;
        Element serviceDefObj = null;
        for (int i = 0; i < objList.getLength(); i++) {
            Element obj = (Element) objList.item(i);
            Element objType = (Element) obj.getElementsByTagName("objType").item(0);
            if (objType != null) {
                String typeValue = objType.getTextContent();
                if ("protocol.client".equals(typeValue)) {
                    protocolObj = obj;
                } else if ("service.buss".equals(typeValue)) {
                    serviceBussObj = obj;
                } else if ("service.definition".equals(typeValue)) {
                    serviceDefObj = obj;
                }
            }
        }

        // 如果未找到对应节点，则创建
        if (protocolObj == null) {
            System.out.println("未找到 protocol.client obj 节点，正在创建...");
            protocolObj = doc.createElement("obj");
            Element objType = doc.createElement("objType");
            objType.setTextContent("protocol.client");
            protocolObj.appendChild(objType);
            Element items = doc.createElement("items");
            protocolObj.appendChild(items);
            objsElement.appendChild(protocolObj);
        }

        if (serviceBussObj == null) {
            System.out.println("未找到 service.buss obj 节点，正在创建...");
            serviceBussObj = doc.createElement("obj");
            Element objType = doc.createElement("objType");
            objType.setTextContent("service.buss");
            serviceBussObj.appendChild(objType);
            Element items = doc.createElement("items");
            serviceBussObj.appendChild(items);
            objsElement.appendChild(serviceBussObj);
        }

        if (serviceDefObj == null) {
            System.out.println("未找到 service.definition obj 节点，正在创建...");
            serviceDefObj = doc.createElement("obj");
            Element objType = doc.createElement("objType");
            objType.setTextContent("service.definition");
            serviceDefObj.appendChild(objType);
            Element items = doc.createElement("items");
            serviceDefObj.appendChild(items);
            objsElement.appendChild(serviceDefObj);
        }

        // 获取 global 节点下的 service.definition
        Element globalElement = (Element) doc.getElementsByTagName("global").item(0);
        Element globalServiceDefObj = null;
        if (globalElement != null) {
            NodeList globalObjList = globalElement.getElementsByTagName("obj");
            for (int i = 0; i < globalObjList.getLength(); i++) {
                Element obj = (Element) globalObjList.item(i);
                Element objType = (Element) obj.getElementsByTagName("objType").item(0);
                if (objType != null && "service.definition".equals(objType.getTextContent())) {
                    globalServiceDefObj = obj;
                    break;
                }
            }
        }
        if (globalServiceDefObj == null) {
            System.out.println("未找到 global service.definition obj 节点，正在创建...");
            if (globalElement == null) {
                globalElement = doc.createElement("global");
                doc.getDocumentElement().appendChild(globalElement);
            }
            globalServiceDefObj = doc.createElement("obj");
            Element objType = doc.createElement("objType");
            objType.setTextContent("service.definition");
            globalServiceDefObj.appendChild(objType);
            Element items = doc.createElement("items");
            globalServiceDefObj.appendChild(items);
            globalElement.appendChild(globalServiceDefObj);
        }

        // 获取现有项，用于去重
        Set<String> existingProtocols = getExistingItemsFromObj(protocolObj);
        Set<String> existingServicesBuss = getExistingItemsFromObj(serviceBussObj);
        Set<String> existingServicesDef = getExistingItemsFromObj(serviceDefObj);
        Set<String> existingGlobalServicesDef = getExistingItemsFromGlobalObj(globalServiceDefObj);

        // 更新 protocol.client, service.buss, service.definition 和 global service.definition
        for (Map<String, String> data : categoryData) {
            String serviceId = data.get("serviceId");
            String protocolId = data.get("protocolId");

            // 更新 protocol.client
            if (!existingProtocols.contains(protocolId)) {
                Element itemElement = doc.createElement("item");
                itemElement.setAttribute("cmd", "add");
                itemElement.setTextContent(protocolId);
                Element itemsElement = (Element) protocolObj.getElementsByTagName("items").item(0);
                itemsElement.appendChild(itemElement);
                existingProtocols.add(protocolId);
                System.out.println("添加协议客户端：" + protocolId);
            }

            // 更新 service.buss
            if (!existingServicesBuss.contains(serviceId)) {
                Element itemElement = doc.createElement("item");
                itemElement.setAttribute("cmd", "add");
                itemElement.setTextContent(serviceId);
                Element itemsElement = (Element) serviceBussObj.getElementsByTagName("items").item(0);
                itemsElement.appendChild(itemElement);
                existingServicesBuss.add(serviceId);
                System.out.println("添加业务服务：" + serviceId);
            }

            // 更新 service.definition
            if (!existingServicesDef.contains(serviceId)) {
                Element itemElement = doc.createElement("item");
                itemElement.setAttribute("cmd", "add");
                itemElement.setTextContent(serviceId);
                Element itemsElement = (Element) serviceDefObj.getElementsByTagName("items").item(0);
                itemsElement.appendChild(itemElement);
                existingServicesDef.add(serviceId);
                System.out.println("添加服务定义：" + serviceId);
            }

            // 更新 global service.definition
            String globalItem = "console/service/definition/service_" + serviceId + ".xml";
            if (!existingGlobalServicesDef.contains(globalItem)) {
                Element itemElement = doc.createElement("item");
                itemElement.setTextContent(globalItem);
                Element itemsElement = (Element) globalServiceDefObj.getElementsByTagName("items").item(0);
                itemsElement.appendChild(itemElement);
                existingGlobalServicesDef.add(globalItem);
                System.out.println("添加全局服务定义文件：" + globalItem);
            }
        }

        // 保存 XML 文件，避免使用 Transformer 可能导致的问题
        StringWriter writer = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        // 将内容写入文件，确保使用 UTF-8 编码
        String xmlContent = writer.toString();
        Files.write(deployInfoPath, xmlContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("deploy_info.xml 文件保存成功（使用直接字符串写入方式）：" + deployInfoPath);
    }

    // 从 obj 节点获取现有项，用于去重
    private static Set<String> getExistingItemsFromObj(Element obj) {
        Set<String> existingItems = new HashSet<>();
        if (obj != null) {
            Element itemsElement = (Element) obj.getElementsByTagName("items").item(0);
            if (itemsElement != null) {
                NodeList itemList = itemsElement.getElementsByTagName("item");
                for (int i = 0; i < itemList.getLength(); i++) {
                    existingItems.add(itemList.item(i).getTextContent());
                }
            }
        }
        return existingItems;
    }

    // 从 global obj 节点获取现有项，用于去重
    private static Set<String> getExistingItemsFromGlobalObj(Element obj) {
        Set<String> existingItems = new HashSet<>();
        if (obj != null) {
            Element itemsElement = (Element) obj.getElementsByTagName("items").item(0);
            if (itemsElement != null) {
                NodeList itemList = itemsElement.getElementsByTagName("item");
                for (int i = 0; i < itemList.getLength(); i++) {
                    existingItems.add(itemList.item(i).getTextContent());
                }
            }
        }
        return existingItems;
    }

//    private static void updateServiceXmlFiles(String allUnzipsDir, String excelFilePath) throws Exception {
//        // 获取当前日期文件夹路径，例如 ALLUNZIPS/YYYY-MM-DD
//        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
//        Path dateFolderPath = Paths.get(allUnzipsDir, dateFolderName);
//        if (!Files.exists(dateFolderPath)) {
//            System.err.println("日期文件夹不存在：" + dateFolderPath);
//            return;
//        }
//
//        // 目标目录：ALLUNZIPS/YYYY-MM-DD/console/system/service
//        Path serviceDirPath = dateFolderPath.resolve("console/system/service");
//        if (!Files.exists(serviceDirPath)) {
//            System.err.println("服务目录不存在：" + serviceDirPath);
//            return;
//        }
//        System.out.println("开始更新服务目录下的XML文件：" + serviceDirPath);
//
//        // 读取 Category Sheet 页数据
//        List<Map<String, String>> categoryData = readCategorySheet(excelFilePath);
//        if (categoryData.isEmpty()) {
//            System.out.println("未从 Category Sheet 页读取到数据。");
//            return;
//        }
//        System.out.println("从 Category Sheet 页读取到 " + categoryData.size() + " 条数据。");
//
//        // 按 channelId 分组 categoryData
//        Map<String, List<Map<String, String>>> groupedData = categoryData.stream()
//                .collect(Collectors.groupingBy(data -> data.get("channelId")));
//
//        // 遍历服务目录下的所有 XML 文件
//        Files.walkFileTree(serviceDirPath, new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                if (file.toString().toLowerCase().endsWith(".xml")) {
//                    String fileName = file.getFileName().toString();
//                    String channelId = fileName.substring(0, fileName.lastIndexOf(".xml"));
//                    System.out.println("处理XML文件：" + fileName + "，对应的渠道ID：" + channelId);
//
//                    // 检查是否有对应渠道ID的数据
//                    List<Map<String, String>> systemData = groupedData.get(channelId);
//                    if (systemData != null && !systemData.isEmpty()) {
//                        try {
//                            updateSingleServiceXml(file.toString(), systemData);
//                        } catch (Exception e) {
//                            System.err.println("更新XML文件失败：" + fileName + "，错误信息：" + e.getMessage());
//                            e.printStackTrace();
//                        }
//                    } else {
//                        System.out.println("未找到对应渠道ID的数据，跳过文件：" + fileName);
//                    }
//                }
//                return FileVisitResult.CONTINUE;
//            }
//        });
//
//        System.out.println("服务目录下的XML文件更新完成：" + serviceDirPath);
//    }
    private static void updateServiceXmlFiles(String allUnzipsDir, String excelFilePath) throws Exception {
        // 获取当前日期文件夹路径，例如 ALLUNZIPS/YYYY-MM-DD
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Path dateFolderPath = Paths.get(allUnzipsDir, dateFolderName);
        if (!Files.exists(dateFolderPath)) {
            System.err.println("日期文件夹不存在：" + dateFolderPath);
            return;
        }

        // 目标目录：ALLUNZIPS/YYYY-MM-DD/console/system/service
        Path serviceDirPath = dateFolderPath.resolve("console/system/service");
        if (!Files.exists(serviceDirPath)) {
            System.err.println("服务目录不存在：" + serviceDirPath);
            return;
        }
        System.out.println("开始更新服务目录下的XML文件：" + serviceDirPath);

        // 读取 Category Sheet 页数据
        List<Map<String, String>> categoryData = readCategorySheet(excelFilePath);
        if (categoryData.isEmpty()) {
            System.out.println("未从 Category Sheet 页读取到数据。");
            return;
        }
        System.out.println("从 Category Sheet 页读取到 " + categoryData.size() + " 条数据。");

        // 按 channelId 分组 categoryData
        Map<String, List<Map<String, String>>> groupedData = categoryData.stream()
                .collect(Collectors.groupingBy(data -> data.get("channelId")));

        // 遍历所有渠道 ID，为每个渠道确保有一个 XML 文件（如果不存在则创建）
        for (Map.Entry<String, List<Map<String, String>>> entry : groupedData.entrySet()) {
            String channelId = entry.getKey();
            List<Map<String, String>> systemData = entry.getValue();

            if (systemData == null || systemData.isEmpty()) {
                System.out.println("跳过渠道：" + channelId + "，无数据");
                continue;
            }

            // 构造 XML 文件路径：渠道ID.xml
            String xmlFileName = channelId + ".xml";
            Path xmlFilePath = serviceDirPath.resolve(xmlFileName);
            String xmlFilePathStr = xmlFilePath.toString();

            System.out.println("处理渠道：" + channelId + "，文件路径：" + xmlFilePathStr);

            // 调用 updateSingleServiceXml 来更新或创建文件
            try {
                updateSingleServiceXml(xmlFilePathStr, systemData);
            } catch (Exception e) {
                System.err.println("更新或创建XML文件失败：" + xmlFileName + "，错误信息：" + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("服务目录下的XML文件更新完成：" + serviceDirPath);
    }

    // 更新单个服务 XML 文件
    private static void updateSingleServiceXml(String xmlFilePath, List<Map<String, String>> systemData) throws Exception {
        File xmlFile = new File(xmlFilePath);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc;

        if (xmlFile.exists()) {
            System.out.println("找到现有的服务 XML 文件，正在解析：" + xmlFilePath);
            doc = docBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
        } else {
            System.out.println("未找到服务 XML 文件，正在创建新文件：" + xmlFilePath);
            doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("servicesystem");
            doc.appendChild(rootElement);

            // 初始化基本结构，参考提供的 AQSCJCSYS.xml
            Element systemName = doc.createElement("serviceSystemName");
            systemName.setTextContent(systemData.get(0).get("channelId"));
            rootElement.appendChild(systemName);

            Element systemDesc = doc.createElement("serviceSystemDesc");
            systemDesc.setTextContent(systemData.get(0).get("channelId") + " 系统");
            rootElement.appendChild(systemDesc);

            Element hostName = doc.createElement("hostName");
            hostName.setTextContent("");
            rootElement.appendChild(hostName);

            Element hostIp = doc.createElement("hostIp");
            hostIp.setTextContent("");
            rootElement.appendChild(hostIp);

            Element status = doc.createElement("status");
            status.setTextContent("add");
            rootElement.appendChild(status);

            Element protocols = doc.createElement("protocols");
            rootElement.appendChild(protocols);

            Element params = doc.createElement("params");
            rootElement.appendChild(params);

            Element services = doc.createElement("services");
            rootElement.appendChild(services);
        }

        Element root = doc.getDocumentElement();
        String channelId = systemData.get(0).get("channelId");

        // 获取现有节点的 hostName 和 hostIp 值，确保不被覆盖
        Element hostName = (Element) root.getElementsByTagName("hostName").item(0);
        Element hostIp = (Element) root.getElementsByTagName("hostIp").item(0);
        String hostNameValue = hostName != null ? hostName.getTextContent() : "";
        String hostIpValue = hostIp != null ? hostIp.getTextContent() : "";
        System.out.println("保留现有系统：" + channelId + ", hostName=" + hostNameValue + ", hostIp=" + hostIpValue);

        // 确保 hostName 和 hostIp 存在并保留原始值
        if (hostName == null) {
            hostName = doc.createElement("hostName");
            hostName.setTextContent("");
            root.appendChild(hostName);
        } else {
            hostName.setTextContent(hostNameValue);
        }

        if (hostIp == null) {
            hostIp = doc.createElement("hostIp");
            hostIp.setTextContent("");
            root.appendChild(hostIp);
        } else {
            hostIp.setTextContent(hostIpValue);
        }

        // 获取 protocols 和 services 节点
        Element protocolsElement = (Element) root.getElementsByTagName("protocols").item(0);
        if (protocolsElement == null) {
            protocolsElement = doc.createElement("protocols");
            root.appendChild(protocolsElement);
        }

        Element servicesElement = (Element) root.getElementsByTagName("services").item(0);
        if (servicesElement == null) {
            servicesElement = doc.createElement("services");
            root.appendChild(servicesElement);
        }

        // 获取已存在的 protocol 和 service 名称，用于去重
        NodeList existingProtocols = protocolsElement.getElementsByTagName("protocol");
        Set<String> existingProtocolSet = new HashSet<>();
        for (int i = 0; i < existingProtocols.getLength(); i++) {
            existingProtocolSet.add(existingProtocols.item(i).getTextContent());
        }

        NodeList existingServices = servicesElement.getElementsByTagName("service");
        Set<String> existingServiceSet = new HashSet<>();
        for (int i = 0; i < existingServices.getLength(); i++) {
            Element service = (Element) existingServices.item(i);
            String serviceName = service.getElementsByTagName("serivcename").item(0).getTextContent();
            existingServiceSet.add(serviceName);
        }

        // 添加新的 protocol 和 service
        for (Map<String, String> data : systemData) {
            String protocolId = data.get("protocolId");
            String serviceId = data.get("serviceId");

            // 添加 protocol，如果不存在
            if (!existingProtocolSet.contains(protocolId)) {
                Element protocolElement = doc.createElement("protocol");
                protocolElement.setTextContent(protocolId);
                protocolsElement.appendChild(protocolElement);
                existingProtocolSet.add(protocolId);
                System.out.println("添加协议：" + protocolId + " 到系统：" + channelId);
            }

            // 添加 service，如果不存在
            if (!existingServiceSet.contains(serviceId)) {
                Element serviceElement = doc.createElement("service");
                Element serviceNameElement = doc.createElement("serivcename");
                serviceNameElement.setTextContent(serviceId);
                Element protocolElement = doc.createElement("protocol");
                protocolElement.setTextContent(protocolId);
                serviceElement.appendChild(serviceNameElement);
                serviceElement.appendChild(protocolElement);
                servicesElement.appendChild(serviceElement);
                existingServiceSet.add(serviceId);
                System.out.println("添加服务：" + serviceId + " 到系统：" + channelId + "，关联协议：" + protocolId);
            }
        }

        // 保存 XML 文件，避免使用 Transformer 可能导致的问题
        StringWriter writer = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        // 将内容写入文件，确保使用 UTF-8 编码
        String xmlContent = writer.toString();
        Files.write(Paths.get(xmlFilePath), xmlContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("XML 文件保存成功（使用直接字符串写入方式）：" + xmlFilePath);
    }

    private static void updateServicesystemXml(String xmlFilePath, List<Map<String, String>> categoryData) throws Exception {
        File xmlFile = new File(xmlFilePath);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder(); // 修改这里
        Document doc;

        // 确保父目录存在
        File parentDir = xmlFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
            System.out.println("创建目录：" + parentDir.getPath());
        }

        if (xmlFile.exists()) {
            System.out.println("找到现有的 servicesystem.xml 文件，正在解析：" + xmlFilePath);
            doc = docBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
        } else {
            System.out.println("未找到 servicesystem.xml 文件，正在创建新文件：" + xmlFilePath);
            doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("servicesystems");
            doc.appendChild(rootElement);
        }

        Element root = doc.getDocumentElement();

        Map<String, List<Map<String, String>>> groupedData = categoryData.stream()
                .filter(data -> data.get("channelId") != null && !data.get("channelId").trim().isEmpty()) // 过滤 null/空
                .collect(Collectors.groupingBy(data -> data.get("channelId")));
        // 获取现有的 serviceSystem 节点
        NodeList serviceSystems = root.getElementsByTagName("servicesystem");
        Map<String, Element> existingSystems = new HashMap<>();
        for (int i = 0; i < serviceSystems.getLength(); i++) {
            Element system = (Element) serviceSystems.item(i);
            String systemName = system.getElementsByTagName("serviceSystemName").item(0).getTextContent();
            existingSystems.put(systemName, system);
        }

        // 为每个 channelId 创建或更新 serviceSystem 节点
        for (Map.Entry<String, List<Map<String, String>>> entry : groupedData.entrySet()) {
            String channelId = entry.getKey();
            List<Map<String, String>> systemData = entry.getValue();
            Element systemElement = existingSystems.get(channelId);

            if (systemElement == null) {
                // 如果不存在，创建新的 serviceSystem 节点
                System.out.println("创建新系统：" + channelId);
                systemElement = doc.createElement("servicesystem");
                root.appendChild(systemElement);

                Element systemName = doc.createElement("serviceSystemName");
                systemName.setTextContent(channelId);
                systemElement.appendChild(systemName);

                Element systemDesc = doc.createElement("serviceSystemDesc");
                systemDesc.setTextContent(channelId + " 系统");
                systemElement.appendChild(systemDesc);

                Element hostName = doc.createElement("hostName");
                hostName.setTextContent("");
                systemElement.appendChild(hostName);

                Element hostIp = doc.createElement("hostIp");
                hostIp.setTextContent("");
                systemElement.appendChild(hostIp);

                Element status = doc.createElement("status");
                status.setTextContent("add");
                systemElement.appendChild(status);

                Element protocols = doc.createElement("protocols");
                systemElement.appendChild(protocols);

                Element params = doc.createElement("params");
                systemElement.appendChild(params);

                Element services = doc.createElement("services");
                systemElement.appendChild(services);
            }

            // 获取 protocols 和 services 节点
            Element protocolsElement = (Element) systemElement.getElementsByTagName("protocols").item(0);
            Element servicesElement = (Element) systemElement.getElementsByTagName("services").item(0);

            // 获取已存在的 protocol 和 service 名称，用于去重
            NodeList existingProtocols = protocolsElement.getElementsByTagName("protocol");
            Set<String> existingProtocolSet = new HashSet<>();
            for (int i = 0; i < existingProtocols.getLength(); i++) {
                existingProtocolSet.add(existingProtocols.item(i).getTextContent());
            }

            NodeList existingServices = servicesElement.getElementsByTagName("service");
            Set<String> existingServiceSet = new HashSet<>();
            for (int i = 0; i < existingServices.getLength(); i++) {
                Element service = (Element) existingServices.item(i);
                String serviceName = service.getElementsByTagName("serivcename").item(0).getTextContent();
                existingServiceSet.add(serviceName);
            }

            // 添加新的 protocol 和 service
            for (Map<String, String> data : systemData) {
                String protocolId = data.get("protocolId");
                String serviceId = data.get("serviceId");

                // 添加 protocol，如果不存在
                if (!existingProtocolSet.contains(protocolId)) {
                    Element protocolElement = doc.createElement("protocol");
                    protocolElement.setTextContent(protocolId);
                    protocolsElement.appendChild(protocolElement);
                    existingProtocolSet.add(protocolId);
                    System.out.println("添加协议：" + protocolId + " 到系统：" + channelId);
                }

                // 添加 service，如果不存在
                if (!existingServiceSet.contains(serviceId)) {
                    Element serviceElement = doc.createElement("service");
                    Element serviceNameElement = doc.createElement("serivcename");
                    serviceNameElement.setTextContent(serviceId);
                    Element protocolElement = doc.createElement("protocol");
                    protocolElement.setTextContent(protocolId);
                    serviceElement.appendChild(serviceNameElement);
                    serviceElement.appendChild(protocolElement);
                    servicesElement.appendChild(serviceElement);
                    existingServiceSet.add(serviceId);
                    System.out.println("添加服务：" + serviceId + " 到系统：" + channelId + "，关联协议：" + protocolId);
                }
            }
        }

        // 保存 XML 文件，保持与原文件一致的格式
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no"); // 禁用缩进
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no"); // 移除 standalone 属性

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new FileOutputStream(xmlFilePath));
        transformer.transform(source, result);
        System.out.println("XML 文件保存成功：" + xmlFilePath);
    }

    // 处理ALLUNZIPS目录下的projectDesc.xml文件
    private static void processProjectDescFiles(String allUnzipsDir, String outputExcelPath, String groupsExcelPath) throws Exception {
        List<File> projectDescFiles = findProjectDescFiles(allUnzipsDir);
        if (projectDescFiles.isEmpty()) {
            System.out.println("未在ALLUNZIPS目录下找到projectDesc.xml文件。");
            return;
        }

        // 从parseZipFiles.xlsx文件中读取协议ID
        List<String> protocolIds = readProtocolIdsFromExcel(outputExcelPath);

        // 从groups.xlsx文件中读取渠道ID
        List<String> channelIds = readChannelIdsFromExcel(groupsExcelPath);

        for (File projectDescFile : projectDescFiles) {
            updateProjectDescFile(projectDescFile, protocolIds, channelIds);
        }
    }

    // 在ALLUNZIPS目录下查找projectDesc.xml文件
    private static List<File> findProjectDescFiles(String directoryPath) throws IOException {
        return Files.walk(Paths.get(directoryPath))
                .filter(path -> path.getFileName().toString().equals("projectDesc.xml"))
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    // 从groups.xlsx文件中读取渠道ID
    private static List<String> readChannelIdsFromExcel(String excelPath) throws IOException {
        List<String> channelIds = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("SystemDesc"); // 读取名为SystemDesc的Sheet页
            if (sheet == null) {
                System.out.println("未找到名为'SystemDesc'的工作表。");
                return channelIds;
            }

            int channelIdColumnIndex = -1;
            Row headerRow = sheet.getRow(0);
            for (Cell cell : headerRow) {
                if (cell.getStringCellValue().equals("渠道ID")) { // 读取“渠道ID”列
                    channelIdColumnIndex = cell.getColumnIndex();
                    break;
                }
            }

            if (channelIdColumnIndex == -1) {
                System.out.println("未找到'渠道ID'列。");
                return channelIds;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell cell = row.getCell(channelIdColumnIndex);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String channelId = cell.getStringCellValue().trim();
                        if (!channelId.isEmpty()) {
                            channelIds.add(channelId);
                        }
                    }
                }
            }
        }
        System.out.println("从groups.xlsx文件的SystemDesc Sheet页中读取了 " + channelIds.size() + " 个渠道ID。");
        return channelIds;
    }

    // 更新projectDesc.xml文件
    private static void updateProjectDescFile(File projectDescFile, List<String> protocolIds, List<String> channelIds) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(projectDescFile);
        doc.getDocumentElement().normalize();

        // 更新buildInfo节点中的buildDate和compileDate为当前日期
        NodeList buildInfoList = doc.getElementsByTagName("buildInfo");
        if (buildInfoList.getLength() > 0) {
            Element buildInfo = (Element) buildInfoList.item(0);
            String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            NodeList buildDateList = buildInfo.getElementsByTagName("buildDate");
            if (buildDateList.getLength() > 0) {
                buildDateList.item(0).setTextContent(currentDate);
            }
            NodeList compileDateList = buildInfo.getElementsByTagName("compileDate");
            if (compileDateList.getLength() > 0) {
                compileDateList.item(0).setTextContent(currentDate);
            }
        }

        // 获取components节点下的component节点
        NodeList componentList = doc.getElementsByTagName("component");
        Element businessComponent = null;
        Element protocolComponent = null;
        for (int i = 0; i < componentList.getLength(); i++) {
            Element component = (Element) componentList.item(i);
            String type = component.getAttribute("type");
            if (type.equals("business")) {
                businessComponent = component;
            } else if (type.equals("protocol_client")) {
                protocolComponent = component;
            }
        }

        // 更新business component中的category和item节点
        if (businessComponent != null) {
            // 移除旧的category节点
            NodeList oldCategoryList = businessComponent.getElementsByTagName("category");
            for (int i = oldCategoryList.getLength() - 1; i >= 0; i--) {
                businessComponent.removeChild(oldCategoryList.item(i));
            }

            // 按protocolIds创建新的category和item节点
            Map<String, List<String>> groupedProtocols = new HashMap<>();
            for (String protocolId : protocolIds) {
                String[] parts = protocolId.split("_");
                if (parts.length == 2) {
                    String group = parts[0];
                    String serviceName = parts[1];
                    groupedProtocols.computeIfAbsent(group, k -> new ArrayList<>()).add(serviceName);
                }
            }

            for (Map.Entry<String, List<String>> entry : groupedProtocols.entrySet()) {
                String group = entry.getKey();
                List<String> services = entry.getValue();
                Element category = doc.createElement("category");
                category.setAttribute("id", group);
                category.setAttribute("path", "/" + group);

                for (String serviceName : services) {
                    Element item = doc.createElement("item");
                    item.setAttribute("id", serviceName);
                    item.setAttribute("location", "2");
                    item.setAttribute("path", "\\\\dev\\\\services\\\\business\\\\" + group + "\\\\" + serviceName + ".biz");
                    category.appendChild(item);
                }
                businessComponent.appendChild(category);
            }
        }

        // 更新protocol_client component中的item节点
        if (protocolComponent != null) {
            NodeList categoryList = protocolComponent.getElementsByTagName("category");
            if (categoryList.getLength() > 0) {
                Element category = (Element) categoryList.item(0);
                NodeList itemList = category.getElementsByTagName("item");
                // 移除旧的item节点
                for (int i = itemList.getLength() - 1; i >= 0; i--) {
                    category.removeChild(itemList.item(i));
                }
                // 添加新的item节点（基于protocolIds）
                for (String protocolId : protocolIds) {
                    Element item = doc.createElement("item");
                    item.setAttribute("id", protocolId);
                    item.setAttribute("location", "2");
                    item.setAttribute("path", "\\\\dev\\\\protocol\\\\client\\\\" + protocolId + ".HTTPServiceConnector");
                    category.appendChild(item);
                }
            }
        }

        // 将更新后的XML写入文件
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(projectDescFile);
        transformer.transform(source, result);
        System.out.println("已更新文件：" + projectDescFile.getAbsolutePath());
    }

    // 处理ALLUNZIPS目录下的deploy_info.xml文件
    private static void processDeployInfoFiles(String allUnzipsDir, String outputExcelPath, String groupsExcelPath) throws Exception {
        List<File> deployInfoFiles = findDeployInfoFiles(allUnzipsDir);
        if (deployInfoFiles.isEmpty()) {
            System.out.println("未在ALLUNZIPS目录下找到deploy_info.xml文件。");
            return;
        }

        // 从parseZipFiles.xlsx文件中读取协议ID
        List<String> protocolIds = readProtocolIdsFromExcel(outputExcelPath);

        // 从groups.xlsx文件中读取系统服务名称
        List<String> systemServices = readSystemServicesFromExcel(groupsExcelPath);

        for (File deployInfoFile : deployInfoFiles) {
            rewriteDeployInfoFile(deployInfoFile, protocolIds, systemServices);
        }
    }

    // 在ALLUNZIPS目录下查找deploy_info.xml文件
    private static List<File> findDeployInfoFiles(String directoryPath) throws IOException {
        return Files.walk(Paths.get(directoryPath))
                .filter(path -> path.getFileName().toString().equals("deploy_info.xml"))
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    // 从parseZipFiles.xlsx文件中读取协议ID
    private static List<String> readProtocolIdsFromExcel(String excelPath) throws IOException {
        List<String> protocolIds = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Protocol Data");
            if (sheet == null) {
                System.out.println("未找到名为'Protocol Data'的工作表。");
                return protocolIds;
            }

            int protocolIdColumnIndex = -1;
            Row headerRow = sheet.getRow(0);
            for (Cell cell : headerRow) {
                if (cell.getStringCellValue().equals("协议id")) {
                    protocolIdColumnIndex = cell.getColumnIndex();
                    break;
                }
            }

            if (protocolIdColumnIndex == -1) {
                System.out.println("未找到'协议id'列。");
                return protocolIds;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell cell = row.getCell(protocolIdColumnIndex);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String protocolId = cell.getStringCellValue().trim();
                        if (!protocolId.isEmpty()) {
                            protocolIds.add(protocolId);
                        }
                    }
                }
            }
        }
        System.out.println("从Excel文件中读取了 " + protocolIds.size() + " 个协议ID。");
        return protocolIds;
    }

    // 从groups.xlsx文件中读取渠道ID
    private static List<String> readSystemServicesFromExcel(String excelPath) throws IOException {
        List<String> systemServices = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("SystemDesc"); // 读取名为SystemDesc的Sheet页
            if (sheet == null) {
                System.out.println("未找到名为'SystemDesc'的工作表。");
                return systemServices;
            }

            int channelIdColumnIndex = -1;
            Row headerRow = sheet.getRow(0);
            for (Cell cell : headerRow) {
                if (cell.getStringCellValue().equals("渠道ID")) { // 读取“渠道ID”列
                    channelIdColumnIndex = cell.getColumnIndex();
                    break;
                }
            }

            if (channelIdColumnIndex == -1) {
                System.out.println("未找到'渠道ID'列。");
                return systemServices;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell cell = row.getCell(channelIdColumnIndex);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String channelId = cell.getStringCellValue().trim();
                        if (!channelId.isEmpty()) {
                            systemServices.add(channelId);
                        }
                    }
                }
            }
        }
        System.out.println("从groups.xlsx文件的SystemDesc Sheet页中读取了 " + systemServices.size() + " 个渠道ID。");
        return systemServices;
    }

    // 重写deploy_info.xml文件
    private static void rewriteDeployInfoFile(File deployInfoFile, List<String> protocolIds, List<String> systemServices) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // 创建根节点
        Element rootElement = doc.createElement("objs");
        doc.appendChild(rootElement);

        // 创建protocol.client节点
        Element protocolObj = doc.createElement("obj");
        rootElement.appendChild(protocolObj);
        Element protocolObjType = doc.createElement("objType");
        protocolObjType.setTextContent("protocol.client");
        protocolObj.appendChild(protocolObjType);
        Element protocolItems = doc.createElement("items");
        protocolObj.appendChild(protocolItems);
        for (String protocolId : protocolIds) {
            Element item = doc.createElement("item");
            item.setAttribute("cmd", "add");
            item.setTextContent(protocolId);
            protocolItems.appendChild(item);
        }

        // 创建service.buss节点
        Element serviceBussObj = doc.createElement("obj");
        rootElement.appendChild(serviceBussObj);
        Element serviceBussObjType = doc.createElement("objType");
        serviceBussObjType.setTextContent("service.buss");
        serviceBussObj.appendChild(serviceBussObjType);
        Element serviceBussItems = doc.createElement("items");
        serviceBussObj.appendChild(serviceBussItems);
        for (String protocolId : protocolIds) {
            String serviceName = protocolId.contains("_") ? protocolId.substring(protocolId.indexOf("_") + 1) : protocolId;
            Element item = doc.createElement("item");
            item.setAttribute("cmd", "add");
            item.setTextContent(serviceName);
            serviceBussItems.appendChild(item);
        }

        // 创建service.definition节点
        Element serviceDefObj = doc.createElement("obj");
        rootElement.appendChild(serviceDefObj);
        Element serviceDefObjType = doc.createElement("objType");
        serviceDefObjType.setTextContent("service.definition");
        serviceDefObj.appendChild(serviceDefObjType);
        Element serviceDefItems = doc.createElement("items");
        serviceDefObj.appendChild(serviceDefItems);
        for (String protocolId : protocolIds) {
            String serviceName = protocolId.contains("_") ? protocolId.substring(protocolId.indexOf("_") + 1) : protocolId;
            Element item = doc.createElement("item");
            item.setAttribute("cmd", "add");
            item.setTextContent(serviceName);
            serviceDefItems.appendChild(item);
        }

        // 创建global节点
        Element globalElement = doc.createElement("global");
        rootElement.appendChild(globalElement);
        Element globalObj = doc.createElement("obj");
        globalElement.appendChild(globalObj);
        Element globalObjType = doc.createElement("objType");
        globalObjType.setTextContent("service.definition");
        globalObj.appendChild(globalObjType);
        Element globalItems = doc.createElement("items");
        globalObj.appendChild(globalItems);
        for (String protocolId : protocolIds) {
            String serviceName = protocolId.contains("_") ? protocolId.substring(protocolId.indexOf("_") + 1) : protocolId;
            Element item = doc.createElement("item");
            item.setTextContent("console/service/definition/service_" + serviceName + ".xml");
            globalItems.appendChild(item);
        }

        // 创建system.service节点
        Element systemServiceObj = doc.createElement("obj");
        rootElement.appendChild(systemServiceObj);
        Element systemServiceObjType = doc.createElement("objType");
        systemServiceObjType.setTextContent("system.service");
        systemServiceObj.appendChild(systemServiceObjType);
        Element systemServiceItems = doc.createElement("items");
        systemServiceObj.appendChild(systemServiceItems);
        for (String serviceName : systemServices) {
            Element item = doc.createElement("item");
            item.setAttribute("cmd", "add");
            item.setTextContent(serviceName);
            systemServiceItems.appendChild(item);
        }

        // 将XML写入文件
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(deployInfoFile);
        transformer.transform(source, result);
        System.out.println("已重写文件：" + deployInfoFile.getAbsolutePath());
    }

    // 根据文件路径获取根文件名（最外层ZIP文件名）
    private static String getRootZipName(String filePath, String unzipBaseDir) {
        try {
            // 获取规范化的文件路径和解压根目录路径
            String normalizedFilePath = new File(filePath).getCanonicalPath();
            String normalizedUnzipBaseDir = new File(unzipBaseDir).getCanonicalPath();

            // 检查文件路径是否以解压根目录开头
            if (normalizedFilePath.startsWith(normalizedUnzipBaseDir)) {
                // 提取解压根目录后的相对路径
                String relativePath = normalizedFilePath.substring(normalizedUnzipBaseDir.length());
                // 去除可能的前缀斜杠或反斜杠
                relativePath = relativePath.replaceFirst("^[/\\\\\\\\]+", "");

                // 找到第一级目录名（对应解压后的ZIP目录名）
                int separatorIndex = relativePath.indexOf(File.separator);
                if (separatorIndex > 0) {
                    String rootDirName = relativePath.substring(0, separatorIndex);
                    // 构建第一级目录的完整路径
                    String rootDirPath = new File(normalizedUnzipBaseDir, rootDirName).getCanonicalPath();
                    // 从映射中获取ZIP文件名
                    String zipName = unzipDirToZipName.get(rootDirPath);
                    return zipName != null ? zipName : "未知ZIP文件（映射未找到）";
                } else {
                    return "未知ZIP文件（无第一级目录）";
                }
            } else {
                return "未知ZIP文件（路径不匹配）";
            }
        } catch (IOException e) {
            System.err.println("获取ZIP文件名时发生错误: " + e.getMessage());
            return "未知ZIP文件（路径解析错误）";
        }
    }

    // 解析协议XML文件并提取数据
// 解析协议XML文件并提取数据（更新后，提取更多参数）
    private static void parseProtocolXmlFiles(List<File> xmlFiles, List<String[]> protocolData, String unzipBaseDir) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        for (File file : xmlFiles) {
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            // 获取protocol.http节点
            NodeList nodeList = doc.getElementsByTagName("protocol.http");
            if (nodeList.getLength() > 0) {
                Element element = (Element) nodeList.item(0);
                String[] rowData = new String[PROTOCOL_HEADERS.length];

                // 提取protocol.http节点的属性
                rowData[0] = element.getAttribute("protocolName"); // protocolName
                rowData[1] = element.getAttribute("id"); // protocol.http id
                rowData[2] = element.getAttribute("ioDirection"); // ioDirection
                rowData[4] = element.getAttribute("mode"); // mode
                rowData[5] = element.getAttribute("side"); // side

                // 提取common节点的属性
                NodeList commonList = doc.getElementsByTagName("common");
                if (commonList.getLength() > 0) {
                    Element commonElement = (Element) commonList.item(0);
                    rowData[3] = commonElement.getAttribute("uri"); // uri
                    rowData[6] = commonElement.getAttribute("serverType"); // serverType
                }

                // 提取request节点的属性
                NodeList requestList = doc.getElementsByTagName("request");
                if (requestList.getLength() > 0) {
                    Element requestElement = (Element) requestList.item(0);
                    rowData[7] = requestElement.getAttribute("method"); // request method
                    rowData[8] = requestElement.getAttribute("encoding"); // request encoding
                    rowData[9] = requestElement.getAttribute("action"); // request action
                    rowData[10] = requestElement.getAttribute("connectTimeout"); // request connectTimeout
                }

                // 提取response节点的属性
                NodeList responseList = doc.getElementsByTagName("response");
                if (responseList.getLength() > 0) {
                    Element responseElement = (Element) responseList.item(0);
                    rowData[11] = responseElement.getAttribute("action"); // response action
                    rowData[12] = responseElement.getAttribute("encoding"); // response encoding
                    rowData[13] = responseElement.getAttribute("readTimeout"); // response readTimeout
                }

                // 提取security节点的属性
                NodeList securityList = doc.getElementsByTagName("security");
                if (securityList.getLength() > 0) {
                    Element securityElement = (Element) securityList.item(0);
                    rowData[14] = securityElement.getAttribute("bidirectional"); // security bidirectional
                    rowData[15] = securityElement.getAttribute("protocol"); // security protocol

                    // 提取keyStore节点的属性
                    NodeList keyStoreList = doc.getElementsByTagName("keyStore");
                    if (keyStoreList.getLength() > 0) {
                        Element keyStoreElement = (Element) keyStoreList.item(0);
                        rowData[16] = keyStoreElement.getAttribute("path"); // keyStore path
                        rowData[17] = keyStoreElement.getAttribute("type"); // keyStore type
                        rowData[18] = keyStoreElement.getAttribute("keyPass"); // keyStore keyPass
                        rowData[19] = keyStoreElement.getAttribute("keyMainPass"); // keyStore keyMainPass
                    }

                    // 提取trustStore节点的属性
                    NodeList trustStoreList = doc.getElementsByTagName("trustStore");
                    if (trustStoreList.getLength() > 0) {
                        Element trustStoreElement = (Element) trustStoreList.item(0);
                        rowData[20] = trustStoreElement.getAttribute("path"); // trustStore path
                        rowData[21] = trustStoreElement.getAttribute("type"); // trustStore type
                        rowData[22] = trustStoreElement.getAttribute("keyPass"); // trustStore keyPass
                    }
                }

                // 提取advanced节点的属性
                NodeList advancedList = doc.getElementsByTagName("advanced");
                if (advancedList.getLength() > 0) {
                    Element advancedElement = (Element) advancedList.item(0);
                    rowData[23] = advancedElement.getAttribute("connPerHostCount"); // advanced connPerHostCount
                    rowData[24] = advancedElement.getAttribute("connectionTimeout"); // advanced connectionTimeout
                    rowData[25] = advancedElement.getAttribute("maxConnCount"); // advanced maxConnCount
                    rowData[26] = advancedElement.getAttribute("readBufferSize"); // advanced readBufferSize
                    rowData[27] = advancedElement.getAttribute("readTimeout"); // advanced readTimeout
                    rowData[28] = advancedElement.getAttribute("reuseAddress"); // advanced reuseAddress
                    rowData[29] = advancedElement.getAttribute("soLinger"); // advanced soLinger
                    rowData[30] = advancedElement.getAttribute("tcpNoDelay"); // advanced tcpNoDelay
                    rowData[31] = advancedElement.getAttribute("threadCount"); // advanced threadCount
                    rowData[32] = advancedElement.getAttribute("threshold"); // advanced threshold
                    rowData[33] = advancedElement.getAttribute("writeBufferSize"); // advanced writeBufferSize
                    rowData[34] = advancedElement.getAttribute("truststoreDisable"); // advanced writeBufferSize
                }

// 提取压缩包根目录文件夹名（从文件路径中获取，确保获取的是DEVUNZIPS目录下的第一层文件夹名）
                String filePath = file.getAbsolutePath();
                String zipRootDirName = "未知压缩包根目录"; // 默认值，防止获取失败时为空
                try {
                    // 检查路径中是否包含DEVUNZIPS
                    String devUnzipsMarker = "DEVUNZIPS";
                    if (filePath.contains(devUnzipsMarker)) {
                        // 获取DEVUNZIPS之后的路径部分
                        String relativePath = filePath.substring(filePath.indexOf(devUnzipsMarker) + devUnzipsMarker.length() + 1);
                        // 如果相对路径中包含分隔符，提取第一个分隔符之前的部分作为压缩包根目录文件夹名
                        if (relativePath.contains(File.separator)) {
                            zipRootDirName = relativePath.substring(0, relativePath.indexOf(File.separator));
                        } else {
                            // 如果没有分隔符，说明文件直接在DEVUNZIPS目录下，将相对路径作为根目录名
                            zipRootDirName = relativePath;
                        }
                    } else {
                        // 如果路径中不包含DEVUNZIPS，尝试从路径中提取可能的根目录名
                        String[] pathParts = filePath.split(File.separator);
                        if (pathParts.length > 1) {
                            // 取路径中的倒数第二部分作为可能的根目录名
                            zipRootDirName = pathParts[pathParts.length - 2];
                        } else if (pathParts.length > 0) {
                            zipRootDirName = pathParts[pathParts.length - 1];
                        }
                    }
                } catch (Exception e) {
                    // 捕获异常，确保即使路径处理出错也能继续执行
                    zipRootDirName = "未知压缩包根目录 (路径处理异常: " + e.getMessage() + ")";
                }
                rowData[35] = zipRootDirName; // 压缩包根目录文件夹名

                // 校验节点属性值
                rowData[36] = validateAdvancedAndChildAttributes(doc); // 节点属性值校验

                // 校验URI列规范性
                String uri = rowData[3];
                rowData[37] = isValidUri(uri) ? "符合" : "不符合"; // 校验URI列规范性

                protocolData.add(rowData);
            }
        }
    }

    // 校验advanced节点及其所有子节点的属性值是否为数值或true/false，不得有空格或其他字符串
    private static String validateAdvancedAndChildAttributes(Document doc) {
        StringBuilder result = new StringBuilder();
        NodeList advancedList = doc.getElementsByTagName("advanced");
        boolean hasInvalidAttributes = false;

        if (advancedList.getLength() > 0) {
            Element advancedElement = (Element) advancedList.item(0);

            // 校验advanced节点本身的属性
            NamedNodeMap advancedAttributes = advancedElement.getAttributes();
            for (int i = 0; i < advancedAttributes.getLength(); i++) {
                String attrName = advancedAttributes.item(i).getNodeName();
                String attrValue = advancedAttributes.item(i).getNodeValue();

                // 跳过空值属性（如果需要校验空值，可以取消此条件）
                if (attrValue == null || attrValue.isEmpty()) {
                    hasInvalidAttributes = true;
                    result.append(String.format("节点 advanced 属性 %s: 空值; ", attrName));
                    continue;
                }

                // 检查是否包含空格
                if (attrValue.contains(" ")) {
                    hasInvalidAttributes = true;
                    result.append(String.format("节点 advanced 属性 %s: 包含空格 (值: '%s'); ", attrName, attrValue));
                    continue;
                }

                // 检查是否为true/false（不区分大小写）
                if (!attrValue.equalsIgnoreCase("true") && !attrValue.equalsIgnoreCase("false")) {
                    // 如果不是布尔值，检查是否为数值
                    try {
                        Long.parseLong(attrValue);
                    } catch (NumberFormatException e) {
                        hasInvalidAttributes = true;
                        result.append(String.format("节点 advanced 属性 %s: 既非数值也非布尔值 (值: '%s'); ", attrName, attrValue));
                    }
                }
            }

// 校验advanced节点下的所有子节点
            NodeList childNodes = advancedElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i) instanceof Element) {
                    Element childElement = (Element) childNodes.item(i);
                    String childNodeName = childElement.getNodeName();
                    NamedNodeMap childAttributes = childElement.getAttributes();

                    for (int j = 0; j < childAttributes.getLength(); j++) {
                        String attrName = childAttributes.item(j).getNodeName();
                        String attrValue = childAttributes.item(j).getNodeValue();

                        // 跳过空值属性（如果需要校验空值，可以取消此条件）
                        if (attrValue == null || attrValue.isEmpty()) {
                            hasInvalidAttributes = true;
                            result.append(String.format("节点 %s 属性 %s: 空值; ", childNodeName, attrName));
                            continue;
                        }

                        // 检查是否包含空格
                        if (attrValue.contains(" ")) {
                            hasInvalidAttributes = true;
                            result.append(String.format("节点 %s 属性 %s: 包含空格 (值: '%s'); ", childNodeName, attrName, attrValue));
                            continue;
                        }

                        // 检查是否为true/false（不区分大小写）
                        if (!attrValue.equalsIgnoreCase("true") && !attrValue.equalsIgnoreCase("false")) {
                            // 如果不是布尔值，检查是否为数值
                            try {
                                Long.parseLong(attrValue);
                            } catch (NumberFormatException e) {
                                hasInvalidAttributes = true;
                                result.append(String.format("节点 %s 属性 %s: 既非数值也非布尔值 (值: '%s'); ", childNodeName, attrName, attrValue));
                            }
                        }
                    }
                }
            }  } else {
            return "无advanced节点";
        }

        if (hasInvalidAttributes) {
            return "不符合 - " + result.toString();
        } else {
            return "符合";
        }
    }

    // 检查URI是否规范（不包含空格或乱码）
    private static boolean isValidUri(String uri) {
        // 检查是否包含空格
        if (uri.contains(" ")) {
            return false;
        }
        // 检查是否包含非 printable ASCII 字符（视为乱码）
        for (char c : uri.toCharArray()) {
            if (c < 32 || c > 126) {
                return false;
            }
        }
        return true;
    }

    // 解析规则XML文件并提取数据
    private static void parseRuleXmlFiles(List<File> xmlFiles, List<String[]> ruleData) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        for (File file : xmlFiles) {
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            // 获取root节点下的id节点
            NodeList idList = doc.getElementsByTagName("id");
            if (idList.getLength() > 0) {
                String[] rowData = new String[RULE_HEADERS.length];

                // 提取id
                rowData[0] = idList.item(0).getTextContent();

                // 提取description
                NodeList descList = doc.getElementsByTagName("description");
                if (descList.getLength() > 0) {
                    rowData[1] = descList.item(0).getTextContent();
                }

                // 提取adapterFlow
                NodeList adapterList = doc.getElementsByTagName("ref");
                if (adapterList.getLength() > 0) {
                    rowData[2] = adapterList.item(0).getTextContent();
                }

                ruleData.add(rowData);
            }
        }
    }

    // 生成Excel文件，分别写入不同Sheet
    private static void createExcelFile(List<String[]> protocolData, List<String[]> ruleData, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // 创建协议数据的Sheet
        Sheet protocolSheet = workbook.createSheet("Protocol Data");
        // 创建表头
        Row protocolHeaderRow = protocolSheet.createRow(0);
        for (int i = 0; i < PROTOCOL_HEADERS.length; i++) {
            Cell cell = protocolHeaderRow.createCell(i);
            cell.setCellValue(PROTOCOL_HEADERS[i]);
        }
        // 填充协议数据
        for (int i = 0; i < protocolData.size(); i++) {
            Row row = protocolSheet.createRow(i + 1);
            String[] rowData = protocolData.get(i);
            for (int j = 0; j < rowData.length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(rowData[j] != null ? rowData[j] : "");
            }
        }
        // 自动调整列宽
        for (int i = 0; i < PROTOCOL_HEADERS.length; i++) {
            protocolSheet.autoSizeColumn(i);
        }

        // 创建规则数据的Sheet
        Sheet ruleSheet = workbook.createSheet("Rule Data");
        // 创建表头
        Row ruleHeaderRow = ruleSheet.createRow(0);
        for (int i = 0; i < RULE_HEADERS.length; i++) {
            Cell cell = ruleHeaderRow.createCell(i);
            cell.setCellValue(RULE_HEADERS[i]);
        }
        // 填充规则数据
        for (int i = 0; i < ruleData.size(); i++) {
            Row row = ruleSheet.createRow(i + 1);
            String[] rowData = ruleData.get(i);
            for (int j = 0; j < rowData.length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(rowData[j] != null ? rowData[j] : "");
            }
        }
        // 自动调整列宽
        for (int i = 0; i < RULE_HEADERS.length; i++) {
            ruleSheet.autoSizeColumn(i);
        }

        // 创建Category数据的Sheet
        Sheet categorySheet = workbook.createSheet("Category");
        // 创建表头
        String[] categoryHeaders = {"渠道ID", "服务ID", "协议ID"};
        Row categoryHeaderRow = categorySheet.createRow(0);
        for (int i = 0; i < categoryHeaders.length; i++) {
            Cell cell = categoryHeaderRow.createCell(i);
            cell.setCellValue(categoryHeaders[i]);
        }
        // 填充Category数据
        int rowIndex = 1;
        for (String[] row : protocolData) {
            String protocolId = row[1]; // protocol.http id 在 PROTOCOL_HEADERS 的第2列，索引为1
            if (protocolId != null && !protocolId.isEmpty() && protocolId.contains("_")) {
                String[] parts = protocolId.split("_");
                if (parts.length == 2) {
                    String channelId = parts[0]; // 渠道ID，_ 前面的部分
                    String serviceId = parts[1]; // 服务ID，_ 后面的部分
                    Row categoryRow = categorySheet.createRow(rowIndex++);
                    categoryRow.createCell(0).setCellValue(channelId);
                    categoryRow.createCell(1).setCellValue(serviceId);
                    categoryRow.createCell(2).setCellValue(protocolId);
                }
            }
        }
        // 自动调整列宽
        for (int i = 0; i < categoryHeaders.length; i++) {
            categorySheet.autoSizeColumn(i);
        }

        // 写入文件
        try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    // 复制文件到MASTERUNZIPS目录，并修改协议XML文件中的参数
    private static void copyAndModifyFiles(String sourceBaseDir, String targetBaseDir, List<File> protocolFiles) throws Exception {
        Path sourcePath = Paths.get(sourceBaseDir);
        Path targetPath = Paths.get(targetBaseDir);
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
        }
        // 复制所有文件，保持原路径结构
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = targetPath.resolve(sourcePath.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
        // 修改协议XML文件中的参数
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        for (File originalFile : protocolFiles) {
            try {
                // 获取原始文件的规范化路径
                String originalFilePath = originalFile.getCanonicalPath();
                String sourceBaseDirPath = new File(sourceBaseDir).getCanonicalPath();

                // 计算相对路径
                if (!originalFilePath.startsWith(sourceBaseDirPath)) {
                    System.err.println("文件路径不在源目录下: " + originalFilePath);
                    continue;
                }

                String relativePathStr = originalFilePath.substring(sourceBaseDirPath.length());
                relativePathStr = relativePathStr.replaceFirst("^[/\\\\\\\\]+", "");
                Path relativePath = Paths.get(relativePathStr);
                Path targetFilePath = Paths.get(targetBaseDir).resolve(relativePath);

                // 解析XML文件
                Document doc = builder.parse(targetFilePath.toFile());
                doc.getDocumentElement().normalize();

                // 获取response节点的readTimeout属性并修改
                NodeList responseList = doc.getElementsByTagName("response");
                if (responseList.getLength() > 0) {
                    Element responseElement = (Element) responseList.item(0);
                    String readTimeout = responseElement.getAttribute("readTimeout");
                    try {
                        long rt = readTimeout.isEmpty() ? 0 : Long.parseLong(readTimeout.trim());
                        if (rt != 10000) {
                            responseElement.setAttribute("readTimeout", "10000");
                        }
                    } catch (NumberFormatException e) {
                        responseElement.setAttribute("readTimeout", "10000");
                    }
                }

                // 获取advanced节点并直接替换为指定内容
                NodeList advancedList = doc.getElementsByTagName("advanced");
                if (advancedList.getLength() > 0) {
                    Element advancedElement = (Element) advancedList.item(0);
                    // 直接设置advanced节点的属性为指定内容
                    advancedElement.setAttribute("connPerHostCount", "200");
                    advancedElement.setAttribute("connectionTimeout", "10000");
                    advancedElement.setAttribute("maxConnCount", "2000");
                    advancedElement.setAttribute("readBufferSize", "2048");
                    advancedElement.setAttribute("readTimeout", "58000");
                    advancedElement.setAttribute("reuseAddress", "true");
                    advancedElement.setAttribute("soLinger", "0");
                    advancedElement.setAttribute("tcpNoDelay", "true");
                    advancedElement.setAttribute("threadCount", "50");
                    advancedElement.setAttribute("threshold", "524288");
                    advancedElement.setAttribute("writeBufferSize", "2048");
                }

                // 将修改后的XML写回文件
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(targetFilePath.toFile());
                transformer.transform(source, result);
                System.out.println("已修改XML文件: " + targetFilePath);
            } catch (Exception e) {
                System.err.println("处理文件时出错: " + originalFile.getAbsolutePath() + ", 错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 复制ALLLIBS下的lib、cssystem和system文件夹到MASTERUNZIPS下每个根文件的指定目录
    private static void copyAllLibsFolders(String allLibsDir, String masterUnzipBaseDir) throws IOException {
        Path allLibsPath = Paths.get(allLibsDir);
        Path masterUnzipPath = Paths.get(masterUnzipBaseDir);

        if (!Files.exists(allLibsPath)) {
            System.out.println("ALLLIBS目录不存在: " + allLibsDir);
            return;
        }

        // 遍历MASTERUNZIPS下的每个一级目录（对应每个根文件）
        Files.list(masterUnzipPath).filter(Files::isDirectory).forEach(rootDir -> {
            try {
                // 复制lib文件夹到console目录下
                Path libSourcePath = allLibsPath.resolve("lib");
                if (Files.exists(libSourcePath)) {
                    // 复制到console/lib
                    Path libConsoleTargetPath = rootDir.resolve("console").resolve("lib");
                    if (!Files.exists(libConsoleTargetPath.getParent())) {
                        Files.createDirectories(libConsoleTargetPath.getParent());
                    }
                    copyFolder(libSourcePath, libConsoleTargetPath);

                    // 复制到根目录/lib
                    Path libRootTargetPath = rootDir.resolve("lib");
                    copyFolder(libSourcePath, libRootTargetPath);
                }

                // 复制cssystem文件夹到dev目录下
                Path cssystemSourcePath = allLibsPath.resolve("cssystem");
                if (Files.exists(cssystemSourcePath)) {
                    Path cssystemTargetPath = rootDir.resolve("dev").resolve("cssystem");
                    if (!Files.exists(cssystemTargetPath.getParent())) {
                        Files.createDirectories(cssystemTargetPath.getParent());
                    }
                    copyFolder(cssystemSourcePath, cssystemTargetPath);
                }

                // 复制system文件夹到console目录下
                Path systemSourcePath = allLibsPath.resolve("system");
                if (Files.exists(systemSourcePath)) {
                    Path systemTargetPath = rootDir.resolve("console").resolve("system");
                    if (!Files.exists(systemTargetPath.getParent())) {
                        Files.createDirectories(systemTargetPath.getParent());
                    }
                    copyFolder(systemSourcePath, systemTargetPath);
                }
            } catch (IOException e) {
                System.err.println("复制文件夹到根目录失败: " + rootDir + ", 错误: " + e.getMessage());
            }
        });
        System.out.println("ALLLIBS文件夹复制完成。");
    }
    // 新增方法：压缩 ALLUNZIPS/YYYY-MM-DD 文件夹为 ZIP 文件
    private static void compressAllUnzipsFolder(String allUnzipsDir) throws IOException {
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Path sourceFolder = Paths.get(allUnzipsDir, dateFolderName);
        Path zipFilePath = Paths.get(allUnzipsDir, dateFolderName + ".zip");

        if (!Files.exists(sourceFolder)) {
            System.err.println("源文件夹不存在：" + sourceFolder);
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            Files.walkFileTree(sourceFolder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = sourceFolder.relativize(file);
                    ZipEntry zipEntry = new ZipEntry(relativePath.toString().replace("\\", "/"));
                    zos.putNextEntry(zipEntry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        System.out.println("压缩成功：" + zipFilePath);
    }
    // 新增方法：复制 ALLUNZIPS/YYYY-MM-DD/console/system/service 到 ALLLIBS/system/service
    private static void copyServiceFolderToAllLibs(String allUnzipsDir, String allLibsDir) throws IOException {
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Path sourceFolder = Paths.get(allUnzipsDir, dateFolderName, "console", "system", "service");
        Path targetFolder = Paths.get(allLibsDir, "system", "service");

        if (!Files.exists(sourceFolder)) {
            System.err.println("源文件夹不存在：" + sourceFolder);
            return;
        }

        if (!Files.exists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }

        Files.walkFileTree(sourceFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relativePath = sourceFolder.relativize(dir);
                Path targetDir = targetFolder.resolve(relativePath);
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = sourceFolder.relativize(file);
                Path targetFile = targetFolder.resolve(relativePath);
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("复制成功：" + sourceFolder + " -> " + targetFolder);
    }
    // 复制文件夹及其内容
    private static void copyFolder(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // 重新压缩文件到MASTERSZIPS目录
    private static void createMasterZipFiles(String masterUnzipBaseDir, String masterZipBaseDir) throws IOException {
        Path unzipPath = Paths.get(masterUnzipBaseDir);
        Path zipPath = Paths.get(masterZipBaseDir);
        if (!Files.exists(zipPath)) {
            Files.createDirectories(zipPath);
        }

        // 遍历解压目录下的每个一级目录，分别压缩
        Files.list(unzipPath).filter(Files::isDirectory).forEach(dir -> {
            String dirName = dir.getFileName().toString();
            String zipFileName = dirName + "_modified.zip";
            Path zipFilePath = zipPath.resolve(zipFileName);
            try {
                zipDirectory(dir, zipFilePath);
            } catch (IOException e) {
                System.err.println("压缩目录失败: " + dir + ", 错误: " + e.getMessage());
            }
        });
        System.out.println("文件压缩操作完成。");
    }

    // 压缩目录为ZIP文件
    private static void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 计算ZIP条目名称，保持目录结构
                    String entryName = sourceDir.relativize(file).toString().replace(File.separator, "/");
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // 为目录创建ZIP条目
                    String entryName = sourceDir.relativize(dir).toString().replace(File.separator, "/") + "/";
                    if (!entryName.equals("./")) {
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}


package org.example;

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
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class XmlToExcelConverter {

    // 定义协议数据的Excel表头
    private static final String[] PROTOCOL_HEADERS = {
            "protocolName", "协议id", "接入接出类型", "接口地址", "请求配置",
            "高级_readTimeout", "高级_connectionTimeout", "新增_threshold",
            "校验数值_VerificationValue", "校验URI列规范性", "根文件名", "节点属性值校验"
    };

    // 定义规则数据的Excel表头
    private static final String[] RULE_HEADERS = {
            "接口id", "中文描述", "A适配器"
    };

    // 存储解压目录与ZIP文件名的映射关系
    private static Map<String, String> unzipDirToZipName = new HashMap<>();

    public static void main(String[] args) {
        // 指定目录路径（请根据实际情况修改）
        String baseDirPath = "src/main/resources/DSPZIPS/DEVZIPS";
        String outputExcelPath = "src/main/resources/DSPZIPS/OUT/output.xlsx";
        String unzipBaseDir = "src/main/resources/DSPZIPS/DEVUNZIPS"; // 解压文件的根目录
        String masterUnzipBaseDir = "src/main/resources/DSPZIPS/MASTERUNZIPS"; // 修改后文件解压根目录
        String masterZipBaseDir = "src/main/resources/DSPZIPS/MASTERSZIPS"; // 修改后压缩文件目录
        String allLibsDir = "src/main/resources/DSPZIPS/ALLLIBS"; // ALLLIBS目录，包含lib、cssystem和system文件夹
        String zipEncoding = "GBK"; // 指定ZIP文件编码，解决中文乱码问题

        try {
            // 解压指定目录下的所有ZIP文件，并记录映射关系
            unzipAllFiles(baseDirPath, unzipBaseDir, zipEncoding);

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

        } catch (Exception e) {
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
                    System.out.println("记录映射: " + normalizedUnzipDir + " -> " + zipFileName);
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

    // 根据文件路径获取根文件名（最外层ZIP文件名）
    private static String getRootZipName(String filePath, String unzipBaseDir) {
        try {
            // 获取规范化的文件路径和解压根目录路径
            String normalizedFilePath = new File(filePath).getCanonicalPath();
            String normalizedUnzipBaseDir = new File(unzipBaseDir).getCanonicalPath();

            System.out.println("尝试获取ZIP文件名，文件路径: " + normalizedFilePath);
            System.out.println("解压根目录: " + normalizedUnzipBaseDir);

            // 检查文件路径是否以解压根目录开头
            if (normalizedFilePath.startsWith(normalizedUnzipBaseDir)) {
                // 提取解压根目录后的相对路径
                String relativePath = normalizedFilePath.substring(normalizedUnzipBaseDir.length());
                // 去除可能的前缀斜杠或反斜杠
                relativePath = relativePath.replaceFirst("^[/\\\\]+", "");

                // 找到第一级目录名（对应解压后的ZIP目录名）
                int separatorIndex = relativePath.indexOf(File.separator);
                if (separatorIndex > 0) {
                    String rootDirName = relativePath.substring(0, separatorIndex);
                    // 构建第一级目录的完整路径
                    String rootDirPath = new File(normalizedUnzipBaseDir, rootDirName).getCanonicalPath();
                    // 从映射中获取ZIP文件名
                    String zipName = unzipDirToZipName.get(rootDirPath);
                    System.out.println("第一级目录路径: " + rootDirPath + ", 对应ZIP文件名: " + (zipName != null ? zipName : "未找到"));
                    return zipName != null ? zipName : "未知ZIP文件（映射未找到）";
                } else {
                    System.out.println("无法找到第一级目录，相对路径: " + relativePath);
                    return "未知ZIP文件（无第一级目录）";
                }
            } else {
                System.out.println("文件路径不在解压根目录下");
                return "未知ZIP文件（路径不匹配）";
            }
        } catch (IOException e) {
            System.err.println("获取ZIP文件名时发生错误: " + e.getMessage());
            return "未知ZIP文件（路径解析错误）";
        }
    }

    // 解析协议XML文件并提取数据
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
                rowData[0] = element.getAttribute("protocolName");
                rowData[1] = element.getAttribute("id");
                rowData[2] = element.getAttribute("ioDirection");

                // 提取common节点的uri属性
                NodeList commonList = element.getElementsByTagName("common");
                if (commonList.getLength() > 0) {
                    rowData[3] = ((Element) commonList.item(0)).getAttribute("uri");
                }

                // 提取request节点的method属性
                NodeList requestList = element.getElementsByTagName("request");
                if (requestList.getLength() > 0) {
                    rowData[4] = ((Element) requestList.item(0)).getAttribute("method");
                }

                // 提取response节点的readTimeout属性
                NodeList responseList = element.getElementsByTagName("response");
                if (responseList.getLength() > 0) {
                    rowData[5] = ((Element) responseList.item(0)).getAttribute("readTimeout");
                }

                // 提取advanced节点的connectionTimeout和threshold属性
                NodeList advancedList = element.getElementsByTagName("advanced");
                if (advancedList.getLength() > 0) {
                    Element advancedElement = (Element) advancedList.item(0);
                    rowData[6] = advancedElement.getAttribute("connectionTimeout");
                    rowData[7] = advancedElement.getAttribute("threshold");
                }

                // 校验数值_VerificationValue
                try {
                    long readTimeout = rowData[5].isEmpty() ? 0 : Long.parseLong(rowData[5].trim());
                    long connectionTimeout = rowData[6].isEmpty() ? 0 : Long.parseLong(rowData[6].trim());
                    long threshold = rowData[7].isEmpty() ? 0 : Long.parseLong(rowData[7].trim());
                    if (readTimeout == 10000 && connectionTimeout == 10000 && threshold == 524288) {
                        rowData[8] = "符合";
                    } else {
                        rowData[8] = "不符合";
                    }
                } catch (NumberFormatException e) {
                    rowData[8] = "不符合";
                }

                // 校验URI列规范性
                String uri = rowData[3];
                if (uri != null && !uri.isEmpty() && isValidUri(uri)) {
                    rowData[9] = "规范";
                } else {
                    rowData[9] = "不规范";
                }

                // 获取根文件名（最外层ZIP文件名）
                rowData[10] = getRootZipName(file.getAbsolutePath(), unzipBaseDir);

                // 校验advanced节点及其子节点的属性值
                rowData[11] = validateAdvancedAndChildAttributes(doc);

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
            }
        } else {
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
                relativePathStr = relativePathStr.replaceFirst("^[/\\\\]+", "");
                Path relativePath = Paths.get(relativePathStr);
                Path targetFilePath = Paths.get(targetBaseDir).resolve(relativePath);

                System.out.println("处理XML文件: " + originalFilePath + " -> " + targetFilePath);

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
                    advancedElement.setAttribute("readTimeout", "58000 ");
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
                    System.out.println("已复制lib文件夹到: " + libConsoleTargetPath);

                    // 复制到根目录/lib
                    Path libRootTargetPath = rootDir.resolve("lib");
                    copyFolder(libSourcePath, libRootTargetPath);
                    System.out.println("已复制lib文件夹到: " + libRootTargetPath);
                } else {
                    System.out.println("lib文件夹不存在: " + libSourcePath);
                }

                // 复制cssystem文件夹到dev目录下
                Path cssystemSourcePath = allLibsPath.resolve("cssystem");
                if (Files.exists(cssystemSourcePath)) {
                    Path cssystemTargetPath = rootDir.resolve("dev").resolve("cssystem");
                    if (!Files.exists(cssystemTargetPath.getParent())) {
                        Files.createDirectories(cssystemTargetPath.getParent());
                    }
                    copyFolder(cssystemSourcePath, cssystemTargetPath);
                    System.out.println("已复制cssystem文件夹到: " + cssystemTargetPath);
                } else {
                    System.out.println("cssystem文件夹不存在: " + cssystemSourcePath);
                }

                // 复制system文件夹到console目录下
                Path systemSourcePath = allLibsPath.resolve("system");
                if (Files.exists(systemSourcePath)) {
                    Path systemTargetPath = rootDir.resolve("console").resolve("system");
                    if (!Files.exists(systemTargetPath.getParent())) {
                        Files.createDirectories(systemTargetPath.getParent());
                    }
                    copyFolder(systemSourcePath, systemTargetPath);
                    System.out.println("已复制system文件夹到: " + systemTargetPath);
                } else {
                    System.out.println("system文件夹不存在: " + systemSourcePath);
                }
            } catch (IOException e) {
                System.err.println("复制文件夹到根目录失败: " + rootDir + ", 错误: " + e.getMessage());
            }
        });
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
                System.out.println("已创建压缩文件: " + zipFilePath);
            } catch (IOException e) {
                System.err.println("压缩目录失败: " + dir + ", 错误: " + e.getMessage());
            }
        });
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
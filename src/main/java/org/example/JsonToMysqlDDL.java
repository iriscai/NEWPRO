package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonToMysqlDDL {

    private static final Logger logger = LoggerFactory.getLogger(JsonToMysqlDDL.class);

    public static void main(String[] args) {

        String directoryPath = "src/main/resources/DHR"; // 指定你的JSON文件目录路径
        String outPath = "src/main/resources/DDLS/"; // 指定你的JSON文件目录路径
        File directory = new File(directoryPath);
        System.out.println(directory);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".docx"));
            System.out.println(files.length);
            if (files != null) {
                for (File file : files) {
                    try {
                        String ddl = generateDDL(file);
                        logger.info(ddl);
                        write(ddl,outPath,file.getName().toLowerCase());
                    } catch (IOException e) {
                        logger.error("Error processing file: {}", file.getName(), e);
                    }
                }
            }
        } else {
            logger.error("Directory does not exist: {}", directoryPath);
        }
    }

    public static void write(String sql,String filePath,String file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath+file+"_ddl.sql"))) {
            writer.write(sql);
            writer.newLine(); // 添加一个新行，以便分隔DDL语句
            System.out.println("DDL statement written to file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error writing DDL statement to file.");
        }
    }

    private static String generateDDL(File file) throws IOException {
        String tableName = file.getName().replace(".docx", "");
        List<Field> fields = readJsonFile(file);
        StringBuilder ddlBuilder = new StringBuilder();
        ddlBuilder.append("CREATE TABLE `").append(tableName.toLowerCase()).append("` (\n");

        List<String> fieldDefinitions = fields.stream()
                .map(field -> "`" + field.getName() + "` " + getMysqlType(field.getType()) + " COMMENT '" + field.getLabel() + "'")
                .collect(Collectors.toList());

        ddlBuilder.append(String.join(",\n", fieldDefinitions));
        ddlBuilder.append("\n);");

        return ddlBuilder.toString();
    }

//    private static List<Field> readJsonFile(File file) throws IOException {
//        String content = new String(Files.readAllBytes(Paths.get(file.getPath())), "UTF-8");
//        ObjectMapper mapper = new ObjectMapper();
//        return mapper.readValue(content, new TypeReference<List<Field>>() {});
//    }
//  private static List<Field> readJsonFile(File file) throws IOException {
//        String content = new String(Files.readAllBytes(Paths.get(file.getPath())), "UTF-8");
//        ObjectMapper mapper = new ObjectMapper();
//        ArrayNode rootNode = (ArrayNode) mapper.readTree(content);
//
//        List<Field> fields = rootNode.stream()
//                .map(jsonNode -> mapper.convertValue(jsonNode, Field.class))
//                .collect(Collectors.toList());
//
//        return fields;
//    }
private static List<Field> readJsonFile(File file) throws IOException {
    String content1 = new String(Files.readAllBytes(Paths.get(file.getPath())), "UTF-8");

    String content = '['+content1+ ']';
    System.out.println(content);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 忽略未知属性
    ArrayNode rootNode = (ArrayNode) mapper.readTree(content);

    List<Field> fields = new ArrayList<>();
    for (JsonNode jsonNode : rootNode) {
        Field field = mapper.convertValue(jsonNode, Field.class);
        fields.add(field);
    }

    return fields;
}
    private static String getMysqlType(String type) {
        switch (type.toLowerCase()) {
            case "string":
                return "VARCHAR(255)";
            case "date":
                return "DATE";
            case "datetime":
                return "DATETIME";
            default:
                return "VARCHAR(255)"; // 默认类型
        }
    }

    static class Field {
        private String name;
        private String label;
        private String type;

        // getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}

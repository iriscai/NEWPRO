package com;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.json.JSONObject;
import org.json.JSONException;

public class JsonFileProcessor  {

    public static void main(String[] args) {
        // 指定文件夹路径（替换为实际路径）
        String folderPath = "src/main/resources/0625/IN"; // 例如: "C:/Users/YourName/Documents/input_files"
        // 输出文件路径（替换为实际路径）
        String outputFilePath = "src/main/resources/0625/OUT/output_content.txt"; // 例如: "C:/Users/YourName/Documents/output_content.txt"

        try {
            // 读取文件夹中的所有文件并处理
            processFilesInFolder(folderPath, outputFilePath);
            System.out.println("文件处理完成，内容已写入到: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("处理文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void processFilesInFolder(String folderPath, String outputFilePath) throws IOException {
        // 创建输出文件写入流
        File outputFile = new File(outputFilePath);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs(); // 创建输出文件目录（如果不存在）
        }

        // 使用 UTF-8 编码写入文件
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            // 遍历文件夹中的所有文件
            Files.walkFileTree(Paths.get(folderPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println("处理文件: " + file.toString());
                    // 使用 UTF-8 编码读取文件内容
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    // 写入文件名作为分隔
                    writer.write("--文件: " + file.getFileName().toString());
                    writer.newLine();
                    writer.write("--内容: ");
                    writer.newLine();

                    // 检查文件类型并处理
                    if (file.toString().toLowerCase().endsWith(".json") || file.toString().toLowerCase().endsWith(".script")) {
                        try {
                            // 尝试解析为 JSON
                            JSONObject jsonObject = new JSONObject(content);
                            // 如果是 JSON 格式，格式化输出
                            if (jsonObject.has("jobs")) {
                                writer.write(jsonObject.getJSONArray("jobs").toString(2)); // 提取 jobs 字段并格式化
                            } else if (jsonObject.has("content")) {
                                writer.write(jsonObject.getString("content")); // 提取 content 字段
                            } else {
                                writer.write(jsonObject.toString(2)); // 否则输出整个 JSON
                            }
                        } catch (JSONException e) {
                            // 如果不是有效的 JSON，直接写入原始内容
                            System.err.println("解析文件内容出错（非 JSON 格式）: " + file.toString());
                            writer.write(content); // 写入原始内容
                        }
                    } else {
                        // 其他文件类型直接写入内容
                        writer.write(content);
                    }

                    writer.newLine();
//                    writer.write("------------------------");
                    writer.newLine();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println("无法访问文件: " + file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
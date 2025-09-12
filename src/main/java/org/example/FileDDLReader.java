package org.example;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class FileDDLReader {
    public static void main(String[] args) {
        String targetPath = "src/main/resources/oldddls";
        String outputFile = "output1.txt";
        File directory = new File(targetPath);

        if (!directory.exists()) {
            System.out.println("目录不存在: " + targetPath);
            return;
        }

        try {
            List<File> files = listFiles(directory);

            if (files.isEmpty()) {
                System.out.println("目录为空: " + targetPath);
                return;
            }

            writeContentToFile(files, outputFile);

            System.out.println("文件写入完成: " + outputFile);
            System.out.println("共处理文件数量: " + files.size());

        } catch (IOException e) {
            System.err.println("发生错误:");
            e.printStackTrace();
        }
    }

    private static List<File> listFiles(File directory) {
        List<File> fileList = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".script")) {
                    fileList.add(file);
                } else if (file.isDirectory()) {
                    fileList.addAll(listFiles(file));
                }
            }
        }
        return fileList;
    }


    private static void writeContentToFile(List<File> files, String outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            JsonParser parser = new JsonParser();

            for (File file : files) {
                try {
                    // 读取整个文件内容
                    String jsonContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

                    // 解析JSON
                    JsonObject jsonObject = parser.parse(jsonContent).getAsJsonObject();

                    // 获取content字段内容
                    String content = jsonObject.get("content").getAsString();

                    // 写入文件名和内容
                    writer.write("-- File: " + file.getName());
                    writer.newLine();
                    writer.write(content);
                    writer.newLine();
                    writer.write("-- ------------------------");
                    writer.newLine();
                    writer.newLine();

                } catch (Exception e) {
                    writer.write("读取文件失败: " + file.getPath());
                    writer.newLine();
                    writer.write("错误信息: " + e.getMessage());
                    writer.newLine();
                    writer.newLine();
                }
            }
        }
    }

}
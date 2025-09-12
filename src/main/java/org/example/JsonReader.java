package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonReader {
    public static void main(String[] args) {
        String filePath = "src/main/resources/DHR/BusinessTripData.docx"; // 替换为你的文件实际路径
        List<String> keyValues = readJsonFile(filePath);

        // 打印所有key值
        for (String key : keyValues) {
            System.out.println(key);
        }
    }

    public static List<String> readJsonFile(String filePath) {
        List<String> keyValues = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            // 处理文件内容，确保是JSON数组格式
            String jsonContent = content.toString().trim();
            if (!jsonContent.startsWith("[")) {
                jsonContent = "[" + jsonContent + "]";
            }

            // 解析JSON数组
            JSONArray jsonArray = JSON.parseArray(jsonContent);

            // 遍历数组中的每个对象
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.containsKey("key")) {
                    String keyValue = jsonObject.getString("key");
                    keyValues.add(keyValue);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("读取文件失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("解析JSON失败: " + e.getMessage());
        }

        return keyValues;
    }
}
package com.icsc.mr.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TableInfo {
    private String tableName;       // 表名
    private String tableNameCn;     // 表中文名
    private String projectName;     // 项目名
    private String className;       // 类名
    private String packageName;     // 包名
    private String entityName;      // 实体名
    private String author;          // 作者
    private String description;     // 描述
    private List<FieldInfo> fields = new ArrayList<>(); // 字段列表
}

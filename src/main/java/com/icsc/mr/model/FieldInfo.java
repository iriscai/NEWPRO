package com.icsc.mr.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FieldInfo {
    private String name;            // 字段名
    private String dataType;        // 数据类型
    private boolean isPrimaryKey;   // 是否主键
    private String description;     // 描述
    private String width;           // 字段宽度
    private String format;          // 格式
    private String defaultValue;    // 默认值
    private boolean isNullable;     // 是否可为空（根据主键推断）
}

package com.sixlens.project.webank.config;

/**
 * @ClassName: Table
 * @Description: //TODO 
 * @Author: cwy
 * @Date: 2023/6/9 0009 
 * @Version: 1.0
 */
public enum Table {

    TABLE_A("table_a", "SELECT * FROM table_a"),
    TABLE_B("table_b", "SELECT * FROM table_b"),
    TABLE_C("table_c", "SELECT * FROM table_c");

    private final String tableName;
    private final String query;

    Table(String tableName, String query) {
        this.tableName = tableName;
        this.query = query;
    }

    public String getTableName() {
        return tableName;
    }

    public String getQuery() {
        return query;
    }
}

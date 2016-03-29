package org.fdroid.fdroid.data;

import java.util.ArrayList;
import java.util.List;

abstract class QueryBuilder {

    private final List<String> fields = new ArrayList<>();
    private final StringBuilder tables = new StringBuilder(getRequiredTables());
    private String selection;
    private String orderBy;

    protected abstract String getRequiredTables();

    public abstract void addField(String field);

    protected int fieldCount() {
        return fields.size();
    }

    public void addFields(String[] fields) {
        for (final String field : fields) {
            addField(field);
        }
    }

    protected boolean isDistinct() {
        return false;
    }

    protected String groupBy() {
        return null;
    }

    protected void appendField(String field) {
        appendField(field, null, null);
    }

    protected void appendField(String field, String tableAlias) {
        appendField(field, tableAlias, null);
    }

    protected final void appendField(String field, String tableAlias,
                             String fieldAlias) {

        StringBuilder fieldBuilder = new StringBuilder();

        if (tableAlias != null) {
            fieldBuilder.append(tableAlias).append('.');
        }

        fieldBuilder.append(field);

        if (fieldAlias != null) {
            fieldBuilder.append(" AS ").append(fieldAlias);
        }

        fields.add(fieldBuilder.toString());
    }

    public void addSelection(String selection) {
        this.selection = selection;
    }

    public void addOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    protected final void leftJoin(String table, String alias, String condition) {
        joinWithType("LEFT", table, alias, condition);
    }

    protected final void join(String table, String alias, String condition) {
        joinWithType("", table, alias, condition);
    }

    private void joinWithType(String type, String table, String alias, String condition) {
        tables.append(' ')
            .append(type)
            .append(" JOIN ")
            .append(table);

        if (alias != null) {
            tables.append(" AS ").append(alias);
        }

        tables.append(" ON (")
            .append(condition)
            .append(')');
    }

    private String distinctSql() {
        return isDistinct() ? " DISTINCT " : "";
    }

    private String fieldsSql() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(fields.get(i));
        }
        return sb.toString();
    }

    private String whereSql() {
        return selection != null ? " WHERE " + selection : "";
    }

    private String orderBySql() {
        return orderBy != null ? " ORDER BY " + orderBy : "";
    }

    private String groupBySql() {
        return groupBy() != null ? " GROUP BY " + groupBy() : "";
    }

    private String tablesSql() {
        return tables.toString();
    }

    public String toString() {
        return "SELECT " + distinctSql() + fieldsSql() + " FROM " + tablesSql() + whereSql() + groupBySql() + orderBySql();
    }
}

package org.fdroid.fdroid.data;

import java.util.ArrayList;
import java.util.List;

abstract class QueryBuilder {

    private List<String> fields = new ArrayList<String>();
    private StringBuilder tables = new StringBuilder(getRequiredTables());
    private String selection = null;
    private String orderBy = null;

    protected abstract String getRequiredTables();

    public abstract void addField(String field);

    protected int fieldCount() {
        return fields.size();
    }

    public void addFields(String[] fields) {
        for (String field : fields) {
            addField(field);
        }
    }

    protected boolean isDistinct() {
        return false;
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

    protected final void leftJoin(String table, String alias,
                                  String condition) {
        tables.append(" LEFT JOIN ");
        tables.append(table);
        if (alias != null) {
            tables.append(" AS ");
            tables.append(alias);
        }
        tables.append(" ON (");
        tables.append(condition);
        tables.append(")");
    }

    private String fieldsSql() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i ++) {
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

    private String tablesSql() {
        return tables.toString();
    }

    public String toString() {
        String distinct = isDistinct() ? " DISTINCT " : "";
        return "SELECT " + distinct + fieldsSql() + " FROM " + tablesSql() + whereSql() + orderBySql();
    }
}

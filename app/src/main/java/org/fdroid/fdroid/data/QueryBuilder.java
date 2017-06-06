package org.fdroid.fdroid.data;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class QueryBuilder {

    private final List<String> fields = new ArrayList<>();
    private final StringBuilder tables = new StringBuilder(getRequiredTables());
    private String selection;
    private String[] selectionArgs;
    private final List<OrderClause> orderBys = new ArrayList<>();
    private int limit = 0;

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

    public void addSelection(@Nullable QuerySelection selection) {
        if (selection == null) {
            this.selection = null;
            this.selectionArgs = null;
        } else {
            this.selection = selection.getSelection();
            this.selectionArgs = selection.getArgs();
        }
    }

    /**
     * Add an order by, which includes an expression and optionally ASC or DESC afterward.
     */
    public void addOrderBy(String orderBy) {
        if (orderBy != null) {
            orderBys.add(new OrderClause(orderBy));
        }
    }

    public void addOrderBy(@Nullable OrderClause orderClause) {
        if (orderClause != null) {
            orderBys.add(orderClause);
        }
    }

    public void addLimit(int limit) {
        this.limit = limit;
    }

    public String[] getArgs() {
        List<String> args = new ArrayList<>();

        if (selectionArgs != null) {
            Collections.addAll(args, selectionArgs);
        }

        for (OrderClause orderBy : orderBys) {
            if (orderBy.getArgs() != null) {
                Collections.addAll(args, orderBy.getArgs());
            }
        }

        String[] strings = new String[args.size()];
        args.toArray(strings);
        return strings;
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
        return TextUtils.join(", ", fields);
    }

    private String whereSql() {
        return selection != null ? " WHERE " + selection : "";
    }

    private String orderBySql() {
        if (orderBys.size() == 0) {
            return "";
        }
        return " ORDER BY " + TextUtils.join(", ", orderBys);
    }

    private String groupBySql() {
        return groupBy() != null ? " GROUP BY " + groupBy() : "";
    }

    private String tablesSql() {
        return tables.toString();
    }

    private String limitSql() {
        return limit > 0 ? " LIMIT " + limit : "";
    }

    public String toString() {
        return "SELECT " + distinctSql() + fieldsSql() + " FROM " + tablesSql()
                + whereSql() + groupBySql() + orderBySql() + limitSql();
    }
}

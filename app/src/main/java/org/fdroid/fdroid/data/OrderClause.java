package org.fdroid.fdroid.data;

public class OrderClause {

    public final String expression;
    private String[] args;

    public OrderClause(String expression) {
        this.expression = expression;
    }

    public OrderClause(String field, String[] args, boolean isAscending) {
        this.expression = field + " " + (isAscending ? "ASC" : "DESC");
        this.args = args;
    }

    @Override
    public String toString() {
        return expression;
    }

    public String[] getArgs() {
        return args;
    }
}
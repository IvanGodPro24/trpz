package chtml;

import java.util.Map;

public class ForNode extends CompositeNode {
    private final String loopVar;
    private final String listExpr;

    public ForNode(String loopVar, String listExpr) {
        this.loopVar = loopVar;
        this.listExpr = listExpr;
    }

    @Override
    public String render(Map<String, Object> context) {
        Object listObj = ChtmlEngine.resolveExpression(listExpr, context);
        StringBuilder out = new StringBuilder();
        if (listObj == null) return "";

        Iterable<?> iterable = null;

        if (listObj instanceof Iterable) {
            iterable = (Iterable<?>) listObj;
        } else if (listObj.getClass().isArray()) {
            iterable = java.util.Arrays.asList((Object[]) listObj);
        } else if (listObj instanceof CharSequence) {
            String s = listObj.toString();
            String[] parts = s.split(",");
            java.util.List<String> list = new java.util.ArrayList<>(parts.length);
            for (String p : parts) list.add(p == null ? "" : p.trim());
            iterable = list;
        }

        if (iterable == null) return "";

        for (Object item : iterable) {
            java.util.HashMap<String,Object> inner = new java.util.HashMap<>(context);
            inner.put(loopVar, item);
            out.append(super.render(inner));
        }
        return out.toString();
    }
}


package chtml;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextNode extends TemplateNode {
    private static final Pattern EXPR = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");
    private final String text;

    public TextNode(String text) {
        this.text = text;
    }

    @Override
    public String render(Map<String, Object> context) {
        Matcher m = EXPR.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String expr = m.group(1).trim();
            Object val = ChtmlEngine.resolveExpression(expr, context);
            String rep = val == null ? "" : val.toString();
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}

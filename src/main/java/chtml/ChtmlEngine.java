package chtml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class ChtmlEngine {
    private static final Pattern TAG_PATTERN = Pattern.compile("\\{%\\s*(.*?)\\s*%}|\\{\\{\\s*(.*?)\\s*}}", Pattern.DOTALL);

    private ChtmlEngine(){}

    public static String render(String templatePath, Map<String, Object> context) throws IOException {
        String text = readTemplate(templatePath);
        TemplateNode root = parse(text);
        Map<String,Object> ctx = context == null ? new HashMap<>() : new HashMap<>(context);
        return root.render(ctx);
    }

    private static String readTemplate(String templatePath) throws IOException {
        String normalized = templatePath.startsWith("/") ? templatePath.substring(1) : templatePath;

        Path p = Path.of(normalized);
        if (Files.exists(p) && Files.isRegularFile(p)) {
            return Files.readString(p);
        }

        Path guess = Path.of("templates").resolve(normalized);
        if (Files.exists(guess) && Files.isRegularFile(guess)) {
            return Files.readString(guess);
        }

        String resPath = "templates/" + normalized;
        var is = ChtmlEngine.class.getClassLoader().getResourceAsStream(resPath);
        if (is != null) {
            try (is) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        throw new IOException("Template not found: " + templatePath);
    }


    private static TemplateNode parse(String text) {
        CompositeNode root = new CompositeNode();
        Deque<CompositeNode> stack = new ArrayDeque<>();
        stack.push(root);

        Matcher m = TAG_PATTERN.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                String raw = text.substring(last, m.start());
                assert stack.peek() != null;
                stack.peek().add(new TextNode(raw));
            }
            String tag = m.group(1); // {% ... %}
            String expr = m.group(2); // {{ ... }}
            if (expr != null) {
                assert stack.peek() != null;
                stack.peek().add(new TextNode("{{ " + expr + " }}"));
            } else if (tag != null) {
                String t = tag.trim();
                if (t.startsWith("if ")) {
                    String cond = t.substring(3).trim();
                    IfNode ifn = new IfNode(cond);
                    assert stack.peek() != null;
                    stack.peek().add(ifn);
                    stack.push(ifn);
                } else if (t.equals("else")) {
                    if (!stack.isEmpty() && stack.peek() instanceof IfNode) {
                        ((IfNode) stack.peek()).setElseMode();
                    } else {
                        assert stack.peek() != null;
                        stack.peek().add(new TextNode("{% else %}"));
                    }
                } else if (t.equals("endif")) {
                    if (stack.size() > 1) stack.pop();
                } else if (t.startsWith("for ")) {
                    String inside = t.substring(4).trim();
                    String[] parts = inside.split("\\s+in\\s+", 2);
                    if (parts.length == 2) {
                        String var = parts[0].trim();
                        String listExpr = parts[1].trim();
                        ForNode fn = new ForNode(var, listExpr);
                        assert stack.peek() != null;
                        stack.peek().add(fn);
                        stack.push(fn);
                    } else {
                        assert stack.peek() != null;
                        stack.peek().add(new TextNode("{% " + t + " %}"));
                    }
                } else if (t.equals("endfor")) {
                    if (stack.size() > 1) stack.pop();
                } else {
                    assert stack.peek() != null;
                    stack.peek().add(new TextNode("{% " + t + " %}"));
                }
            }
            last = m.end();
        }
        if (last < text.length()) {
            assert stack.peek() != null;
            stack.peek().add(new TextNode(text.substring(last)));
        }
        return root;
    }

    public static Object resolveExpression(String expr, Map<String, Object> context) {
        if (expr == null) return null;
        expr = expr.trim();
        if (expr.matches("-?\\d+")) {
            try { return Long.parseLong(expr); } catch (Exception ignored) {}
        }
        if (expr.matches("-?\\d+\\.\\d+")) {
            try { return Double.parseDouble(expr); } catch (Exception ignored) {}
        }
        if ((expr.startsWith("'") && expr.endsWith("'")) || (expr.startsWith("\"") && expr.endsWith("\""))) {
            return expr.substring(1, expr.length()-1);
        }
        String[] parts = expr.split("\\.");
        Object cur = context.get(parts[0]);
        if (parts.length == 1) return cur;
        for (int i = 1; i < parts.length; i++) {
            if (cur == null) return null;
            String p = parts[i];
            if (cur instanceof Map) {
                cur = ((Map<?,?>)cur).get(p);
            } else {
                try {
                    var field = cur.getClass().getDeclaredField(p);
                    field.setAccessible(true);
                    cur = field.get(cur);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    try {
                        String getter = "get" + Character.toUpperCase(p.charAt(0)) + p.substring(1);
                        cur = cur.getClass().getMethod(getter).invoke(cur);
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
        }
        return cur;
    }

    public static boolean isTruthy(Object v) {
        return switch (v) {
            case null -> false;
            case Boolean b -> b;
            case Number number -> number.doubleValue() != 0.0;
            case String s -> !s.isBlank();
            case Collection collection -> !collection.isEmpty();
            default -> true;
        };
    }
}

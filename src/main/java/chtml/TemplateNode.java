package chtml;

import java.util.Map;

public abstract class TemplateNode {
    public abstract String render(Map<String, Object> context);
}

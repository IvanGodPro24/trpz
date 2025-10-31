package chtml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompositeNode extends TemplateNode {
    protected final List<TemplateNode> children = new ArrayList<>();

    public void add(TemplateNode node) {
        children.add(node);
    }

    @Override
    public String render(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        for (TemplateNode c : children) sb.append(c.render(context));
        return sb.toString();
    }
}

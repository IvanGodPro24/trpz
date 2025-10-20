package composite;

import java.util.ArrayList;
import java.util.List;

public class HtmlComposite implements HtmlComponent {
    private final String tag;
    private final List<HtmlComponent> children = new ArrayList<>();

    public HtmlComposite(String tag) {
        this.tag = tag;
    }

    public void add(HtmlComponent component) {
        children.add(component);
    }

    public void remove(HtmlComponent component) {
        children.remove(component);
    }

    @Override
    public String render() {
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(tag).append(">");
        for (HtmlComponent child : children) {
            builder.append(child.render());
        }
        builder.append("</").append(tag).append(">");
        return builder.toString();
    }
}
package composite;

public class HtmlElement implements HtmlComponent {
    private final String tag;
    private final String content;

    public HtmlElement(String tag, String content) {
        this.tag = tag;
        this.content = content;
    }

    @Override
    public String render() {
        return "<" + tag + ">" + content + "</" + tag + ">";
    }
}

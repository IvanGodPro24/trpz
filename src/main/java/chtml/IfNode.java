package chtml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class IfNode extends CompositeNode {
    private final String conditionExpr;

    private final List<TemplateNode> elseChildren = new ArrayList<>();

    private boolean inElseMode = false;

    public IfNode(String conditionExpr) {
        this.conditionExpr = conditionExpr;
    }

    @Override
    public void add(TemplateNode node) {
        if (!inElseMode) {
            super.add(node);
        } else {
            elseChildren.add(node);
        }
    }

    public void setElseMode() {
        this.inElseMode = true;
    }

    @Override
    public String render(Map<String, Object> context) {
        Object v = ChtmlEngine.resolveExpression(conditionExpr, context);
        if (ChtmlEngine.isTruthy(v)) {
            return super.render(context);
        } else {
            StringBuilder sb = new StringBuilder();
            for (TemplateNode t : elseChildren) sb.append(t.render(context));
            return sb.toString();
        }
    }
}

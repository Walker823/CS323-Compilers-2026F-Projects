import framework.project2.ConstExprResult;
import generated.SplcBaseVisitor;
import generated.SplcParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstExprVisitor extends SplcBaseVisitor<Integer> {

    private final List<ConstExprResult> results = new ArrayList<>();

    public final List<ConstExprResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    protected final void addResult(String expression, int value) {
        results.add(new ConstExprResult(expression, value));
    }

    @Override
    public Integer visitLocalVarDef(SplcParser.LocalVarDefContext ctx) {
        if (ctx.expression() != null) {
            Integer value = visit(ctx.expression());
            if (value != null) {
                addResult(ctx.expression().getText(), value);
            }
        }
        return null;
    }

    /** Returns null for every expression outside the project's constexpr subset. */
    @Override
    public Integer visitExpression(SplcParser.ExpressionContext ctx) {
        if (ctx.getChildCount() == 1 && ctx.Number() != null) {
            return Integer.parseInt(ctx.Number().getText());
        }
        if (ctx.getChildCount() == 3 && ctx.LPAREN() != null
                && ctx.getStart().getType() == SplcParser.LPAREN) {
            return visit(ctx.expression(0));
        }
        if (ctx.getChildCount() == 2 && (ctx.PLUS() != null || ctx.MINUS() != null)) {
            Integer value = visit(ctx.expression(0));
            return value == null ? null : (ctx.MINUS() != null ? -value : value);
        }
        if (ctx.expression().size() != 2 || ctx.getChildCount() != 3) {
            return null;
        }
        // Check the operator before evaluating operands of unsupported expressions.
        if (ctx.PLUS() == null && ctx.MINUS() == null && ctx.STAR() == null
                && ctx.DIV() == null && ctx.MOD() == null) {
            return null;
        }
        Integer left = visit(ctx.expression(0));
        Integer right = visit(ctx.expression(1));
        if (left == null || right == null) {
            return null;
        }
        if (ctx.PLUS() != null) return left + right;
        if (ctx.MINUS() != null) return left - right;
        if (ctx.STAR() != null) return left * right;
        if (ctx.DIV() != null) return left / right;
        return left % right;
    }
}

package impl;
import framework.project3.ErrorReporter;
import generated.Splc;
import generated.SplcBaseVisitor;
import impl.symbol.*;
import impl.type.*;
import org.antlr.v4.runtime.Token;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

// Student's main semantic-analysis implementation.
public final class NameResolver extends SplcBaseVisitor<Void> {
    private final GlobalScope globalScope = new GlobalScope();
    private Scope currentScope = globalScope;
    private final ErrorReporter reporter;
    public NameResolver(ErrorReporter reporter) { this.reporter = reporter; }
    public GlobalScope getGlobalScope() { return globalScope; }

    public void analyze(Splc.ProgramContext program) {
        for (Splc.GlobalDefContext definition : program.globalDef()) visit(definition);
    }
    private Type resolveType(Splc.TypeContext ctx) {
        if (ctx.INT() != null) return PrimitiveType.INT;
        Token name = ctx.Identifier().getSymbol();
        Type type = globalScope.resolveStruct(name.getText());
        if (type == null) reporter.undefinedStruct(name);
        return type;
    }
    private Type resolveDeclaredType(Splc.VariableDeclContext ctx) {
        Type type = resolveType(ctx.type());
        if (type == null) return null;
        List<Splc.ArrayDimensionContext> dimensions = ctx.arrayDimension();
        for (int i = dimensions.size() - 1; i >= 0; i--) {
            int length = Integer.parseInt(dimensions.get(i).Number().getText());
            type = new ArrayType(type, length);
        }
        return type;
    }
    private void enterScope() { currentScope = new Scope(currentScope); }
    private void exitScope() { currentScope = currentScope.getParent(); }

    @Override public Void visitStructDef(Splc.StructDefContext ctx) {
        Token name = ctx.Identifier().getSymbol();
        StructType struct = new StructType(name.getText());
        boolean valid = globalScope.resolveStruct(name.getText()) == null;
        if (!valid) reporter.redefinedStruct(name);
        // Track names even for fields whose types cannot be resolved.
        Set<String> memberNames = new HashSet<>();
        for (Splc.FieldDefContext field : ctx.fieldDef()) {
            Splc.VariableDeclContext declaration = field.variableDecl();
            Type type = resolveDeclaredType(declaration);
            Token member = declaration.Identifier().getSymbol();
            boolean unique = memberNames.add(member.getText());
            if (!unique) reporter.duplicateMember(member);
            if (type == null || !unique) {
                valid = false;
            } else {
                struct.defineMember(new VariableSymbol(member.getText(), type));
            }
        }
        if (valid) globalScope.defineStruct(struct);
        return null;
    }
    @Override public Void visitGlobalVarDef(Splc.GlobalVarDefContext ctx) {
        defineVariable(ctx.variableDecl(), globalScope);
        return null;
    }
    @Override public Void visitFunctionDef(Splc.FunctionDefContext ctx) {
        Type returnType = resolveType(ctx.type());
        Token name = ctx.Identifier().getSymbol();
        FunctionSymbol function = new FunctionSymbol(name.getText(), returnType, globalScope);
        if (returnType != null && !globalScope.define(function)) {
            reporter.redefinedSymbol(name);
        }
        Scope previous = currentScope;
        currentScope = function;
        try {
            if (ctx.parameterList() != null) {
                for (Splc.ParameterContext parameter : ctx.parameterList().parameter()) {
                    Splc.VariableDeclContext declaration = parameter.variableDecl();
                    Type type = resolveDeclaredType(declaration);
                    if (type == null) continue;
                    Token parameterName = declaration.Identifier().getSymbol();
                    if (!function.addParameter(new VariableSymbol(parameterName.getText(), type))) {
                        reporter.redefinedSymbol(parameterName);
                    }
                }
            }
            // The outermost body shares its scope with the parameters.
            for (Splc.StatementContext statement : ctx.block().statement()) visit(statement);
        } finally {
            currentScope = previous;
        }
        return null;
    }
    @Override public Void visitBlock(Splc.BlockContext ctx) {
        enterScope();
        try {
            for (Splc.StatementContext statement : ctx.statement()) visit(statement);
        } finally {
            exitScope();
        }
        return null;
    }
    @Override public Void visitLocalVarDef(Splc.LocalVarDefContext ctx) {
        defineVariable(ctx.variableDecl(), currentScope);
        if (ctx.expression() != null) visit(ctx.expression());
        return null;
    }
    @Override public Void visitExpression(Splc.ExpressionContext ctx) {
        // A direct Identifier is a reference/call name unless it follows DOT.
        if (ctx.Identifier() != null && ctx.DOT() == null) {
            Token name = ctx.Identifier().getSymbol();
            if (currentScope.resolve(name.getText()) == null) {
                reporter.undeclaredIdentifier(name);
            }
        }
        return visitChildren(ctx);
    }

    private void defineVariable(Splc.VariableDeclContext declaration, Scope scope) {
        Type type = resolveDeclaredType(declaration);
        if (type == null) return;
        Token name = declaration.Identifier().getSymbol();
        if (!scope.define(new VariableSymbol(name.getText(), type))) {
            reporter.redefinedSymbol(name);
        }
    }
}

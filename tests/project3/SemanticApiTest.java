import framework.project3.Diagnostic;
import framework.project3.ErrorReporter;
import generated.SplLexer;
import generated.Splc;
import impl.NameResolver;
import impl.symbol.*;
import impl.type.*;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/** Assertions for contracts that are not observable through Grader output. */
public final class SemanticApiTest {
    private static int checks;

    public static void main(String[] args) {
        tables();
        resolvedTypes();
        recovery();
        System.out.println("API checks passed: " + checks);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static VariableSymbol variable(String name) {
        return new VariableSymbol(name, PrimitiveType.INT);
    }

    private static void tables() {
        GlobalScope global = new GlobalScope();
        VariableSymbol x = variable("x");
        VariableSymbol y = variable("y");
        check(global.getParent() == null, "global has no parent");
        check(global.define(x) && global.define(y), "define global variables");
        check(!global.define(variable("x")), "reject duplicate variable");
        check(List.copyOf(global.getLocalSymbols()).equals(List.of(x, y)),
                "duplicate preserves objects and order");
        Scope child = new Scope(global);
        check(child.resolveLocal("x") == null, "local lookup does not consult parent");
        check(child.resolve("x") == x, "lookup returns original outer object");
        VariableSymbol inner = variable("x");
        check(child.define(inner), "allow shadowing");
        check(new Scope(child).resolve("x") == inner, "nearest declaration wins");
        check(child.resolve("y") == y && child.resolve("missing") == null,
                "parent lookup and missing name");
        check(global.resolve("x") == x, "shadowing leaves parent unchanged");

        StructType first = new StructType("x");
        StructType second = new StructType("B");
        check(global.defineStruct(first) && global.defineStruct(second),
                "type names are independent of ordinary names");
        check(!global.defineStruct(new StructType("x")), "reject duplicate type");
        check(global.resolveStruct("x") == first && global.resolveStruct("y") == null,
                "struct lookup uses only the type table");
        check(List.copyOf(global.getStructTypes()).equals(List.of(first, second)),
                "struct order and identity survive duplicate");
        VariableSymbol member = variable("m");
        VariableSymbol other = variable("n");
        check(first.defineMember(member) && first.defineMember(other), "define fields");
        check(!first.defineMember(variable("m")), "reject duplicate member");
        check(first.resolveMember("m") == member && first.resolveMember("y") == null,
                "member lookup returns original object and ignores global symbols");
        check(List.copyOf(first.getMembers()).equals(List.of(member, other)),
                "field order survives duplicate");
        check(second.defineMember(variable("m")), "different structs may share field names");

        FunctionSymbol function = new FunctionSymbol("f", first, global);
        VariableSymbol parameter = variable("x");
        VariableSymbol parameter2 = variable("p");
        check(function.addParameter(parameter) && function.addParameter(parameter2),
                "parameters may shadow globals");
        check(function.resolveLocal("x") == parameter
                && function.getParameters().get(0) == parameter,
                "parameter list and scope share one object");
        check(!function.addParameter(variable("x")), "duplicate parameter rejected");
        check(function.define(variable("local"))
                && !function.addParameter(variable("local")), "parameter conflicts with any local symbol");
        check(!function.define(variable("p")), "top-level local conflicts with parameter");
        check(function.getParameters().equals(List.of(parameter, parameter2)),
                "failed registration does not append parameters");
        check(function.getType() == first && function.getParent() == global,
                "function retains return type and lexical parent");
    }

    private static NameResolver analyze(String source, ErrorReporter reporter) {
        Splc parser = new Splc(new CommonTokenStream(new SplLexer(CharStreams.fromString(source))));
        Splc.ProgramContext program = parser.program();
        check(parser.getNumberOfSyntaxErrors() == 0, "test input is syntactically valid");
        NameResolver resolver = new NameResolver(reporter);
        resolver.analyze(program);
        return resolver;
    }

    private static void resolvedTypes() {
        ErrorReporter reporter = new ErrorReporter();
        NameResolver resolver = analyze("""
                struct A { int grid[2][3]; };
                struct B { struct A a; };
                struct A global[0][2];
                struct A f(struct A p[2][3], int n) {
                    struct A local[3][0];
                    { int local; }
                    return p;
                }
                """, reporter);
        check(!reporter.hasErrors(), "valid types produce no diagnostics");
        GlobalScope global = resolver.getGlobalScope();
        StructType a = global.resolveStruct("A");
        check(global.resolveStruct("B").resolveMember("a").getType() == a,
                "field reuses registered struct object");
        ArrayType grid = (ArrayType) a.resolveMember("grid").getType();
        check(grid.getLength() == 2 && ((ArrayType) grid.getElementType()).getLength() == 3,
                "array nesting follows declaration order");
        check(((ArrayType) grid.getElementType()).getElementType() == PrimitiveType.INT,
                "array primitive base is canonical INT");
        ArrayType array = (ArrayType) global.resolve("global").getType();
        check(array.getLength() == 0 && ((ArrayType) array.getElementType()).getElementType() == a,
                "zero dimension and struct identity preserved");
        FunctionSymbol function = (FunctionSymbol) global.resolve("f");
        check(function.getType() == a, "return type reuses registered struct");
        ArrayType parameter = (ArrayType) function.getParameters().get(0).getType();
        check(((ArrayType) parameter.getElementType()).getElementType() == a,
                "array parameter reuses registered struct");
        check(function.resolveLocal("p") == function.getParameters().get(0),
                "visitor registers same parameter object in both collections");
        check(function.resolveLocal("local").getType().prettyPrint().equals("struct A[3][0]"),
                "top-level local belongs to function, nested shadow does not replace it");
        check(global.resolveLocal("local") == null, "function locals do not leak globally");
    }

    private static void recovery() {
        ErrorReporter reporter = new ErrorReporter();
        NameResolver resolver = analyze("""
                struct A { int x; };
                struct A { int y; };
                struct Bad { int x; int x; };
                struct Forward { struct Later x; };
                struct Later { int y; };
                struct Missing omitted;
                int f(struct Missing bad, int good) {
                    struct Missing local = missing;
                    int good;
                    return good + body;
                }
                struct Missing broken(int p) { return p; }
                int f(int replacement) { return replacement; }
                int after;
                """, reporter);
        GlobalScope global = resolver.getGlobalScope();
        check(global.resolveStruct("A").resolveMember("x") != null
                && global.resolveStruct("A").resolveMember("y") == null,
                "duplicate struct preserves original fields");
        check(global.resolveStruct("Bad") == null && global.resolveStruct("Forward") == null,
                "invalid structs never become visible");
        check(global.resolveStruct("Later") != null && global.resolve("omitted") == null,
                "analysis resumes after invalid declarations");
        FunctionSymbol function = (FunctionSymbol) global.resolve("f");
        check(function.getParameters().size() == 1
                && function.getParameters().get(0).getName().equals("good"),
                "invalid parameter skipped, valid parameter retained");
        check(function.resolveLocal("bad") == null && function.resolveLocal("local") == null,
                "invalid declarations do not introduce symbols");
        check(function.resolveLocal("good") == function.getParameters().get(0),
                "duplicate local leaves parameter unchanged");
        check(function.resolveLocal("replacement") == null && global.resolve("broken") == null,
                "duplicate function and invalid return type do not alter registered functions");
        check(global.resolve("after") != null && global.resolve("p") == null,
                "global scope restored after invalid function");
        List<Diagnostic> diagnostics = reporter.getDiagnostics();
        check(diagnostics.size() == 11, "all expected recovery diagnostics collected once");
        check(diagnostics.get(6).getKind() == Diagnostic.Kind.UNDECLARED_IDENTIFIER
                && diagnostics.get(6).getName().equals("missing")
                && diagnostics.get(6).getLine() == 7,
                "invalid local initializer is checked with correct token location");
    }
}

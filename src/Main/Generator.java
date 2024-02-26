package Main;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // create "class Main {"
        print("public class Main {");
        newline(0);
        newline(++indent);
        // declare fields
        for (int i = 0; i < ast.getFields().size(); i++)
        {
            if (i != 0)
                newline(indent);
            print(ast.getFields().get(i));
        }
        // declare "public static void main (String[] args) {
        print("public static void main(String[] args) {");
        newline(++indent);
        //              System.exit(main());
        print("System.exit(new Main().main());");
        newline(--indent);
        //          }"
        print("}");
        // declare methods (one of which will be main)
        for (int i = 0; i < ast.getMethods().size(); i++)
        {
            newline(0);
            newline(indent);
            print(ast.getMethods().get(i));
        }

        // }"
        newline(0);
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(ast.getTypeName(), " ", ast.getName());
        if (ast.getValue().isPresent())
            print(" = ", ast.getValue().get());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        if (ast.getReturnTypeName().isPresent())
            print(Environment.getType(ast.getReturnTypeName().get()).getJvmName(), " ");
        else
            print("void ");

        print(ast.getName(), "(");
        for (int i = 0; i < ast.getParameters().size(); i++)
        {
            if (i != 0)
                print(", ");
            print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName(),
                    " ", ast.getParameters().get(i));
        }
        print(")");

        print(" {");
        if (!ast.getStatements().isEmpty())
        {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++)
            {
                if (i != 0)
                    newline(indent);
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if (ast.getValue().isPresent())
            print(" = ", ast.getValue().get());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver());
        print(" = ");
        print(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (");
        print(ast.getCondition());
        print(") {");

        if (!ast.getThenStatements().isEmpty())
        {
            newline(++indent);
            for (int i = 0; i < ast.getThenStatements().size(); i++)
            {
                if (i != 0)
                    newline(indent);
                print(ast.getThenStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        if (!ast.getElseStatements().isEmpty())
        {
            print(" else {");
            newline(++indent);
            for (int i = 0; i < ast.getElseStatements().size(); i++)
            {
                if (i != 0)
                    newline(indent);
                print(ast.getElseStatements().get(i));
            }
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (int ", ast.getName(), " : ", ast.getValue(), ") {");

        for (int i = 0; i < ast.getStatements().size(); i++)
        {
            newline(++indent);
            if (i != 0)
                newline(indent);
            print(ast.getStatements().get(i));
            newline(--indent);
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");
        if (!ast.getStatements().isEmpty())
        {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++)
            {
                if (i != 0)
                    newline(indent);
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getType() == Environment.Type.STRING)
        {
            print("\"", ast.getLiteral(), "\"");
        }
        else if (ast.getType() == Environment.Type.CHARACTER)
        {
            print("\'", ast.getLiteral(), "\'");
        }
        else
            print(ast.getLiteral());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        if (ast.getOperator().equals("AND"))
            print(" && ");
        else if (ast.getOperator().equals("OR"))
            print(" || ");
        else
            print(" ", ast.getOperator(), " ");
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent())
        {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getVariable().getJvmName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent())
        {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getFunction().getJvmName(), "(");
        for (int i = 0; i < ast.getArguments().size(); i++)
        {
            if (i != 0)
                print(", ");
            visit(ast.getArguments().get(i));
        }
        print(")");
        return null;
    }

}


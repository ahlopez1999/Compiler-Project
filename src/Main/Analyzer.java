package Main;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (int i = 0; i < ast.getFields().size(); i++)
        {
            visit(ast.getFields().get(i));
        }
        for (int i = 0; i < ast.getMethods().size(); i++)
        {
            visit(ast.getMethods().get(i));
        }
        scope.lookupFunction("main", 0);
        if (scope.lookupFunction("main", 0).getReturnType() != Environment.Type.INTEGER)
        {
            throw new RuntimeException("main/0 function must have an integer return type");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getValue().isPresent())
        {
            visit(ast.getValue().get());
            requireAssignable(ast.getValue().get().getType(), Environment.getType(ast.getTypeName()));
        }
        scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        // getting parameter types
        List<Environment.Type> parameterTypes = new ArrayList<>();
        for (int i = 0; i < ast.getParameterTypeNames().size(); i++)
        {
            parameterTypes.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }
        Environment.Type returnType;
        if (ast.getReturnTypeName().isPresent())
        {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }
        else
            returnType = Environment.Type.NIL;

        scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, returnType, args->Environment.NIL);
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        for (int i = 0; i < ast.getStatements().size(); i++)
        {
            scope = new Scope(scope);
            //define parameters in scope
            for (int j = 0; j < ast.getParameters().size(); j++)
            {
                scope.defineVariable(ast.getParameters().get(i), Environment.NIL);
            }
            visit(ast.getStatements().get(i));
        }
        method = ast;
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expr.Function))
            throw new RuntimeException("Expression must be a function");
        else
            visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        Environment.Type type;
        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent())
        {
            throw new RuntimeException("Both type and value do not exist");
        }
        else if (ast.getTypeName().isPresent())
        {
            type = Environment.getType(ast.getTypeName().get());
        }
        else
        {
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        }
        scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        if (!(ast.getReceiver() instanceof  Ast.Expr.Access))
        {
            throw new RuntimeException("Receiver must be an access expression");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN)
            throw new RuntimeException("Condition must be of type BOOLEAN");
        if (ast.getThenStatements().isEmpty())
            throw new RuntimeException("Then statements cannot be empty");
        // visit then statements
        for (int i = 0; i < ast.getThenStatements().size(); i++)
        {
            scope = new Scope(scope);
            visit(ast.getThenStatements().get(i));
        }
        for (int i = 0; i < ast.getElseStatements().size(); i++)
        {

            scope = new Scope(scope);
            visit(ast.getElseStatements().get(i));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        if (ast.getValue().getType() != Environment.Type.INTEGER_ITERABLE)
        {
            throw new RuntimeException("Value must be of type INTEGER_ITERABLE");
        }
        else if (ast.getStatements().isEmpty())
        {
            throw new RuntimeException("Statements cannot be zero");
        }
        for (int i = 0; i < ast.getStatements().size(); i++)
        {
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            visit(ast.getStatements().get(i));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN)
            throw new RuntimeException("Condition must be of type BOOLEAN");
        for (int i = 0; i < ast.getStatements().size(); i++)
        {
            scope = new Scope(scope);
            visit(ast.getStatements().get(i));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        requireAssignable(ast.getValue().getType(), method.getFunction().getReturnType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null)
        {
            ast.setType(Environment.Type.NIL);
        }
        else if (ast.getLiteral() instanceof Boolean)
        {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (ast.getLiteral() instanceof Character)
        {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (ast.getLiteral() instanceof String)
        {
            ast.setType(Environment.Type.STRING);
        }
        else if (ast.getLiteral() instanceof BigInteger)
        {
            if (((BigInteger) ast.getLiteral()).bitCount() > 32)
            {
                throw new RuntimeException("Integer exceeds 32 bits.");
            }
            else
                ast.setType(Environment.Type.INTEGER);
        }
        else if (ast.getLiteral() instanceof BigDecimal)
        {
            Double literal = ((BigDecimal) ast.getLiteral()).doubleValue();
            if (literal == Double.POSITIVE_INFINITY || literal == Double.NEGATIVE_INFINITY)
            {
                throw new RuntimeException("Decimal Exceeds double limit.");
            }
            else
                ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        //is not an instance of a binary expression
        if (!(ast.getExpression() instanceof Ast.Expr.Binary))
        {
            throw new RuntimeException("Grouped Expression is not binary");
        }
        else
            visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        switch (ast.getOperator())
        {
            case "AND":
            case "OR":
                if (ast.getLeft().getType() == Environment.Type.BOOLEAN && ast.getRight().getType() == Environment.Type.BOOLEAN)
                    ast.setType(Environment.Type.BOOLEAN);
                else
                    throw new RuntimeException("Both expressions must be Booleans.");
                break;
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
                if (ast.getLeft().getType() == ast.getRight().getType() && ast.getLeft().getType() == Environment.Type.COMPARABLE )
                    ast.setType(Environment.Type.BOOLEAN);
                else
                    throw new RuntimeException("Expressions are not comparable");
                break;
            case "+":
                if (ast.getLeft().getType() == Environment.Type.STRING || ast.getRight().getType() == Environment.Type.STRING)
                    ast.setType(Environment.Type.STRING);
                else if (ast.getLeft().getType() == Environment.Type.INTEGER && ast.getRight().getType() == Environment.Type.INTEGER)
                    ast.setType(Environment.Type.INTEGER);
                else if (ast.getLeft().getType() == Environment.Type.DECIMAL && ast.getRight().getType() == Environment.Type.DECIMAL)
                    ast.setType(Environment.Type.DECIMAL);
                else
                    throw new RuntimeException("Expressions cannot be added");
                break;
            case "-":
            case "*":
            case "/":
                if (ast.getLeft().getType() == Environment.Type.INTEGER && ast.getRight().getType() == Environment.Type.INTEGER)
                    ast.setType(Environment.Type.INTEGER);
                else if (ast.getLeft().getType() == Environment.Type.DECIMAL && ast.getRight().getType() == Environment.Type.DECIMAL)
                    ast.setType(Environment.Type.DECIMAL);
                break;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent())
        {
            visit(ast.getReceiver().get());
            ast.setVariable(ast.getReceiver().get().getType().getField(ast.getName()));
        }
        else
            ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent())
        {
            visit(ast.getReceiver().get());
            ast.setFunction(ast.getReceiver().get().getType().getMethod(ast.getName(),ast.getArguments().size()));
        }
        else
            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        //visit arguments
        if (ast.getArguments().size() > 0)
        {
            for (int i = 0; i < ast.getArguments().size(); i++)
            {
                visit(ast.getArguments().get(i));
                // Possibly switched up type and target check on this
                Environment.Type type = ast.getArguments().get(i).getType();
                Environment.Type target = scope.lookupFunction(ast.getName(), ast.getArguments().size()).getParameterTypes().get(i);
                requireAssignable(target, type);
            }
        }
        return  null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target == type)
            return;
        else if (target == Environment.Type.ANY)
            return;
        else if (target == Environment.Type.COMPARABLE)
        {
            if (type == Environment.Type.INTEGER || type == Environment.Type.DECIMAL || type == Environment.Type.CHARACTER || type == Environment.Type.STRING)
                return;
            else
                throw new RuntimeException("Not assignable");
        }
        else
            throw new RuntimeException("Not assignable");

    }

}

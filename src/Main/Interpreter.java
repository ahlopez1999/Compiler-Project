package Main;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent())
        {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else
        {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition())))
        {
            try
            {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null)
        {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());

        switch (ast.getOperator()) {
            case "AND":
                if (!requireType(Boolean.class, left))
                    return Environment.create(left.getValue());
                else
                {
                    Environment.PlcObject right = visit(ast.getRight());
                    requireType(Boolean.class, right);
                    return Environment.create(right.getValue());
                }
            case "OR":
                if (requireType(Boolean.class, left))
                    return Environment.create(left.getValue());
                else
                {
                    Environment.PlcObject right = visit(ast.getRight());
                    requireType(Boolean.class, right);
                    return Environment.create(right.getValue());
                }
            case "<":
                Environment.PlcObject right = visit(ast.getRight());
                int i = requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), right));
                if (i < 0)
                {
                    return Environment.create(Boolean.TRUE);
                }
                else
                    return Environment.create(Boolean.FALSE);
            case "<=":
                right = visit(ast.getRight());
                i = requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), right));
                requireType(left.getClass(), right);
                if (i < 0 || i == 0)
                {
                    return Environment.create(Boolean.TRUE);
                }
                else
                    return Environment.create(Boolean.FALSE);
            case ">":
                right = visit(ast.getRight());
                i = requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), right));
                if (i > 0)
                {
                    return Environment.create(Boolean.TRUE);
                }
                else
                    return Environment.create(Boolean.FALSE);
            case ">=":
                right = visit(ast.getRight());
                i = requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), right));
                if (i > 0 || i == 0)
                {
                    return Environment.create(Boolean.TRUE);
                }
                else
                    return Environment.create(Boolean.FALSE);
            case "==":
                right = visit(ast.getRight());
                if (left.getValue().equals(right.getValue()))
                    return Environment.create(Boolean.TRUE);
                else
                    return Environment.create(Boolean.FALSE);
            case "+":
                right = visit(ast.getRight());
                if (left.getValue().getClass() == String.class || right.getValue().getClass() == String.class)
                {
                    String buffer = left.getValue().toString() + right.getValue().toString();
                    return Environment.create(buffer);
                }
                else if (left.getValue().getClass() == BigInteger.class)
                {
                    BigInteger buffer = requireType(BigInteger.class, left).add(requireType(BigInteger.class, right));
                    return Environment.create(buffer);
                }
                else
                {
                    BigDecimal buffer = requireType(BigDecimal.class, left).add(requireType(BigDecimal.class, right));
                    return Environment.create(buffer);
                }
            case "-":
                right = visit(ast.getRight());
                if (left.getValue().getClass() == BigInteger.class)
                {
                    BigInteger buffer = requireType(BigInteger.class, left).subtract(requireType(BigInteger.class, right));
                    return Environment.create(buffer);
                }
                else
                {
                    BigDecimal buffer = requireType(BigDecimal.class, left).subtract(requireType(BigDecimal.class, right));
                    return Environment.create(buffer);
                }
            case "*":
                right = visit(ast.getRight());
                if (left.getValue().getClass() == BigInteger.class)
                {
                    BigInteger buffer = requireType(BigInteger.class, left).multiply(requireType(BigInteger.class, right));
                    return Environment.create(buffer);
                }
                else
                {
                    BigDecimal buffer = requireType(BigDecimal.class, left).multiply(requireType(BigDecimal.class, right));
                    return Environment.create(buffer);
                }
            case "/":
                right = visit(ast.getRight());
                if (left.getValue().getClass() == BigInteger.class)
                {
                    BigInteger buffer = requireType(BigInteger.class, left).divide(requireType(BigInteger.class, right));
                    return Environment.create(buffer);
                }
                else
                {
                    BigDecimal buffer = requireType(BigDecimal.class, left).divide(requireType(BigDecimal.class, right), RoundingMode.HALF_EVEN);
                    return Environment.create(buffer);
                }
        }
        return null;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent())
        {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            //Object value = scope.lookupVariable(receiver.getValue().toString()).getValue().getField(ast.getName()).getValue().getValue();
            Object value = receiver.getField(ast.getName()).getValue().getValue();
            return Environment.create(value);
        }
        else
        {
            //return new Environment.PlcObject(getScope(), getScope().lookupVariable(ast.getName()).getValue().getValue());
            return new Environment.PlcObject(getScope().lookupVariable(ast.getName()).getValue().getType().getScope(), getScope().lookupVariable(ast.getName()).getValue().getValue());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent())
        {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            List<Environment.PlcObject> args = new ArrayList<>();
            for (int i = 0; i < ast.getArguments().size(); i++)
            {
                args.add(visit(ast.getArguments().get(i)));
            }
            Object value = receiver.callMethod(ast.getName(), args).getValue();
            return Environment.create(value);
        }
        else
        {
            List<Environment.PlcObject> args = new ArrayList<>();
            Environment.Function uninvoked = getScope().lookupFunction(ast.getName(), ast.getArguments().size());
            for (int i = 0; i < ast.getArguments().size(); i++)
            {
                args.add(visit(ast.getArguments().get(i)));
            }
            Environment.PlcObject invoked = uninvoked.invoke(args);
            return new Environment.PlcObject(getScope(), invoked.getValue());
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}

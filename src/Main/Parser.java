package Main;
/** 
import com.sun.org.apache.xpath.internal.operations.Bool;
import jdk.nashorn.internal.runtime.options.Option;
import org.omg.CORBA.portable.IDLEntity;
*/
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();
        while (match("LET"))
        {
            fields.add(parseField());
        }
        while (match("DEF"))
        {
            methods.add(parseMethod());
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        if (match(Token.Type.IDENTIFIER, ":", Token.Type.IDENTIFIER))
        {
            String name = tokens.get(-3).getLiteral();
            String typename = tokens.get(-1).getLiteral();
            Ast.Expr value = null;
            if (match("="))
            {
                value = parseExpression();
            }
            if (match(";"))
            {
                return new Ast.Field(name, typename, Optional.ofNullable(value));
            }
            else
                throw new ParseException("Expected ';'", tokens.index);
        }
        else
            throw new ParseException("Expected IDENTIFIER", tokens.index);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (match(Token.Type.IDENTIFIER))
        {
            String name = tokens.get(-1).getLiteral();
            if (match("("))
            {
                List<String> parameters = new ArrayList<>();
                List<String> parametertypes = new ArrayList<>();
                while (!match(")"))
                {
                    if (match(Token.Type.IDENTIFIER, ":", Token.Type.IDENTIFIER))
                    {
                        parameters.add(tokens.get(-3).getLiteral());
                        parametertypes.add(tokens.get(-1).getLiteral());
                    }
                    else if (match(","))
                    {
                        if (match(Token.Type.IDENTIFIER, ":", Token.Type.IDENTIFIER))
                        {
                            parameters.add(tokens.get(-3).getLiteral());
                            parametertypes.add(tokens.get(-1).getLiteral());
                        }
                        else
                            throw new ParseException("Expected IDENTIFIER with type", tokens.index);
                    }
                    else
                        throw new ParseException("Expected IDENTIFIER or ','", tokens.index);
                }
                String returntype = null;
                if (match(":", Token.Type.IDENTIFIER))
                {
                    returntype = tokens.get(-1).getLiteral();
                }
                if (match("DO"))
                {
                    List<Ast.Stmt> statements = new ArrayList<>();
                    while (!match("END"))
                    {
                        statements.add(parseStatement());
                    }
                    return  new Ast.Method(name, parameters, parametertypes, Optional.ofNullable(returntype), statements);
                }
                else
                    throw new ParseException("Expected DO", tokens.index);
            }
            else
                throw new ParseException("Expected '('", tokens.index);
        }
        else
            throw new ParseException("Expected IDENTIFIER", tokens.index);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if (match("LET"))
            return parseDeclarationStatement();
        else if (match("IF"))
            return parseIfStatement();
        else if (match("FOR"))
            return parseForStatement();
        else if (match("WHILE"))
            return parseWhileStatement();
        else if (match("RETURN"))
            return parseReturnStatement();
        else
        {
            // Assignment/Expression statement
            Ast.Expr receiver = parseExpression();
            if (match("="))
            {
                Ast.Expr value = parseExpression();
                if (match(";"))
                    return new Ast.Stmt.Assignment(receiver, value);
                else
                    throw new ParseException("Expected semicolon", tokens.index);
            }
            else if (match(";"))
                return new Ast.Stmt.Expression(receiver);
            else
                throw new ParseException("Expected Semicolon", tokens.index);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        if (match(Token.Type.IDENTIFIER))
        {
            String name = tokens.get(-1).getLiteral();
            if (match(":"))
            {
                if (match(Token.Type.IDENTIFIER))
                {
                    String typename = tokens.get(-1).getLiteral();
                    if (match("="))
                    {
                        Ast.Expr value = parseExpression();
                        //Declaration w/name, type, and value
                        if (match(";"))
                        {
                            return new Ast.Stmt.Declaration(name, Optional.of(typename), Optional.of(value));
                        }
                        else
                            throw new ParseException("Expected semicolon", tokens.index);
                    }
                    //Declaration w/name and type
                    else if (match(";"))
                    {
                        return new Ast.Stmt.Declaration(name, Optional.of(typename), Optional.empty());
                    }
                    else
                        throw new ParseException("Expected semicolon", tokens.index);
                }
                else
                    throw new ParseException("Expected IDENTIFIER", tokens.index);
            }
            //Declaration w/name and value
            else if (match("="))
            {
                Ast.Expr value = parseExpression();
                if (match(";"))
                {
                    return new Ast.Stmt.Declaration(name, Optional.empty(), Optional.of(value));
                }
                else
                    throw new ParseException("Expected semicolon", tokens.index);
            }
            //Declaration w/name and nothing else
            else if (match(";"))
            {
                return new Ast.Stmt.Declaration(name, Optional.empty(), Optional.empty());
            }
            else
                throw new ParseException("Expect semicolon", tokens.index);
        }
        else
            throw new ParseException("Expected IDENTIFIER", tokens.index);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        Ast.Expr condition = parseExpression();
        if (match("DO"))
        {
            //get DO statements
            List<Ast.Stmt> dostatements = new ArrayList<>();
            List<Ast.Stmt> elsestatements = new ArrayList<>();
            while (!(peek("ELSE") || peek("END")))
            {
                dostatements.add(parseStatement());
            }
            if (match("ELSE"))
            {
                while (!peek("END"))
                {
                    elsestatements.add(parseStatement());
                }
            }
            match("END");
            return new Ast.Stmt.If(condition, dostatements, elsestatements);
        }
        else
            throw new ParseException("Expected DO", tokens.index);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        if (match(Token.Type.IDENTIFIER))
        {
            String name = tokens.get(-1).getLiteral();
            if (match("IN"))
            {
                Ast.Expr value = parseExpression();
                if (match("DO"))
                {
                    List<Ast.Stmt> statements = new ArrayList<>();
                    while (!match("END"))
                    {
                        statements.add(parseStatement());
                    }
                    return new Ast.Stmt.For(name, value, statements);
                }
                else
                    throw new ParseException("Expected DO", tokens.index);
            }
            else
                throw new ParseException("Expected IN", tokens.index);
        }
        else
            throw new ParseException("Expected IDENTIFIER", tokens.index);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        Ast.Expr condition = parseExpression();
        if (match("DO"))
        {
            List<Ast.Stmt> statements = new ArrayList<>();
            while (!match("END"))
            {
                statements.add(parseStatement());
            }
            return new Ast.Stmt.While(condition, statements);
        }
        else
            throw new ParseException("Expected DO", tokens.index);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        Ast.Expr value = parseExpression();
        if (match(";"))
            return new Ast.Stmt.Return(value);
        else
            throw new ParseException("Expected semicolon", tokens.index);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr left = parseEqualityExpression();
        String operator = null;
        Ast.Expr right = null;
        if (match("AND") | match("OR"))
        {
            operator = tokens.get(-1).getLiteral();
            right = parseEqualityExpression();
            return new Ast.Expr.Binary(operator, left, right);
        }
        else
            return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr left = parseAdditiveExpression();
        String operator = null;
        Ast.Expr right = null;
        if (match("<") | match("<=") | match(">") | match(">=") | match("==")| match("!="))
        {
            operator = tokens.get(-1).getLiteral();
            right = parseAdditiveExpression();
            return new Ast.Expr.Binary(operator, left, right);
        }
        else
            return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr left = parseMultiplicativeExpression();
        String operator = null;
        Ast.Expr right = null;
        if (match("+") | match("-"))
        {
            operator = tokens.get(-1).getLiteral();
            right = parseMultiplicativeExpression();
            return new Ast.Expr.Binary(operator, left, right);
        }
        else
            return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr left = parseSecondaryExpression();
        String operator = null;
        Ast.Expr right = null;
        if (match("*") | match("/"))
        {
            operator = tokens.get(-1).getLiteral();
            right = parseSecondaryExpression();
            return new Ast.Expr.Binary(operator, left, right);
        }
        else
            return left;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr primary = parsePrimaryExpression();
        //Functions w/receiver
        if (match(".", Token.Type.IDENTIFIER, "("))
        {
            String name = tokens.get(-2).getLiteral();
            ArrayList<Ast.Expr> parameters = new ArrayList<>();

            //Add parameters if any
            while (!match(")")) {
                parameters.add(parseExpression());
                match(",");
            }
            return new Ast.Expr.Function(Optional.of(primary), name, parameters);
        }
        //Access w/receiver
        else if (match(".", Token.Type.IDENTIFIER))
        {
            return new Ast.Expr.Access(Optional.of(primary), tokens.get(-1).getLiteral());
        }
        else
            return primary;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        //literals
        if (peek("NIL"))
        {
            match("NIL");
            return new Ast.Expr.Literal(null);
        }
        else if (peek("TRUE"))
        {
            match("TRUE");
            return new Ast.Expr.Literal(Boolean.TRUE);
        }
        else if (peek("FALSE"))
        {
            match("FALSE");
            return new Ast.Expr.Literal(Boolean.FALSE);
        }
        else if (peek(Token.Type.INTEGER))
        {
            match(Token.Type.INTEGER);
            return new Ast.Expr.Literal(BigInteger.valueOf(Integer.parseInt(tokens.get(-1).getLiteral())));
        }
        else if (peek(Token.Type.DECIMAL))
        {
            match(Token.Type.DECIMAL);
            return new Ast.Expr.Literal(BigDecimal.valueOf(Double.parseDouble(tokens.get(-1).getLiteral())));
        }
        else if (peek(Token.Type.CHARACTER))
        {
            match(Token.Type.CHARACTER);
            // replacing single quotations
            String buffer = tokens.get(-1).getLiteral().replace("'","");
            // replacing escape characters
            buffer = buffer.replace("\\b","\b");
            buffer = buffer.replace("\\n","\n");
            buffer = buffer.replace("\\r","\r");
            buffer = buffer.replace("\\'","'");
            buffer = buffer.replace("\\\"","\"");
            buffer = buffer.replace("\\\\","\\");
            char lit = buffer.charAt(0);
            return new Ast.Expr.Literal(lit);
        }
        else if (peek(Token.Type.STRING))
        {
            match(Token.Type.STRING);
            // replacing quotations
            String buffer = tokens.get(-1).getLiteral().replace("\"","");
            // replacing escape characters
            buffer = buffer.replace("\\b","\b");
            buffer = buffer.replace("\\n","\n");
            buffer = buffer.replace("\\r","\r");
            buffer = buffer.replace("\\'","'");
            buffer = buffer.replace("\\\"","\"");
            buffer = buffer.replace("\\\\","\\");
            return new Ast.Expr.Literal(buffer);
        }
        //Group
        else if (peek("("))
        {
            match("(");
            Ast.Expr buffer = parseExpression();
            match(")");
            return new Ast.Expr.Group(buffer);
        }
        //Function
        else if (peek(Token.Type.IDENTIFIER, "("))
        {
            match(Token.Type.IDENTIFIER, "(");
            String name = tokens.get(-2).getLiteral();
            ArrayList<Ast.Expr> parameters = new ArrayList<>();

            //Add parameters if any
            while (!match(")")) {
                parameters.add(parseExpression());
                match(",");
            }
            return new Ast.Expr.Function(Optional.empty(), name, parameters);
        }
        //Access (variables)
        else if (peek(Token.Type.IDENTIFIER))
        {
            match(Token.Type.IDENTIFIER);
            return new Ast.Expr.Access(Optional.empty(), tokens.get(-1).getLiteral());
        }
        else
            throw new ParseException("Could not Parse PrimaryExpr",tokens.index);
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError(("Invalid pattern object: " + patterns[i].getClass()));
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}

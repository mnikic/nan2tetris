import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;

public class CompilationEngine implements AutoCloseable {
    private static final Set<KeyWordType> KEY_WORD_CONSTANT = new HashSet<>(asList(KeyWordType.TRUE, KeyWordType.FALSE, KeyWordType.NULL, KeyWordType.THIS));
    private static final List<Character> UNARY_OPS = asList('~', '-');
    private static final List<Character> TERM_LOOKAHEAD = asList('[', '.', '(');
    private final JackTokenizer tokenizer;
    private final SymbolTable symbolTable = new SymbolTable();
    private final VMWriter vmWriter;
    private String className;
    private int classWordSize;
    private int labelCount;

    public CompilationEngine(JackTokenizer tokenizer, Path file) {
        try {
            if (Files.exists(file)) {
                Files.delete(file);
            }
            this.vmWriter = new VMWriter(Files.createFile(file));
            this.tokenizer = tokenizer;
        } catch (IOException e) {
            throw new IllegalArgumentException("Something is up with the file!");
        }
    }

    //type, className, subroutineName, variableName, statement, subroutineCall don't have methods
    public void compileClass() throws IOException {
        eatKeyWord(KeyWordType.CLASS);
        className = eatIdentifier();
        eatSymbol("{");
        while (TokenType.KEYWORD.equals(tokenizer.getFutureToken().tokenType()) && tokenizer.getFutureToken().keyWord() == KeyWordType.STATIC || tokenizer.getFutureToken().keyWord() == KeyWordType.FIELD) {
            classWordSize += compileClassVarDec();
        }
        while (TokenType.KEYWORD.equals(tokenizer.getFutureToken().tokenType()) && (tokenizer.getFutureToken().keyWord() == KeyWordType.CONSTRUCTOR || tokenizer.getFutureToken().keyWord() == KeyWordType.FUNCTION
                || tokenizer.getFutureToken().keyWord() == KeyWordType.METHOD)) {
            compileSubroutineDec();
        }
        if (tokenizer.getFutureToken().tokenType() == TokenType.SYMBOL && tokenizer.getFutureToken().symbol() == '}') {
            eatSymbol("}");
            return;
        }
        if (tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException("Something is fucked! Tokens appear after the end of class");
        }
    }


    private void addToSymbolTable(String identifier, KeyWordType kind, String type) throws IOException {
        SymbolKind symbolKind;
        if (KeyWordType.STATIC == kind)
            symbolKind = SymbolKind.STATIC;
        else if (KeyWordType.FIELD == kind)
            symbolKind = SymbolKind.FIELD;
        else if (KeyWordType.VAR == kind)
            symbolKind = SymbolKind.VAR;
        else throw new IllegalArgumentException("Aaaaa " + kind);
        symbolTable.define(identifier, type, symbolKind);
    }

    private String eatIdentifier() {
        return extractNext(TokenType.IDENTIFIER, Token::identifier);
    }

    private <T> T extractNext(TokenType type, Function<Token, T> transformer) {
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            Token token = tokenizer.getCurrentToken();
            if (!type.equals(token.tokenType())) {
                throw new IllegalArgumentException("Didn't get what I was hoping for. Got " + token.tokenType()
                        + " expected " + type + ". Token: " + token);
            }
            return transformer.apply(token);
        }
        return null;
    }

    private KeyWordType eatKeyWord(KeyWordType expected) {
        KeyWordType gotten = extractNext(TokenType.KEYWORD, Token::keyWord);
        if (!expected.equals(gotten)) {
            throw new IllegalArgumentException("Noooo, I expect " + expected + " at the end!!!");
        }
        return gotten;
    }

    private KeyWordType eatKeyWord(Collection<KeyWordType> expected) {
        KeyWordType gotten = extractNext(TokenType.KEYWORD, Token::keyWord);
        if (!expected.contains(gotten)) {
            throw new IllegalArgumentException("Noooo, I expect " + expected + " at the end!!!");
        }
        return gotten;
    }

    private Character eatSymbol(String expected) {
        Character gotten = extractNext(TokenType.SYMBOL, Token::symbol);
        if (!gotten.equals(expected.charAt(0))) {
            throw new IllegalArgumentException("Noooo, I expect " + expected + " at the end!!!");
        }
        return gotten;
    }

    private Character eatSymbol(List<Character> expected) {
        Character gotten = extractNext(TokenType.SYMBOL, Token::symbol);
        if (!expected.contains(gotten)) {
            throw new IllegalArgumentException("Noooo, I expect " + expected + " at the end!!!");
        }
        return gotten;
    }

    private int compileClassVarDec() throws IOException {
        tokenizer.advance();
        Token token = tokenizer.getCurrentToken();
        KeyWordType keyWordType = token.keyWord();
        int i = compileTypeWithoutVar(keyWordType);
        return KeyWordType.STATIC.equals(keyWordType) ? 0 : i;
    }

    private int compileTypeWithoutVar(KeyWordType type) throws IOException {
        //expecting type
        String varType = eatType(asList(KeyWordType.BOOLEAN, KeyWordType.INT, KeyWordType.CHAR));
        String varName = eatIdentifier();
        addToSymbolTable(varName, type, varType);
        int varNumber = 1;
        while (tokenizer.hasMoreTokens() && !(tokenizer.getFutureToken().tokenType().equals(TokenType.SYMBOL) && tokenizer.getFutureToken().symbol() == ';')) {
            eatSymbol(",");
            String oneMoreVarName = eatIdentifier();
            addToSymbolTable(oneMoreVarName, type, varType);
            varNumber++;
        }
        if (!tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException("Noooo, I expect ; at the end!!!");
        }
        eatSymbol(";");
        return varNumber;
    }

    private String eatType(List<KeyWordType> types) throws IOException {
        if (TokenType.IDENTIFIER.equals(tokenizer.getFutureToken().tokenType())) {
            String type = eatIdentifier();
            return type;
        } else {
            KeyWordType keyWordType = eatKeyWord(types);
            return keyWordType.toString();
        }
    }

    private void compileSubroutineDec() throws IOException {
        symbolTable.startSubroutine();
        KeyWordType subroutine = eatKeyWord(asList(KeyWordType.CONSTRUCTOR, KeyWordType.FUNCTION, KeyWordType.METHOD));
        eatType(asList(KeyWordType.VOID, KeyWordType.BOOLEAN, KeyWordType.INT, KeyWordType.CHAR));
        String soubroutineName = eatIdentifier();
        eatSymbol("(");
        if (KeyWordType.METHOD.equals(subroutine)) {
            symbolTable.define("this", className, SymbolKind.ARG);
        }
        compileParameterList();
        eatSymbol(")");
        eatSymbol("{");
        int varNumber = 0;
        while (tokenizer.hasMoreTokens() && (tokenizer.getFutureToken().tokenType().equals(TokenType.KEYWORD) && tokenizer.getFutureToken().keyWord().equals(KeyWordType.VAR))) {
            varNumber += compileVarDec();
        }
        if (KeyWordType.CONSTRUCTOR.equals(subroutine)) {
            vmWriter.writeFunction(className + "." + soubroutineName, varNumber);
            vmWriter.writePush(VMSegment.CONST, classWordSize);
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(VMSegment.POINTER, 0);
        } else if (KeyWordType.METHOD.equals(subroutine)) {
            vmWriter.writeFunction(className + "." + soubroutineName, varNumber);
            symbolTable.define("this", className, SymbolKind.ARG);
            vmWriter.writePush(VMSegment.ARG, 0);
            vmWriter.writePop(VMSegment.POINTER, 0);
        } else {
            vmWriter.writeFunction(className + "." + soubroutineName, varNumber);
        }
        compileStatements();
        eatSymbol("}");
    }

    private void compileParameterList() throws IOException {
        boolean firstTime = true;
        while (tokenizer.hasMoreTokens() && !(tokenizer.getFutureToken().tokenType().equals(TokenType.SYMBOL) && tokenizer.getFutureToken().symbol() == ')')) {
            if (!firstTime) {
                eatSymbol(",");
            }
            String type = eatType(asList(KeyWordType.BOOLEAN, KeyWordType.INT, KeyWordType.CHAR));
            String varName = eatIdentifier();
            symbolTable.define(varName, type, SymbolKind.ARG);
            firstTime = false;
        }
    }

    private int compileVarDec() throws IOException {
        eatKeyWord(KeyWordType.VAR);
        return compileTypeWithoutVar(KeyWordType.VAR);
    }

    private void compileStatements() throws IOException {
        while (tokenizer.hasMoreTokens() && !(tokenizer.getFutureToken().tokenType().equals(TokenType.SYMBOL) && tokenizer.getFutureToken().symbol() == '}')) {
            compileStatement();
        }
    }

    private void compileStatement() throws IOException {
        if (!tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException("Was expecting a statement.");
        }
        Token futureToken = tokenizer.getFutureToken();
        if (futureToken.tokenType() != TokenType.KEYWORD) {
            throw new IllegalArgumentException("Was expecting a keyword.");
        }
        if (KeyWordType.LET == futureToken.keyWord())
            compileLet();
        else if (KeyWordType.IF == futureToken.keyWord())
            compileIf();
        else if (KeyWordType.WHILE == futureToken.keyWord())
            compileWhile();
        else if (KeyWordType.DO == futureToken.keyWord())
            compileDo();
        else if (KeyWordType.RETURN == futureToken.keyWord())
            compileReturn();
    }

    private void compileLet() throws IOException {
        eatKeyWord(KeyWordType.LET);
        String varName = eatIdentifier();
        int index = symbolTable.indexOf(varName);
        SymbolKind symbolKind = symbolTable.kindOf(varName);
        Token futureToken = tokenizer.getFutureToken();
        if (futureToken.tokenType() != TokenType.SYMBOL)
            throw new IllegalArgumentException("AAAAAAAAAAAAAAAAAAAAAA");
        if (futureToken.symbol() == '[') {
            eatSymbol("[");
            compileExpression();
            vmWriter.writePush(symbolKind.getVmSegment(), index);
            vmWriter.writeArithmetic(VMArithmetic.ADD);
            eatSymbol("]");
            eatSymbol("=");
            compileExpression();
            vmWriter.writePop(VMSegment.TEMP, 0);
            vmWriter.writePop(VMSegment.POINTER, 1);
            vmWriter.writePush(VMSegment.TEMP, 0);
            vmWriter.writePop(VMSegment.THAT, 0);
            eatSymbol(";");
        } else {
            eatSymbol("=");
            compileExpression();
            eatSymbol(";");
            vmWriter.writePop(symbolKind.getVmSegment(), index);
        }
    }

    private void compileIf() throws IOException {
        eatKeyWord(KeyWordType.IF);
        eatSymbol("(");
        String l1 = getNextLabel();
        compileExpression();
        vmWriter.writeArithmetic(VMArithmetic.NOT);
        vmWriter.writeIf(l1);
        eatSymbol(")");
        eatSymbol("{");
        compileStatements();
        eatSymbol("}");
        Token futureToken = tokenizer.getFutureToken();
        if (futureToken.tokenType() == TokenType.KEYWORD && futureToken.keyWord() == KeyWordType.ELSE) {
            String l2 = getNextLabel();
            eatKeyWord(KeyWordType.ELSE);
            eatSymbol("{");
            vmWriter.writeGoto(l2);
            vmWriter.writeLabel(l1);
            compileStatements();
            eatSymbol("}");
            vmWriter.writeLabel(l2);
        } else {
            vmWriter.writeLabel(l1);
        }
    }

    private void compileWhile() throws IOException {
        String l1 = getNextLabel();
        String l2 = getNextLabel();
        eatKeyWord(KeyWordType.WHILE);
        eatSymbol("(");
        vmWriter.writeLabel(l1);
        compileExpression();
        vmWriter.writeArithmetic(VMArithmetic.NOT);
        vmWriter.writeIf(l2);
        eatSymbol(")");
        eatSymbol("{");
        compileStatements();
        eatSymbol("}");
        vmWriter.writeGoto(l1);
        vmWriter.writeLabel(l2);
    }

    private void compileDo() throws IOException {
        eatKeyWord(KeyWordType.DO);
        compileSubroutineCall();
        eatSymbol(";");
        vmWriter.writePop(VMSegment.TEMP, 0);
    }

    private void compileSubroutineCall() throws IOException {
        String firstName = eatIdentifier();
        String lastName = null;
        Character symbol = eatSymbol(asList('.', '('));
        int numOfArgs = 0;
        if ('.' == symbol) {
            lastName = eatIdentifier();
            eatSymbol("(");
        }
        String functionName;
        if (lastName == null) {
            //method call
            vmWriter.writePush(VMSegment.POINTER, 0);
            numOfArgs++;
            functionName = className + "." + firstName;
        } else {
            String type = symbolTable.typeOf(firstName);
            if (type.isEmpty()) {
                //function call (static, no this)
                functionName = firstName + "." + lastName;
            } else {
                //method call on object (has this)
                vmWriter.writePush(symbolTable.kindOf(firstName).getVmSegment(), symbolTable.indexOf(firstName));
                numOfArgs++;
                functionName = type + "." + lastName;
            }

        }
        numOfArgs += compileExpressionList();
        eatSymbol(")");

        vmWriter.writeCall(functionName, numOfArgs);
    }

    private void compileReturn() throws IOException {
        eatKeyWord(KeyWordType.RETURN);
        if (!(tokenizer.getFutureToken().tokenType() == TokenType.SYMBOL && tokenizer.getFutureToken().symbol() == ';')) {
            compileExpression();
        } else {
            vmWriter.writePush(VMSegment.CONST, 0);
        }
        eatSymbol(";");
        vmWriter.writeReturn();
    }

    private void compileExpression() throws IOException {
        compileTerm();
        List<Character> op = Arrays.asList('a', '+', '-', '=', '/', '*', '&', '|', '<', '>');
        while (tokenizer.getFutureToken().tokenType().equals(TokenType.SYMBOL) && op.contains(tokenizer.getFutureToken().symbol())) {
            Character operation = eatSymbol(op);
            compileTerm();
            if ('*' == operation) {
                vmWriter.writeCall("Math.multiply", 2);
            } else if ('/' == operation) {
                vmWriter.writeCall("Math.divide", 2);
            } else {
                vmWriter.writeArithmetic(VMArithmetic.fromNonAmbigious(operation));
            }
        }
    }

    private void compileTerm() throws IOException {
        Token futureToken = tokenizer.getFutureToken();
        Token futureFutureToken = tokenizer.getFutureFutureToken();
        if (futureToken.tokenType().equals(TokenType.INT_CONST)) {
            tokenizer.advance();
            int intVal = tokenizer.getCurrentToken().intVal();
            vmWriter.writePush(VMSegment.CONST, intVal);
        } else if (futureToken.tokenType().equals(TokenType.STRING_CONST)) {
            tokenizer.advance();
            String strVal = tokenizer.getCurrentToken().stringVal();
            vmWriter.writePush(VMSegment.CONST, strVal.length());
            vmWriter.writeCall("String.new", 1);
            for (char c : strVal.toCharArray()) {
                vmWriter.writePush(VMSegment.CONST, (int) c);
                vmWriter.writeCall("String.appendChar", 2);
            }
        } else if (futureToken.tokenType().equals(TokenType.KEYWORD) && KEY_WORD_CONSTANT.contains(futureToken.keyWord())) {
            KeyWordType constant = eatKeyWord(KEY_WORD_CONSTANT);
            if (KeyWordType.NULL == constant || KeyWordType.FALSE == constant) {
                vmWriter.writePush(VMSegment.CONST, 0);
            } else if (KeyWordType.TRUE == constant) {
                vmWriter.writePush(VMSegment.CONST, 1);
                vmWriter.writeArithmetic(VMArithmetic.NEG);
            } else {
                //THIS
                vmWriter.writePush(VMSegment.POINTER, 0);
            }
        } else if (futureToken.tokenType().equals(TokenType.SYMBOL) && futureToken.symbol() == '(') {
            eatSymbol("(");
            compileExpression();
            eatSymbol(")");
        } else if (futureToken.tokenType().equals(TokenType.SYMBOL) && UNARY_OPS.contains(futureToken.symbol())) {
            Character unaryOp = eatSymbol(UNARY_OPS);
            compileTerm();
            if ('~' == unaryOp)
                vmWriter.writeArithmetic(VMArithmetic.NOT);
            else
                vmWriter.writeArithmetic(VMArithmetic.NEG);
        } else if (futureFutureToken != null && futureFutureToken.tokenType().equals(TokenType.SYMBOL) && TERM_LOOKAHEAD.contains(futureFutureToken.symbol())) {
            if ('[' == futureFutureToken.symbol()) {
                String varName = eatIdentifier();
                eatSymbol("[");
                compileExpression();
                vmWriter.writePush(symbolTable.kindOf(varName).getVmSegment(), symbolTable.indexOf(varName));
                eatSymbol("]");
                vmWriter.writeArithmetic(VMArithmetic.ADD);
                vmWriter.writePop(VMSegment.POINTER, 1);
                vmWriter.writePush(VMSegment.THAT, 0);
            } else {
                compileSubroutineCall();
            }
        } else {
            String varName = eatIdentifier();
            SymbolKind symbolKind = symbolTable.kindOf(varName);
            int index = symbolTable.indexOf(varName);
            vmWriter.writePush(symbolKind.getVmSegment(), index);
        }
    }


    private int compileExpressionList() throws IOException {
        Token futureToken = tokenizer.getFutureToken();
        int numberOfExpressions = 0;
        if (futureToken.tokenType().equals(TokenType.SYMBOL) && futureToken.symbol() == ')') {
            return numberOfExpressions;
        }
        compileExpression();
        numberOfExpressions++;
        while (tokenizer.getFutureToken().tokenType().equals(TokenType.SYMBOL) && tokenizer.getFutureToken().symbol() == ',') {
            eatSymbol(",");
            compileExpression();
            numberOfExpressions++;
        }
        return numberOfExpressions;
    }

    @Override
    public void close() throws IOException {
        if (vmWriter != null)
            vmWriter.close();
    }

    public String getNextLabel() {
        return className + "_" + labelCount++;
    }
}

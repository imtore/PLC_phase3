package main.visitor.typeChecker;

import com.sun.org.apache.xpath.internal.operations.Bool;
import main.ast.Type.NoType;
import main.ast.Type.UserDefinedType.UserDefinedType;
import main.ast.node.Node;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.expression.*;
import main.ast.node.expression.Value.BooleanValue;
import main.ast.node.expression.Value.IntValue;
import main.ast.node.expression.Value.StringValue;
import main.ast.node.statement.*;
import main.ast.Type.PrimitiveType.*;
import main.ast.Type.ArrayType.*;
import main.ast.Type.Type;
import main.symbolTable.ClassSymbolTableItem;
import main.symbolTable.SymbolTable;
import main.symbolTable.SymbolTableMethodItem;
import main.symbolTable.itemException.ItemNotFoundException;
import main.symbolTable.symbolTableVariable.SymbolTableVariableItemBase;
import main.visitor.VisitorImpl;
import sun.applet.Main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TypeChecker extends VisitorImpl {
    private TraverseState traverseState;
    private ArrayList<String> typeErrors;
    private String scope;
    private Declaration dec;

    public TypeChecker() {
        typeErrors = new ArrayList<>();
        setState(TraverseState.TypeAndUsageErrorCatching);
        scope = "";
    }

    public int numOfErrors() {
        return typeErrors.size();
    }

    private void switchState() {
        if (traverseState.name().equals(TraverseState.TypeAndUsageErrorCatching.toString()) && typeErrors.size() != 0)
            setState(TraverseState.PrintError);
        else
            setState(TraverseState.Exit);
    }

    private void setState(TraverseState traverseState) {
        this.traverseState = traverseState;
    }

    //TODO: some functions for error catching

    private void enterScope(ClassDeclaration classDeclaration) {
        String name = classDeclaration.getName().getName();
        scope = name;
        try {
            ClassSymbolTableItem classItem = (ClassSymbolTableItem) SymbolTable.root.getInCurrentScope(ClassSymbolTableItem.CLASS + name);
            SymbolTable next = classItem.getClassSym();
            SymbolTable.push(next);
        } catch (ItemNotFoundException itemNotFound) {
            System.out.println("there is an error in pushing class symbol table");
        }
    }

    private void checkForParentExistence(ClassDeclaration classDeclaration) {
//        System.out.println("start checking parent");
        if(classDeclaration.getParentName()!=null) {
            String parent = classDeclaration.getParentName().getName();
//            System.out.println("got parent name");
            SymbolTable classSymPre = SymbolTable.top.getPreSymbolTable();
            if (parent != null && classSymPre == null) {
                typeErrors.add("Line:" + classDeclaration.getParentName().getLineNum() +
                        ":class " + parent + " is not declared");
            }
        }
//        System.out.println("end checking parent");
    }//ok

    private void checkCircularInheritance(ClassDeclaration classDeclaration){
//        System.out.println("start checking circular inheritance");
        Set<String> visitedClasses = new HashSet<>();
//        System.out.println("hash defined");
        String name = classDeclaration.getName().getName();
        do{
            if(visitedClasses.contains(name)){
                typeErrors.add("Line:" + classDeclaration.getName().getLineNum() + ":circular inheritance detected");
                break;
            }
            visitedClasses.add(name);
            if(classDeclaration.getParentName()!=null) {
                name = classDeclaration.getParentName().getName();
            }
            else
                break;
        }while(name != null);
//        System.out.println("end of checking circular inheritance");
    }//ok

    /***********************************************************************************/

    private void enterScope(MethodDeclaration methodDeclaration) {
        String name = methodDeclaration.getName().getName();
        try {
            SymbolTableMethodItem methodItem = (SymbolTableMethodItem) SymbolTable.top.getInCurrentScope(SymbolTableMethodItem.METHOD + name);
            SymbolTable next = methodItem.getMethodSymbolTable();
            SymbolTable.push(next);
        } catch (ItemNotFoundException itemNotFound) {
            System.out.println("there is an error in pushing method symbol table");
        }
    }//ok

    private void checkReturnType(MethodDeclaration methodDeclaration) {
        if (!T2isT1Subtype(methodDeclaration.getActualReturnType(), methodDeclaration.getReturnValue().getType())) {
            typeErrors.add("Line:" + methodDeclaration.getReturnValue().getLineNum() +
                    ":" + methodDeclaration.getName().getName() + " return type must be " + methodDeclaration.getActualReturnType().toString());
        }
    }//ok

    /**********************************************************************************/

    private void checkMainMethodStat(Statement statement){
        if(statement instanceof Assign){
            if(((Assign) statement).getrValue()!= null){
                typeErrors.add("Line:" + statement.getLineNum() + ":unsupported statement in main method");
            }
        }
    }//ok

    /*********************************************************************************/

    private boolean checkArrayCallInstance(Expression instance){
        if (!T2isT1Subtype(new ArrayType(), instance.getType())) {
            typeErrors.add("Line:" + instance.getLineNum() + ":unsupported type for memory access");
            return false;
        }
        return true;
    }//cok

    /********************************************************************************/

    private void checkBinaryExpression(BinaryExpression binaryExpression) {
        if (binaryExpression.getBinaryOperator().toString().equals("=") ||
                 binaryExpression.getBinaryOperator().toString().equals("*") ||
                 binaryExpression.getBinaryOperator().toString().equals("/") ||
                 binaryExpression.getBinaryOperator().toString().equals("+") ||
                 binaryExpression.getBinaryOperator().toString().equals("-") ||
                 binaryExpression.getBinaryOperator().toString().equals(">") ||
                 binaryExpression.getBinaryOperator().toString().equals("<") ){
            if(!(binaryExpression.getRight().getType() instanceof IntType && binaryExpression.getLeft().getType() instanceof IntType)){
                typeErrors.add("Line:" + binaryExpression.getLineNum() +
                        ":unsupported operand type for " + binaryExpression.getBinaryOperator().toString());
                binaryExpression.setType(new NoType());
            }
            binaryExpression.setType(new IntType());
        }
        else if (binaryExpression.getBinaryOperator().toString().equals("&&") ||
                 binaryExpression.getBinaryOperator().toString().equals("||") ){
            if(!(binaryExpression.getRight().getType() instanceof BooleanType && binaryExpression.getLeft().getType() instanceof BooleanType)){
                typeErrors.add("Line:" + binaryExpression.getLineNum() +
                        ":unsupported operand type for " + binaryExpression.getBinaryOperator().toString());
                binaryExpression.setType(new NoType());
            }
            binaryExpression.setType(new BooleanType());
        }
        else if (binaryExpression.getBinaryOperator().toString().equals("==") ||
                 binaryExpression.getBinaryOperator().toString().equals("<>")){
            if(!(binaryExpression.getRight().getType() instanceof BooleanType &&
                    binaryExpression.getLeft().getType() instanceof BooleanType)){
                typeErrors.add("Line:" + binaryExpression.getLineNum() +
                        ":unable to compare operand of type " + binaryExpression.getRight().getType().toString() + " with "
                        + binaryExpression.getLeft().getType().toString());
            }
            binaryExpression.setType(new BooleanType());
        }
    }//ok

    /********************************************************************************/

    private void checkMethodCallInstance(Expression instance) {
        if (!T2isT1Subtype(new UserDefinedType(), instance.getType())) {
            typeErrors.add("Line:" + instance.getLineNum()
                    + ":cannot call method on expression of type " + instance.getType().toString());

        }
    }

    private ClassDeclaration checkForClassExistence(UserDefinedType type){
        String key = ClassSymbolTableItem.CLASS + type.getName().getName();
        ClassSymbolTableItem classItem;
        try {
            classItem = (ClassSymbolTableItem)SymbolTable.root.getInCurrentScope(key);

        }
        catch(ItemNotFoundException itemNotFound){
            return null;
        }
        return classItem.getClassDeclaration();
    }//ok

    private MethodDeclaration checkForMethodExistence(MethodCall methodCall){
        if(methodCall.getInstance() instanceof This){
            try{
                SymbolTableMethodItem methodSym = (SymbolTableMethodItem)SymbolTable.top.getPreSymbolTable()
                        .getInCurrentScope(SymbolTableMethodItem.METHOD + methodCall.getMethodName());
                return methodSym.getMethodDeclaration();
            }
            catch(ItemNotFoundException itemNotFound){
                typeErrors.add("Line:" + methodCall.getLineNum() +
                        ":there is no method named " + methodCall.getMethodName().getName() +" in this class");
                return null;
            }
        }
        else if(methodCall.getInstance().getType() instanceof UserDefinedType) {
            ClassDeclaration classDec = checkForClassExistence((UserDefinedType) methodCall.getInstance().getType());
            if (classDec != null) {
                for (MethodDeclaration method : classDec.getMethodDeclarations()) {
                    if (method.getName().getName().equals(methodCall.getMethodName().getName())) {
                        return method;
                    }
                }
                typeErrors.add("Line:" + methodCall.getLineNum() +
                        ":there is no method named " + methodCall.getMethodName().getName() +" in class " + classDec.getName().getName());
            }
            typeErrors.add("Line:" + methodCall.getInstance().getLineNum() + ":class "
                            + ((UserDefinedType) methodCall.getInstance().getType()).getName().getName() + " is not declared");
        }
        return null;
    }//ok

    private boolean  checkMethodArgs(MethodDeclaration methodDeclaration, MethodCall methodCall) {
        if (methodCall.getArgs().size() < methodDeclaration.getArgs().size()) {
            typeErrors.add("Line:" + methodCall.getLineNum() + ":too few arguments for " + methodCall.getMethodName()
                    + ", expected " + methodDeclaration.getArgs().size() + " but there are " + methodCall.getArgs().size());
            return false;
        } else if (methodCall.getArgs().size() > methodDeclaration.getArgs().size()) {
            typeErrors.add("Line:" + methodCall.getLineNum() + ":too many arguments for " + methodCall.getMethodName()
                    + ", expected " + methodDeclaration.getArgs().size() + " but there are " + methodCall.getArgs().size());
            return false;
        } else if (methodCall.getArgs().size() == methodDeclaration.getArgs().size()) {
            for (int i = 0; i < methodDeclaration.getArgs().size(); i++) {
                if (!(methodDeclaration.getArgs().get(i).getType().toString().equals(methodCall.getArgs().get(i).getType().toString()))){
                    typeErrors.add("Line:" + methodCall.getLineNum() + ":incompatible argument type. cannot pass "
                            + methodCall.getArgs().get(i).getType().toString() + " to " + methodDeclaration.getArgs().get(i).getType().toString());
                    return false;
                }
            }
        }
        return true;
    } //ok

    /********************************************************************************/

    private void checkConditionType(Expression condition) {
        if (!(T2isT1Subtype(new BooleanType(),condition.getType()))) {
            typeErrors.add("Line:" + condition.getLineNum() +
                    ":condition type must be boolean");
        }
    }

    private void checkWriteArgType(Expression arg) {
        if (!(arg.getType() instanceof IntType || arg.getType() instanceof StringType
                || arg.getType() instanceof ArrayType || arg.getType() instanceof NoType)) {
            typeErrors.add("Line:" + arg.getLineNum() +
                    ":unsupported type for writeln");
        }
    }

    private Boolean T2isT1Subtype(Type t1, Type t2) {
        if (t2 instanceof NoType || t1 instanceof NoType) {
            return true;
        } else if (t1 instanceof UserDefinedType) {
            if (t2 instanceof UserDefinedType) {
                Identifier currentType = ((UserDefinedType) t2).getName();
                ClassSymbolTableItem parent = null;
                ClassDeclaration current = ((UserDefinedType) t2).getClassDeclaration();
                do {
                    if (currentType == ((UserDefinedType) t1).getName()) {
                        return true;
                    }
                    if (current.getParentName() != null) {
                        try {

                            parent = (ClassSymbolTableItem) SymbolTable.root
                                    .getInCurrentScope(ClassSymbolTableItem.CLASS + current.getParentName().getName());
                            current = parent.getClassDeclaration();
                            currentType = current.getName();
                        } catch (ItemNotFoundException itemNotFound) {
                            parent = null;
                        }
                    } else
                        return false;
                } while (parent != null);
                return false;
            } else
                return false;
        } else if (t1 instanceof ArrayType) {
            return(t2 instanceof ArrayType);

        } else if (t1 instanceof BooleanType) {
            return(t2 instanceof BooleanType);

        } else if (t1 instanceof IntType) {
            return(t2 instanceof IntType);

        } else if (t1 instanceof StringType) {
            return(t2 instanceof StringType);

        } else {
            System.out.println("there is an error in checking subtyping");
            return false;
        }
    }

    private void validateLvalue(Expression lvalue) {
        if(!(lvalue instanceof ArrayCall || lvalue instanceof Identifier)){
            typeErrors.add("Line:" + lvalue.getLineNum() +
                    ":left side of assignment must be a valid lvalue");
        }
    }//cok

    private void checkUnaryExpression(UnaryExpression unaryExpression){
        if(unaryExpression.getUnaryOperator().toString().equals("!")){
            if (!T2isT1Subtype(new BooleanType(), unaryExpression.getValue().getType())) {
                typeErrors.add("Line:" + unaryExpression.getValue().getLineNum() +
                        ":unsupported operand type for " + unaryExpression.getUnaryOperator().toString());
                unaryExpression.setType(new NoType());
            }
            unaryExpression.setType(new BooleanType());
        }
        else if(unaryExpression.getUnaryOperator().toString().equals("-")){
            if (!T2isT1Subtype(new IntType(), unaryExpression.getValue().getType())) {
                typeErrors.add("Line:" + unaryExpression.getValue().getLineNum() +
                        ":unsupported operand type for " + unaryExpression.getUnaryOperator().toString());
                unaryExpression.setType(new NoType());
            }
            unaryExpression.setType(new IntType());
        }
    }//cok



    @Override
    public void visit(Node node) {

    }

    @Override
    public void visit(Program program) {
        while (!traverseState.toString().equals(main.visitor.typeChecker.TraverseState.Exit.toString())) {
            if (traverseState.name().equals(main.visitor.typeChecker.TraverseState.PrintError.toString())) {
                for (String error : typeErrors)
                    System.out.println(error);
                return;
            }
            if(!(program.getMainClass().getName().getName().equals("Main"))){
                typeErrors.add("Line:" + program.getMainClass().getName().getLineNum() + ":name of this class must be Main");
            }
            this.visit(program.getMainClass());

            for (ClassDeclaration classDeclaration : program.getClasses())
                this.visit(classDeclaration);
            switchState();
        }
    }//cok

    @Override
    public void visit(ClassDeclaration classDeclaration) {
        //System.out.println("Class Declaration");
        if (classDeclaration == null)
            return;

        checkForParentExistence(classDeclaration);
        //System.out.println("parent checked");
        checkCircularInheritance(classDeclaration);
        //System.out.println("circular inheritance checked");
        enterScope(classDeclaration);
        //System.out.println("entered class scope");


        for (VarDeclaration varDeclaration : classDeclaration.getVarDeclarations())
            this.visit(varDeclaration);
        //System.out.println("variable declarations are visited");
        for (MethodDeclaration methodDeclaration : classDeclaration.getMethodDeclarations())
            if(methodDeclaration instanceof MainMethodDeclaration){
                this.visit((MainMethodDeclaration) methodDeclaration);
            }
            else {
                this.visit(methodDeclaration);
            }
        //System.out.println("method declarations are visited");

        SymbolTable.pop();
        scope = "";
//        System.out.println("*");
    }//cok

    @Override
    public void visit(MethodDeclaration methodDeclaration) {
        //System.out.println("Method Declaration");
//        System.out.println(scope);
        if (methodDeclaration == null)
            return;
//        else if (traverseState.name().equals(TraverseState.TypeAndUsageErrorCatching.toString()))
        enterScope(methodDeclaration);
//        System.out.println("entered method scope");
        for (VarDeclaration argDeclaration : methodDeclaration.getArgs())
            visit(argDeclaration);
//        System.out.println("argument declarations are visited");
        for (VarDeclaration localVariable : methodDeclaration.getLocalVars())
            this.visit(localVariable);
//        System.out.println("local variables are visited");
//        System.out.println(methodDeclaration.getLocalVars().size());
//        System.out.println(methodDeclaration.getBody().size());
        for (Statement statement : methodDeclaration.getBody())
            visitStatement(statement);
//        System.out.println("statements are visited");

        visitExpr(methodDeclaration.getReturnValue());
        checkReturnType(methodDeclaration);
        SymbolTable.pop();
//        System.out.println("*");
    }//cok

    @Override
    public void visit(MainMethodDeclaration mainMethodDeclaration) {
//        System.out.println("Main Method Declaration");
        if (mainMethodDeclaration == null)
            return;
        enterScope(mainMethodDeclaration);
        if(!(mainMethodDeclaration.getName().getName().equals("main"))){
            typeErrors.add("Line:" + mainMethodDeclaration.getName().getLineNum() + ":name of this method must be main");
        }

        for (Statement statement : mainMethodDeclaration.getBody()) {
            visitStatement(statement);
            checkMainMethodStat(statement);
        }
        visitExpr(mainMethodDeclaration.getReturnValue());
        checkReturnType(mainMethodDeclaration);
        SymbolTable.pop();
//        System.out.println("*");
    }//cok

    @Override
    public void visit(VarDeclaration varDeclaration) {
        if (varDeclaration == null)
            return;
        //visitExpr(varDeclaration.getIdentifier());
    }//cok

    @Override
    public void visit(ArrayCall arrayCall) {
        boolean ok = true;
        if (arrayCall == null)
            return;
        try {
            visitExpr(arrayCall.getInstance());
            if(!checkArrayCallInstance(arrayCall.getInstance())){
                arrayCall.setType(new NoType());
                ok = false;
            }

            visitExpr(arrayCall.getIndex());
            if (!T2isT1Subtype(new IntType(), arrayCall.getIndex().getType())) {
                typeErrors.add("Line:" + arrayCall.getIndex().getLineNum() +
                        ":index must be of type int");
                arrayCall.setType(new NoType());
                ok = false;
            }
            if(ok){
                arrayCall.setType(new IntType());
            }
        } catch (NullPointerException npe) {
            System.out.println("instance or index is null");
        }
    }//cok

    @Override
    public void visit(BinaryExpression binaryExpression) {

        if (binaryExpression == null)
            return;
        Expression lOperand = binaryExpression.getLeft();
        Expression rOperand = binaryExpression.getRight();
        try {
            visitExpr(lOperand);
            visitExpr(rOperand);
            checkBinaryExpression(binaryExpression);
        } catch (NullPointerException npe) {
            System.out.println("one of operands is null, there is a syntax error");
        }
    }//cok

    @Override
    public void visit(Identifier identifier) {
        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
        String key = SymbolTableVariableItemBase.VARIABLE + identifier.getName();
        SymbolTable current = SymbolTable.top;
        visitedSymbolTables.add(current);
        SymbolTableVariableItemBase symItem = null;
        do {
            try {
                symItem = (SymbolTableVariableItemBase) current.getInCurrentScope(key);
                break;
            }
            catch(ItemNotFoundException itemNotFound){
                if(current.getPreSymbolTable()!=null) {
                    current = current.getPreSymbolTable();
                    if (visitedSymbolTables.contains(current))
                        break;
                    visitedSymbolTables.add(current);
                }
                else
                    break;
            }
        }while(current !=null);
        if(symItem == null){
            typeErrors.add("Line:"+ identifier.getLineNum() + ":variable " + identifier.getName() + " is not declared");
            identifier.setType(new NoType());
        }
        else{
            identifier.setType(symItem.getType());
        }
    }//cok

    @Override
    public void visit(Length length) {
        if (length == null)
            return;
        visitExpr(length.getExpression());
        if (!T2isT1Subtype(new ArrayType(), length.getExpression().getType())) {
            typeErrors.add("Line:" + length.getLineNum() +
                    ":invalid use of length");
            length.setType(new NoType());
        }
        else
            length.setType(new IntType());
    }//cok

    @Override
    public void visit (MethodCall methodCall){
        boolean ok=true;
        if (methodCall == null)
            return;
        try {
            visitExpr(methodCall.getInstance());
            checkMethodCallInstance(methodCall.getInstance());

            MethodDeclaration md = checkForMethodExistence(methodCall);
            if(md==null)
                ok = false;
            for (Expression argument : methodCall.getArgs())
                visitExpr(argument);
            if(md!=null){
                ok = checkMethodArgs(md, methodCall);
            }
            if(ok){
                methodCall.setType(md.getReturnValue().getType());
            }
        } catch (NullPointerException npe) {
            System.out.println("syntax error occurred");
        }
    }//cok

    @Override
    public void visit (NewArray newArray){
        if (newArray == null)
            return;
        visitExpr(newArray.getExpression());
        if(!(T2isT1Subtype(new IntType(), newArray.getExpression().getType()))){
            typeErrors.add("Line:" + newArray.getLineNum() + ":array size cannot be of type " + newArray.getExpression().getType().toString());
        }
        else{
            newArray.setType(new ArrayType());
        }
    }//cok

    @Override
    public void visit (NewClass newClass){

        if (newClass == null)
            return;
        //visitExpr(newClass.getClassName());
        //System.out.println(newClass.getClassName());
        UserDefinedType t = new UserDefinedType();
        t.setName(newClass.getClassName());
        if(checkForClassExistence(t)==null){
            newClass.setType(new NoType());
            typeErrors.add("Line:" + newClass.getLineNum() + ":class "
                    + newClass.getClassName().getName() + " is not declared");
        }
        else{
            newClass.setType(t);
        }

    }//cok

    @Override
    public void visit (This instance){
        UserDefinedType t = new UserDefinedType();
        t.setName(new Identifier(scope));
        instance.setType(t);
    } //cok

    @Override
    public void visit (UnaryExpression unaryExpression){
        if (unaryExpression == null)
            return;
        try {
            visitExpr(unaryExpression.getValue());
            checkUnaryExpression(unaryExpression);
        } catch (NullPointerException npe) {
            System.out.println("unary value is null");
        }
    }//cok

    @Override
    public void visit (BooleanValue value){

    }//cok

    @Override
    public void visit (IntValue value){

    }//cok

    @Override
    public void visit (StringValue value){

    }//cok

    @Override
    public void visit (Assign assign){
        //System.out.println("Assign Statement");
        if (assign == null)
            return;
        try {
            Expression lExpr = assign.getlValue();
            //System.out.println(lExpr.toString());
            visitExpr(lExpr);
            //System.out.println("lexpr visited");
            Expression rValExpr = assign.getrValue();
            if (rValExpr != null) {
                validateLvalue(lExpr);
                visitExpr(rValExpr);
                if (!T2isT1Subtype(lExpr.getType(), rValExpr.getType())) {
                    typeErrors.add("Line:" + assign.getLineNum() +
                            ":incompatible operands of type " + lExpr.getType().toString() + " and " + rValExpr.getType().toString());
                }
            }
        } catch (NullPointerException npe) {
            System.out.println("lvalue expression is null");
        }
        //System.out.println("end of Assign Statement");
    }//ok and Type handled

    @Override
    public void visit (Block block){
        if (block == null)
            return;
        for (Statement blockStat : block.getBody())
            this.visitStatement(blockStat);
    }//cok

    @Override
    public void visit (Conditional conditional){
        if (conditional == null)
            return;
        visitExpr(conditional.getExpression());
        checkConditionType(conditional.getExpression());
        visitStatement(conditional.getConsequenceBody());
        visitStatement(conditional.getAlternativeBody());
    }//cok

    @Override
    public void visit (While loop){
        if (loop == null)
            return;
        visitExpr(loop.getCondition());
        checkConditionType(loop.getCondition());
        visitStatement(loop.getBody());
    }//cok

    @Override
    public void visit (Write write){
        if (write == null)
            return;
        visitExpr(write.getArg());
        checkWriteArgType(write.getArg());
    }//cok
}




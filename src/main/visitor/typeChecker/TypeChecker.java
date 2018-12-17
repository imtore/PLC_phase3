package main.visitor.typeChecker;

import main.ast.Type.NoType;
import main.ast.Type.UserDefinedType.UserDefinedType;
import main.ast.node.Node;
import main.ast.node.Program;
import main.ast.node.declaration.ClassDeclaration;
import main.ast.node.declaration.MainMethodDeclaration;
import main.ast.node.declaration.MethodDeclaration;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.Value.BooleanValue;
import main.ast.node.expression.Value.IntValue;
import main.ast.node.expression.Value.StringValue;
import main.ast.node.statement.*;
import main.ast.Type.PrimitiveType.*;
import main.ast.Type.ArrayType.*;
import main.ast.Type.UserDefinedType.UserDefinedType;
import main.ast.Type.Type;
import main.symbolTable.ClassSymbolTableItem;
import main.symbolTable.SymbolTable;
import main.symbolTable.SymbolTableMethodItem;
import main.symbolTable.itemException.ItemNotFoundException;
import main.visitor.VisitorImpl;


import java.util.ArrayList;

public class TypeChecker extends VisitorImpl {
    private TraverseState traverseState;
    private ArrayList<String> typeErrors;

    public TypeChecker()
    {
        typeErrors = new ArrayList<>();
        setState( TraverseState.TypeAndUsageErrorCatching );
    }
    public int numOfErrors()
    {
        return typeErrors.size();
    }

    private void switchState()
    {
        if( traverseState.name().equals( TraverseState.TypeAndUsageErrorCatching.toString() ) && typeErrors.size() != 0 )
            setState( TraverseState.PrintError );
        else
            setState( TraverseState.Exit );
    }

    private void setState( TraverseState traverseState )
    {
        this.traverseState = traverseState;
    }

    //TODO: some functions for error chatching
    private void enterScope(ClassDeclaration classDeclaration) {
        String name = classDeclaration.getName().getName();
        try {
            ClassSymbolTableItem classItem = (ClassSymbolTableItem) SymbolTable.root.getInCurrentScope(ClassSymbolTableItem.CLASS + name);
            SymbolTable next = classItem.getClassSym();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound) {
            System.out.println( "there is an error in pushing class symbol table" );
        }
    }

    private void enterScope(MethodDeclaration methodDeclaration) {
        String name = methodDeclaration.getName().getName();
        try {
            SymbolTableMethodItem methodItem = (SymbolTableMethodItem) SymbolTable.top.getInCurrentScope(SymbolTableMethodItem.METHOD + name);
            SymbolTable next = methodItem.getMethodSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound) {
            System.out.println( "there is an error in pushing method symbol table" );
        }
    }


    private void checkForParentExistence(ClassDeclaration classDeclaration) {
        String parent = classDeclaration.getParentName().getName();
        SymbolTable classSymPre = SymbolTable.top.getPreSymbolTable();
        if(parent!=null && classSymPre==null) {
            typeErrors.add("Line:" + classDeclaration.getName().getLineNum() +
                    ":class " + parent + " is not declared");
        }
    }

    private void checkConditionType(Expression condition) {
        if(!(condition.getType() instanceof  BooleanType) || !(condition.getType() instanceof NoType)) {
            typeErrors.add("Line:" + condition.getLineNum() +
                    ":condition type must be boolean");
        }
    }

    private void checkWriteArgType(Expression arg) {
        if(!(arg.getType() instanceof IntType || arg.getType() instanceof StringType
                || arg.getType() instanceof ArrayType || arg.getType() instanceof NoType)) {
            typeErrors.add("Line:" + arg.getLineNum() +
                    ":unsupported type for writeln");
        }
    }

    private Boolean T2isT1Subtype(Type t1, Type t2){
        if(t2 instanceof NoType || t1 instanceof NoType){
            return true;
        }
        else if(t1 instanceof UserDefinedType){
            if(t2 instanceof UserDefinedType){
                Identifier currentType = ((UserDefinedType) t2).getName();
                ClassSymbolTableItem parent = null;
                ClassDeclaration current = ((UserDefinedType) t2).getClassDeclaration();
                do {
                    if(currentType==((UserDefinedType) t1).getName()){
                        return true;
                    }
                    if(current.getParentName()!=null) {
                        try {

                            parent = (ClassSymbolTableItem) SymbolTable.root
                                    .getInCurrentScope(ClassSymbolTableItem.CLASS + current.getParentName().getName());
                            current = parent.getClassDeclaration();
                            currentType = current.getName();
                        } catch (ItemNotFoundException itemNotFound) {
                            parent = null;
                        }
                    }
                    else
                        return false;
                }while(parent!=null);
                return false;
            }
            else
                return false;
        }
        else if(t1 instanceof ArrayType){
            if(t2 instanceof ArrayType)
                return true;
            else
                return false;
        }
        else if(t1 instanceof BooleanType){
            if(t2 instanceof BooleanType)
                return true;
            else
                return false;
        }
        else if(t1 instanceof IntType){
            if(t2 instanceof IntType)
                return true;
            else
                return false;
        }
        else if(t1 instanceof StringType){
            if(t2 instanceof StringType)
                return true;
            else
                return false;
        }
        else{
            System.out.println( "there is an error in checking subtyping" );
            return false;
        }
    }

    private void checkBinaryExpression(BinaryExpression binaryExpression){
        if (binaryOperator.getBinaryOperator() == '*'){
            if(!T2isT1Subtype(new IntType(), arrayCall.getIndex().getType()))
        }

    }
    
    private void checkReturnType(MethodDeclaration methodDeclaration){
        if (!T2isT1Subtype(methodDeclaration.getActualReturnType(), methodDeclaration.getReturnValue().getType())){
            typeErrors.add("Line:" + methodDeclaration.getReturnValue().getLineNum() +
                    ":" + methodDeclaration.getName().getName() + " return type must be " + methodDeclaration.getActualReturnType().toString());
        }
    }

    private void validateLvalue(Expression lvalue){
        if(!(lvalue instanceof ArrayCall || lvalue instanceof Identifier)){
            typeErrors.add("Line:" + lvalue.getLineNum() +
                    ":left side of assignment must be a valid lvalue");
        }
    }



    @Override
    public void visit(Node node) {

    }

    @Override
    public void visit(Program program){//ok somehow
        //TODO: implement appropriate visit functionality
        while( !traverseState.toString().equals( main.visitor.typeChecker.TraverseState.Exit.toString() )){
            if (traverseState.name().equals(TraverseState.TypeAndUsageErrorCatching.toString())) {
                //TODO: still don't know
                //guess there's nothing to do about this except in nameAnalyzer....
            }
            else if( traverseState.name().equals( main.visitor.typeChecker.TraverseState.PrintError.toString() ) ) {
                for (String error : typeErrors)
                    System.out.println(error);
                return;
            }
            this.visit(program.getMainClass());
            for (ClassDeclaration classDeclaration : program.getClasses())
                this.visit(classDeclaration);
            switchState();
        }
    }

    @Override
    public void visit(ClassDeclaration classDeclaration) {
        //TODO: implement appropriate visit functionality
        if( classDeclaration == null )
            return;
        else if( traverseState.name().equals( TraverseState.TypeAndUsageErrorCatching.toString() ) ) {
            checkForParentExistence(classDeclaration);//TODO: implement function
            enterScope(classDeclaration);
        }
        visitExpr( classDeclaration.getName() );
        visitExpr( classDeclaration.getParentName() );
        for( VarDeclaration varDeclaration: classDeclaration.getVarDeclarations() )
            this.visit( varDeclaration );
        for( MethodDeclaration methodDeclaration: classDeclaration.getMethodDeclarations() )
            this.visit( methodDeclaration );
        SymbolTable.pop();
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration) {
        if( methodDeclaration == null )
            return;
        else if( traverseState.name().equals( TraverseState.TypeAndUsageErrorCatching.toString() ) )
            enterScope( methodDeclaration );
        for( VarDeclaration argDeclaration: methodDeclaration.getArgs() )
            visit( argDeclaration );
        for( VarDeclaration localVariable: methodDeclaration.getLocalVars() )
            this.visit( localVariable );
        for( Statement statement : methodDeclaration.getBody() )
            visitStatement( statement );
        visitExpr( methodDeclaration.getReturnValue() );
        checkReturnType(methodDeclaration);
        SymbolTable.pop();
    }

    @Override
    public void visit(MainMethodDeclaration mainMethodDeclaration) {
        if( mainMethodDeclaration == null )
            return;
        else if( traverseState.name().equals( TraverseState.TypeAndUsageErrorCatching.toString()) )
            visit( ( MethodDeclaration ) mainMethodDeclaration );
        for( Statement statement : mainMethodDeclaration.getBody() )
            visitStatement( statement );
        visitExpr( mainMethodDeclaration.getReturnValue() );
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        if( varDeclaration == null )
            return;
        visitExpr( varDeclaration.getIdentifier() );
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        if( arrayCall == null )
            return;
        try {
            visitExpr( arrayCall.getInstance() );
            if(!T2isT1Subtype(new ArrayType(), arrayCall.getInstance().getType())){
                typeErrors.add("Line:" + arrayCall.getInstance().getLineNum() +
                        ":invalid array access on a non array object");
            }
            visitExpr( arrayCall.getIndex() );
            if(!T2isT1Subtype(new IntType(), arrayCall.getIndex().getType())){
                typeErrors.add("Line:" + arrayCall.getIndex().getLineNum() +
                        ":array index must be of type int");
            }
        }
        catch( NullPointerException npe )
        {
            System.out.println( "instance or index is null" );//TODO: what to do if in type checking index or instance was null
        }

    }//TODO: q inside

    @Override
    public void visit(BinaryExpression binaryExpression) {
        //TODO: implement appropriate visit functionality
        if( binaryExpression == null )
            return;
        Expression lOperand = binaryExpression.getLeft();
        Expression rOperand = binaryExpression.getRight();
        try {
            visitExpr(lOperand);
            visitExpr(rOperand);
            checkBinaryExpression(binaryExpression);
        }
        catch( NullPointerException npe )
        {
            System.out.println( "one of operands is null, there is a syntax error" );
        }
    }

    @Override
    public void visit(Identifier identifier) {
        //TODO: implement appropriate visit functionality

    }

    @Override
    public void visit(Length length) {
        if( length == null )
            return;
        visitExpr( length.getExpression() );
        if(!T2isT1Subtype(new ArrayType(), Length.getExpression().getType())){
            typeErrors.add("Line:" + arrayCall.getExpression().getLineNum() +
                    ":invalid use of length");
            length.setType() = new NoType();
    }

    @Override
    public void visit(MethodCall methodCall) {
        if( methodCall == null )
            return;
        try {
            visitExpr(methodCall.getInstance());
            visitExpr(methodCall.getMethodName());
            for (Expression argument : methodCall.getArgs())
                visitExpr(argument);
        }
        catch( NullPointerException npe )
        {
            System.out.println( "syntax error occurred" );
        }
        //TODO: implement appropriate visit functionality
        // does the class exists 
        // does it have this method
    }

    @Override
    public void visit(NewArray newArray) {
        if( newArray == null )
            return;
        visitExpr( newArray.getExpression() );
    }

    @Override
    public void visit(NewClass newClass) {
        //TODO: implement appropriate visit functionality
        if( newClass == null )
            return;
        visitExpr( newClass.getClassName() );
    }

    @Override
    public void visit(This instance) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        if( unaryExpression == null )
            return;
        try {
            visitExpr(unaryExpression.getValue());
            if(!T2isT1Subtype(new IntType(), naryExpression.getValue().getType()){
                typeErrors.add("Line:" + unaryExpression.getValue().getLineNum() +
                        ":invalid use of unary expression , no valid type");
                unaryExpression.getValue().setType() = new NoType();
            }
        }
        catch( NullPointerException npe )
        {
            System.out.println( "unary value is null" );
        }
    }

    @Override
    public void visit(BooleanValue value) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(IntValue value) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(StringValue value) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(Assign assign) {
        if( assign == null )
            return;
        try {
            Expression lExpr = assign.getlValue();
            visitExpr(lExpr);
            Expression rValExpr = assign.getrValue();
            if (rValExpr != null)
                validateLvalue(lExpr);
                visitExpr(rValExpr);
                if(!T2isT1Subtype(lExpr.getType(), rValExpr.getType())) {
                    typeErrors.add("Line:" + assign.getLineNum() +
                            ":incompatible operands of type " + lExpr.getType().toString() + " and " + rValExpr.getType().toString());
                }
        }
        catch( NullPointerException npe )
        {
            System.out.println( "lvalue expression is null" );
        }
    }

    @Override
    public void visit(Block block) {
        if( block == null )
            return;
        for( Statement blockStat : block.getBody() )
            this.visitStatement( blockStat );
    }

    @Override
    public void visit(Conditional conditional) {
        if( conditional == null )
            return;
        visitExpr( conditional.getExpression() );
        checkConditionType(conditional.getExpression());
        visitStatement( conditional.getConsequenceBody() );
        visitStatement( conditional.getAlternativeBody() );
    }

    @Override
    public void visit(While loop) {
        if( loop == null )
            return;
        visitExpr( loop.getCondition() );
        checkConditionType(loop.getCondition());
        visitStatement( loop.getBody() );
    }

    @Override
    public void visit(Write write) {
        if( write == null )
            return;
        visitExpr( write.getArg() );
        checkWriteArgType(write.getArg());
    }
}



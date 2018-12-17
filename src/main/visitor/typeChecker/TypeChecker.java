package main.visitor.typeChecker;

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
import main.symbolTable.SymbolTable;
import main.visitor.VisitorImpl;
import sun.jvm.hotspot.debugger.cdbg.Sym;

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
    private void checkForParentExistence(ClassDeclaration classDeclaration) {
        String parent = classDeclaration.getParentName().getName();
        SymbolTable classSymPre = SymbolTable.top.getPreSymbolTable();
        if(parent!=null && classSymPre==null) {
            typeErrors.add("Line:" + classDeclaration.getName().getLineNum() +
                    ":class " + parent + " is not declared");
        }
    }



    @Override
    public void visit(Node node) {
        //TODO: implement appropriate visit functionality
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
        else if( traverseState.name().equals( TraverseState.TypeAndUsageErrorCatching.toString() ) )
            checkForParentExistence( classDeclaration );//TODO: implement function
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
        //TODO: implement appropriate visit functionality
        if( methodDeclaration == null )
            return;
        else if( traverseState.name().equals( TraverseState.TypeAndUsageErrorCatching.toString() ) )
            checkForPropertyRedefinition( methodDeclaration );
        for( VarDeclaration argDeclaration: methodDeclaration.getArgs() )
            visit( argDeclaration );
        for( VarDeclaration localVariable: methodDeclaration.getLocalVars() )
            this.visit( localVariable );
        for( Statement statement : methodDeclaration.getBody() )
            visitStatement( statement );
        visitExpr( methodDeclaration.getReturnValue() );
        SymbolTable.pop();
    }

    @Override
    public void visit(MainMethodDeclaration mainMethodDeclaration) {
        //TODO: implement appropriate visit functionality
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
        //TODO: implement appropriate visit functionality
        if( varDeclaration == null )
            return;
        if( traverseState.name().equals( TraverseState.TypeAndUsageErrorCatching.toString() ) )
            checkForPropertyRedefinition( varDeclaration );
        visitExpr( varDeclaration.getIdentifier() );
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        //TODO: implement appropriate visit functionality
        if( arrayCall == null )
            return;
        try {
            visitExpr( arrayCall.getInstance() );
            visitExpr( arrayCall.getIndex() );
        }
        catch( NullPointerException npe )
        {
            System.out.println( "instance or index is null" );
        }

    }

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
        //TODO: implement appropriate visit functionality
        if( length == null )
            return;
        visitExpr( length.getExpression() );
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
    }

    @Override
    public void visit(NewArray newArray) {
        //TODO: implement appropriate visit functionality
        if( newArray == null )
            return;
        if( traverseState.name().equals( TraverseState.ErrorCatching.toString() ) )
            if( newArray.getExpression() instanceof IntValue && ((IntValue) newArray.getExpression()).getConstant() <= 0 )
            {
                nameErrors.add( "Line:" + newArray.getExpression().getLineNum() + ":Array length should not be zero or negative" );
                ((IntValue) newArray.getExpression()).setConstant( 0 );
            }
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
        //TODO: implement appropriate visit functionality
        if( unaryExpression == null )
            return;
        try {
            visitExpr(unaryExpression.getValue());
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
        //TODO: implement appropriate visit functionality
        if( assign == null )
            return;
        try {
            Expression lExpr = assign.getlValue();
            visitExpr(lExpr);
            Expression rValExpr = assign.getrValue();
            if (rValExpr != null)
                visitExpr(rValExpr);
        }
        catch( NullPointerException npe )
        {
            System.out.println( "lvalue expression is null" );
        }
    }

    @Override
    public void visit(Block block) {
        //TODO: implement appropriate visit functionality
        if( block == null )
            return;
        for( Statement blockStat : block.getBody() )
            this.visitStatement( blockStat );
    }

    @Override
    public void visit(Conditional conditional) {
        //TODO: implement appropriate visit functionality
        if( conditional == null )
            return;
        visitExpr( conditional.getExpression() );
        visitStatement( conditional.getConsequenceBody() );
        visitStatement( conditional.getAlternativeBody() );
    }

    @Override
    public void visit(While loop) {
        //TODO: implement appropriate visit functionality
        if( loop == null )
            return;
        visitExpr( loop.getCondition() );
        visitStatement( loop.getBody() );

    }

    @Override
    public void visit(Write write) {
        //TODO: implement appropriate visit functionality
        if( write == null )
            return;
        visitExpr( write.getArg() );
    }
}

}

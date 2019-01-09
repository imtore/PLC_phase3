package main.visitor.codeGenerator;


import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.tools.javac.jvm.Code;
import main.ast.Type.ArrayType.ArrayType;
import main.ast.Type.PrimitiveType.BooleanType;
import main.ast.Type.PrimitiveType.StringType;
import main.ast.Type.Type;
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
import main.visitor.VisitorImpl;
import sun.jvm.hotspot.debugger.cdbg.IntType;


import javax.swing.text.StyledEditorKit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CodeGenerator extends VisitorImpl {

    public static final String DIR="../output/";
    public static final String FORMAT=".j";
    public static final String NL="\n";
    public static final String T="\t";
    private ArrayList<String> classCode;
//    private boolean Done;
    private String currentClass;

    private int currentLabel;

    public CodeGenerator() {
        classCode = new ArrayList<>();
//        Done = false;
        currentClass = "object";
        currentLabel = 0;
    }

//    private classHeaderGenerator(){}

    private String classFieldGen(VarDeclaration varDeclaration){
        String gen = ".field private ";
        gen += varDeclaration.getIdentifier().getName();
        Type type = varDeclaration.getType();
        if(type instanceof IntType){//TODO: when type is int, it doesn't go in this if statement
            gen += " I";
        }
        else if(type instanceof BooleanType){
            gen += " Z";
        }
        else if(type instanceof StringType){
            gen += " Ljava/lang/String;";
        }
        else if(type instanceof UserDefinedType){
            gen += " L" + ((UserDefinedType)varDeclaration.getType()).getName().getName() + ";" ;
        }
        else if(type instanceof ArrayType){
            gen += " [I";
        }
        return gen;
    }

    private void writeInFile(){
        String filepath = CodeGenerator.DIR + currentClass + CodeGenerator.FORMAT;
        try {
            File file = new File(filepath);
            if (!file.createNewFile()){
                System.out.println("file cannot be created");
            }
            FileWriter fileWriter = new FileWriter(filepath);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            for (String line : classCode){
                printWriter.print(line + CodeGenerator.NL);
            }
            printWriter.close();
        }catch(IOException ioe){
            System.out.println(ioe.getLocalizedMessage());
            return;
        }
        classCode.clear();

    }

    @Override
    public void visit(Node node) {

    }

    @Override
    public void visit(Program program) {

        //make object
        classCode.add(".class public " + currentClass);
        classCode.add(".super java/lang/Object");
        //what else to write?
        writeInFile();

        this.visit(program.getMainClass());

//
//            for (ClassDeclaration classDeclaration : program.getClasses()) {
//                this.visit(classDeclaration);
//            }

//                writeInFile();//pass args
//                //clear the arrayList

    }

    @Override
    public void visit(ClassDeclaration classDeclaration) {

        //what to do about symbol table?

        currentClass = classDeclaration.getName().getName();
        classCode.add(".class public " + currentClass);
        if(classDeclaration.getParentName() == null)
            classCode.add(".super object.j");
        else
            classCode.add(".super " + classDeclaration.getParentName().getName() + CodeGenerator.FORMAT);
        classCode.add(CodeGenerator.NL);

        //constructor
        classCode.add(".method public <init>()V");
        classCode.add(CodeGenerator.T + ".limit stack 20");
        classCode.add(CodeGenerator.T + ".limit locals 20");
        classCode.add(CodeGenerator.T + "aload_0");
        if(classDeclaration.getParentName() == null)
            classCode.add(CodeGenerator.T + "invokespecial java/lang/Object/<init>()V");
        else
            classCode.add(CodeGenerator.T + "invokespecial " + classDeclaration.getParentName() + "/<init>()V");

        classCode.add(CodeGenerator.T + "return");
        classCode.add(".end method");


        for (VarDeclaration varDeclaration : classDeclaration.getVarDeclarations())
            classCode.add(classFieldGen(varDeclaration));


        for (MethodDeclaration methodDeclaration : classDeclaration.getMethodDeclarations())
            if(methodDeclaration instanceof MainMethodDeclaration){
                this.visit((MainMethodDeclaration) methodDeclaration);
            }
            else {
                this.visit(methodDeclaration);
            }

//        SymbolTable.pop();

        writeInFile();
    }



    @Override
    public void visit(MethodDeclaration methodDeclaration) {
        //System.out.println("Method Declaration");
//        System.out.println(scope);
//        if (methodDeclaration == null)
//            return;
//        else if (traverseState.name().equals(TraverseState.TypeAndUsageErrorCatching.toString()))
//        enterScope(methodDeclaration);
//        System.out.println("entered method scope");
//        for (VarDeclaration argDeclaration : methodDeclaration.getArgs())
//            visit(argDeclaration);
//        System.out.println("argument declarations are visited");
//        for (VarDeclaration localVariable : methodDeclaration.getLocalVars())
//            this.visit(localVariable);
//        System.out.println("local variables are visited");
//        System.out.println(methodDeclaration.getLocalVars().size());
//        System.out.println(methodDeclaration.getBody().size());
//        for (Statement statement : methodDeclaration.getBody())
//            visitStatement(statement);
//        System.out.println("statements are visited");

//        visitExpr(methodDeclaration.getReturnValue());
//        checkReturnType(methodDeclaration);
//        SymbolTable.pop();
//        System.out.println("*");
    }

    //TODO: popping unused objects from stack!!
    //TODO: farghe aload_0 ba aload 0 :/
    @Override
    public void visit(MainMethodDeclaration mainMethodDeclaration) {

        //should return type be void like java?? how to run this if not??
        classCode.add(".method public static main()I");
        classCode.add(CodeGenerator.T + ".limit stack 8");
        classCode.add(CodeGenerator.T + ".limit locals 10");
        classCode.add(CodeGenerator.NL);

        for (Statement statement : mainMethodDeclaration.getBody()) {
            visitStatement(statement);
        }

        visitExpr(mainMethodDeclaration.getReturnValue());

        classCode.add(CodeGenerator.T + "ireturn");
        classCode.add(".end method");
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
//        if (varDeclaration == null)
//            return;
        //visitExpr(varDeclaration.getIdentifier());
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        visitExpr(arrayCall.getInstance());
        visitExpr(arrayCall.getIndex());
    }//done

    @Override
    public void visit(BinaryExpression binaryExpression) {
//
//        if (binaryExpression == null)
//            return;
//        Expression lOperand = binaryExpression.getLeft();
//        Expression rOperand = binaryExpression.getRight();
//        try {
//            visitExpr(lOperand);
//            visitExpr(rOperand);
//            checkBinaryExpression(binaryExpression);
//        } catch (NullPointerException npe) {
//            System.out.println("one of operands is null, there is a syntax error");
//        }
    }

    @Override
    public void visit(Identifier identifier) {
//        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
//        String key = SymbolTableVariableItemBase.VARIABLE + identifier.getName();
//        SymbolTable current = SymbolTable.top;
//        visitedSymbolTables.add(current);
//        SymbolTableVariableItemBase symItem = null;
//        do {
//            try {
//                symItem = (SymbolTableVariableItemBase) current.getInCurrentScope(key);
//                break;
//            }
//            catch(ItemNotFoundException itemNotFound){
//                if(current.getPreSymbolTable()!=null) {
//                    current = current.getPreSymbolTable();
//                    if (visitedSymbolTables.contains(current))
//                        break;
//                    visitedSymbolTables.add(current);
//                }
//                else
//                    break;
//            }
//        }while(current !=null);
//        if(symItem == null){
//            typeErrors.add("Line:"+ identifier.getLineNum() + ":variable " + identifier.getName() + " is not declared");
//            identifier.setType(new NoType());
//        }
//        else{
//            identifier.setType(symItem.getType());
//        }
    }

    @Override
    public void visit(Length length) {
        visitExpr(length.getExpression());
        classCode.add(CodeGenerator.T + "arraylength");
    }//done

    @Override
    public void visit (MethodCall methodCall){
//        boolean ok=true;
//        if (methodCall == null)
//            return;
//        try {
//            visitExpr(methodCall.getInstance());
//            checkMethodCallInstance(methodCall.getInstance());
//
//            MethodDeclaration md = checkForMethodExistence(methodCall);
//            if(md==null)
//                ok = false;
//            for (Expression argument : methodCall.getArgs())
//                visitExpr(argument);
//            if(md!=null){
//                ok = checkMethodArgs(md, methodCall);
//            }
//            if(ok){
//                methodCall.setType(md.getReturnValue().getType());
//            }
//        } catch (NullPointerException npe) {
//            System.out.println("syntax error occurred");
//        }
    }

    @Override
    public void visit (NewArray newArray){
        visitExpr(newArray.getExpression());
        classCode.add(CodeGenerator.T + "newarray int");
    }//done

    @Override
    public void visit (NewClass newClass){
        classCode.add(CodeGenerator.T + "new " + newClass.getClassName());
        classCode.add(CodeGenerator.T + "dup");
        classCode.add(CodeGenerator.T + "invokespecial " + newClass.getClassName() + "/<init>()V");
    }//done

    @Override
    public void visit (This instance){

        classCode.add(CodeGenerator.T + "aload 0");
    }//done

    @Override
    public void visit (UnaryExpression unaryExpression){

         visitExpr(unaryExpression.getValue());

         if(unaryExpression.getValue().getType() instanceof IntType)
             classCode.add(CodeGenerator.T + "ineg");
         else if(unaryExpression.getValue().getType() instanceof BooleanType) {
             classCode.add(CodeGenerator.T + "ifne " + "Label" + currentLabel);
             int first = currentLabel;
             currentLabel++;
             classCode.add(CodeGenerator.T + "iconst_1");
             classCode.add(CodeGenerator.T + "goto " + "Label" + currentLabel);
             int second = currentLabel;
             currentLabel++;
             classCode.add(CodeGenerator.T + "Label" + first + ":");
             classCode.add(CodeGenerator.T + "iconst_0");
             classCode.add(CodeGenerator.T + "Label" + second + ":");

         }
    }//done

    @Override
    public void visit (BooleanValue value){
        if(value.isConstant())
            classCode.add(CodeGenerator.T + "iconst_1");
        else
            classCode.add(CodeGenerator.T + "iconst_0");
    }//done

    @Override
    public void visit (IntValue value){

        classCode.add(CodeGenerator.T + "bipush " + value.getConstant());
    }//done

    @Override
    public void visit (StringValue value){

        classCode.add(CodeGenerator.T + "ldc " + value.getConstant());
    }//done

    @Override
    public void visit (Assign assign){
//        //System.out.println("Assign Statement");
//        if (assign == null)
//            return;
//        try {
//            Expression lExpr = assign.getlValue();
//            //System.out.println(lExpr.toString());
//            visitExpr(lExpr);
//            //System.out.println("lexpr visited");
//            Expression rValExpr = assign.getrValue();
//            if (rValExpr != null) {
//                validateLvalue(lExpr);
//                visitExpr(rValExpr);
//                if (!T2isT1Subtype(lExpr.getType(), rValExpr.getType())) {
//                    typeErrors.add("Line:" + assign.getLineNum() +
//                            ":incompatible operands of type " + lExpr.getType().toString() + " and " + rValExpr.getType().toString());
//                }
//            }
//        } catch (NullPointerException npe) {
//            System.out.println("lvalue expression is null");
//        }

    }

    @Override
    public void visit (Block block){
        for (Statement blockStat : block.getBody())
            this.visitStatement(blockStat);
    }//done

    @Override
    public void visit (Conditional conditional){

        visitExpr(conditional.getExpression());

        int nthen = currentLabel;
        classCode.add(CodeGenerator.T + "ifneq " + "Label" + nthen);
        currentLabel++;

        visitStatement(conditional.getAlternativeBody());

        int nafter = currentLabel;
        classCode.add(CodeGenerator.T + "goto " + "Label" + nafter);
        currentLabel++;

        classCode.add(CodeGenerator.T + "Label" + nthen + ":");

        visitStatement(conditional.getConsequenceBody());

        classCode.add(CodeGenerator.T + "Label" + nafter + ":");

    }//done

    @Override
    public void visit (While loop){

        int nstart = currentLabel;
        classCode.add(CodeGenerator.T + "Label" + nstart + ":");
        currentLabel++;

        visitExpr(loop.getCondition());

        int nexit = currentLabel;
        classCode.add(CodeGenerator.T + "ifeq " + "Label" + nexit);
        currentLabel++;

        visitStatement(loop.getBody());

        classCode.add(CodeGenerator.T + "goto " + "Label" + nstart);

        classCode.add(CodeGenerator.T + "Label" + nexit + ":");

    }//done

    @Override
    public void visit (Write write){

        //get a print stream
        classCode.add(CodeGenerator.T + "getstatic java/lang/System/out Ljava/io/PrintStream;");

        visitExpr(write.getArg()); //pushes args to stack

        //invoke
        //same problem with int type here!!! toString of type is int! but is not instance of IntType
        if(write.getArg().getType() instanceof IntType){
            classCode.add(CodeGenerator.T + "invokevirtual java/io/PrintStream/println(I)V");
        }
        else if(write.getArg().getType() instanceof StringType){
            classCode.add(CodeGenerator.T + "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
        }
        else if(write.getArg().getType() instanceof ArrayType){
            classCode.add(CodeGenerator.T + "invokevirtual java/io/PrintStream/println(Ljava/lang/Object;)V");
        }
    }//done **array is not tested
}









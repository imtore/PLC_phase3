.class public Main
.super object.j


.method public static main()I
	.limit stack 8
	.limit locals 10


	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc "hello world"
	invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
	bipush 0
	ireturn
.end method

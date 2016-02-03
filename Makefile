JAVA=javac

compiler:
	javac -cp SaM-2.6.2.jar BaliCompiler.java
run:
	java -cp SaM-2.6.2.jar:. BaliCompiler  tests/bad.expr-2.bali good.break.sam

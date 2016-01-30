JAVA=javac

compiler:
	javac -cp SaM-2.6.2.jar BaliCompiler.java
run:
	java -cp SaM-2.6.2.jar:. BaliCompiler  good.break.bali good.break.sam

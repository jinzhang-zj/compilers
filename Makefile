JAVA=javac

compiler:
	javac -cp SaM-2.6.2.jar BaliCompiler.java
run:
	java -cp SaM-2.6.2.jar:. BaliCompiler  absolute.bali good.break.sam

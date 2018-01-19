all: src/*.java
	javac -sourcepath src -d out src/Main.java

clean:
	rm -rf out/*
run:	
	javac -sourcepath src -d out src/Main.java
	java -cp out Main Lab1.map 20 20

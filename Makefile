all: src/*.java
	javac -sourcepath src -d out/production/dit391lab1 src/Main.java

clean:
	rm -rf out/production/dit391lab1/*
run:	
	javac -sourcepath src -d out/production/dit391lab1 src/Main.java
	java -cp out/production/dit391lab1 Main Lab1.map 20 20 10

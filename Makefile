JAVAC = javac
JAVA  = java

SRC_PATH = src
ENTRY = Entry
SRC = $(shell find src -name '*.java')
SRC_CLASS = $(shell find src -name '*.class')
TEST = $(shell find testcases -name '*.java')
TEST_CLASS = $(shell find testcases -name '*.class')
CP1 = .
CP2 = lib/soot-4.6.0-jar-with-dependencies.jar

.PHONY: compile run

compile:
	$(JAVAC) -cp $(CP1):$(CP2) $(SRC)
	$(JAVAC) -cp $(CP1) $(TEST)

run: compile
	$(JAVA) -cp $(CP1):$(CP2) $(SRC_PATH)/$(ENTRY) $(ARG)

clean:
	rm -rf $(SRC_CLASS)
	rm -rf $(TEST_CLASS)
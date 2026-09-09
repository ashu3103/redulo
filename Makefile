JAVAC = javac
JAVA  = java

CLASS_NAME ?= Test1
TEST_DIR = testcases/$(CLASS_NAME)

ENTRY = Entry
SRC = $(shell find . -maxdepth 1 -name '*.java')
SRC_CLASS = $(shell find . -name '*.class')
TEST = $(shell find $(TEST_DIR) -name '*.java')
TEST_CLASS = $(shell find $(TEST_DIR) -name '*.class')
CP1 = .
CP2 = lib/soot-4.6.0-jar-with-dependencies.jar

.PHONY: compile run

compile:
	$(JAVAC) -cp $(CP1):$(CP2) $(SRC)
	$(JAVAC) -cp $(CP1) $(TEST)

run: compile
	$(JAVA) -cp $(CP1):$(CP2) $(ENTRY) $(CLASS_NAME)

clean:
	rm -rf $(SRC_CLASS)
	rm -rf $(TEST_CLASS)

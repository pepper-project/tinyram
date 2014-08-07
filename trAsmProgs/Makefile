
SRCFILES:=build.xml $(shell find src -type f -name '*.java' -or -name '*.g')
include build.prop

all: dist

dist: $(DIST)/trAsmProgs.jar

$(DIST)/trAsmProgs.jar: $(SRCFILES)
	ant dist

build: $(BUILD)/build.foo

$(BUILD)/build.foo: $(SRCFILES)
	ant compile
	touch $@

.PHONY : clean
clean:
	ant clean


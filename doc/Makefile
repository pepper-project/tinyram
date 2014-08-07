
.SUFFIXES: .md .html

PANDOC_CMD := pandoc --no-wrap -sS -t html -H htmlsrc/head.html -A htmlsrc/tail.html
HTMLOUTPUTDIR := htmlgen

all: README.html TrAsm.html TrSim.html TrDasm.html TrVer.html TrExo.html asm

.md.html:
	@mkdir -p ${HTMLOUTPUTDIR}
	@echo -n "Processing $<... "
	@cat $< | ${PANDOC_CMD} > ${HTMLOUTPUTDIR}/$@
	@echo "done."
	@echo

.PHONY: clean
clean:
	@echo "Cleaning up..."
	@rm -rf ${HTMLOUTPUTDIR}
	@$(MAKE) -C asm clean
	@echo Done.
	@echo

.PHONY: asm
asm:
	@echo "Building assembler documentation..."
	@$(MAKE) -C asm
	@echo "Done."
	@echo


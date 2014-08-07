
.PHONY: help
help:
	@echo 'TinyRAM'
	@echo
	@echo 'targets:'
	@echo '  help               This message.'
	@echo '* trAsmProgs         TrAsm, TrDasm, and TrSim (requires Java 7)'
	@echo '* trVerLocal         Code to run program machinery without verification (experimentation only)'
	@echo '* trVer              Code to run program machinery inside verification framework'
	@echo '* doc                Build the documentation (requires LaTeX and pandoc)'
	@echo '  clean              Clean up everywhere'
	@echo
	@echo 'Options marked with * execute in the eponymous subdirectory'
	@echo
	@echo 'If you are new here, make doc and look at doc/htmlgen/README.html'
	@echo

SUBDIRS=trAsmProgs trVerLocal trVer constrSrc doc

.PHONY: $(SUBDIRS)

doc:
	@$(MAKE) -C $@

trAsmProgs:
	@$(MAKE) -C $@

trVerLocal: trAsmProgs
	@$(MAKE) -C $@

trVer: trAsmProgs
	@$(MAKE) -C $@

clean:
	@for i in $(SUBDIRS); do $(MAKE) -C $$i clean; done


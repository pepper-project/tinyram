
.SUFFIXES: .c .o .so

# don't delete intermediate files that result from these rules
.PRECIOUS: build build/TrVer build/ver_% build/%.jo %_out %_ver %_ver_so %.o %.so ver_% build/TrVer build build/%_ver.c build/zcc/%.ZAATAR.spec build/zcc/bin/%.qap bin/zcc build/%_pantry

SRCFILES:=$(shell find src/ver -name '*.c' -or -name '*.h')

# default, but you can override on the commandline
TR_NUMSTEPS?=6125
TR_INTAPELEN?=65
TR_AUXTAPELEN?=64

CFLAGS+=-Wall
CFLAGS+=-fPIC
ifeq ($(DEBUG),1)
	CFLAGS+=-ggdb3
endif
ifeq ($(VER_LOCAL),1)
	CFLAGS+=-DVER_LOCAL
endif

#CPPFLAGS+=-DTR_VEC_DIVOPS=1
#CPPFLAGS+=-DTR_VECSIZE=8
SOFLAGS=-shared
CC=gcc

RUN_LOCAL?=0
RUN_REMOTE?=0
RUN_LARGEMEM?=
RUN_TIME?="02:00:00"
RELEASE_NAME?=

ZCC_FOLDER=../constrSrc/
PEPPER_FOLDER=../../pepper/
SCRIPT_FOLDER=../../scripts/

ZCCPATH=../../compiler
ZCCORIG=$(ZCCPATH)/zcc
ZCCPATCH=lib/zcc.patch
ZCC_ECS?=--ecs

all: TR_NUMSTEPS=128
all: TR_INTAPELEN=1
all: TR_AUXTAPELEN=0
all: build/ver_fact

build/ver_%: SOFILES = $(shell find build -type f -name '$(notdir $<)_*.so')
build/ver_%: build/%_ver build build/TrVer build/%.jo build/%_out build/%_ver_so
	@echo "Running verification locally..."
	@bin/TrVer $(SOFILES)
	@echo Done.
	@echo

build/TrVer: build build/TrVer.c
	@echo "Building the dispatcher..."
	@$(CC) $(CFLAGS) $@.c -ldl -o $@
	@echo Done.
	@echo

build: $(SRCFILES)
	@echo -n "Setting up build... "
	@mkdir -p build
	@cp $(SRCFILES) build
	@touch build
	@echo done.
	@echo

build/%.jo: src/%.asm build
	@echo -n "Assembling $< with TrAsm... "
	@../trAsmProgs/bin/TrAsm -i -c -p -D $(TR_NUMSTEPS) $(TR_INTAPELEN) $(TR_AUXTAPELEN) -f 2 $< -o $(basename $@)
	@echo done.
	@echo

%_out: %.jo
	@echo "Running $< with TrSim..."
	@../trAsmProgs/bin/TrSim -c -n $(TR_NUMSTEPS) build $< > $@
	@cat $@
	@echo Done.
	@echo

%_ver: OUTFILES = $(shell find build -type f -name '$(notdir $<)_*.c')
%_ver: %_out
	@echo -n "Creating execution verifiers... "
	@for i in $(OUTFILES); do ./bin/mkVer.sh $$i ; done
	@touch $@
	@echo done.
	@echo

%_ver_so: CFILES = $(shell find build -type f -name '$(notdir $<)_*.c' -and -not -name '*cvars*')
%_ver_so: VERFILES = $(addsuffix .o,$(basename $(CFILES)))
%_ver_so: EXEVERFILES = $(addsuffix .so,$(basename $(CFILES)))
%_ver_so: %_ver $(VERFILES) $(EXEVERFILES)
	@echo "Compiling execution verifiers..."
	@if [ ! -z "$(EXEVERFILES)" ]; then $(MAKE) VER_LOCAL=1 $(addsuffix .so,$(basename $(CFILES))); else echo "failed to produce execution verifiers."; fi
	@touch $@
	@echo Done.
	@echo

.o.so:
	$(CC) $(LDFLAGS) $(LDLIBS) $(SOFLAGS) -Wl,-soname,$(notdir $@) -o $@ $<

##########
# PANTRY #
##########

build/%_pantry: PREFIX = $(patsubst build/zcc/bin/%_ver.qap,%,$<)
build/%_pantry: INPUTFILES = $(shell find build -maxdepth 1 -type f -name '$(PREFIX)_tape_*_input')
build/%_pantry: OLDPWD := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
build/%_pantry: build/zcc/bin/%.qap
	@echo "Running verified computation of $<"
	@cp bin/exo1 build/zcc/bin
	@cp ../lib/antlr-3.5.1-complete.jar build/zcc/bin
	@cp ../trAsmProgs/dist/trAsmProgs.jar build/zcc/bin
	@if [ -d src/$(PREFIX) ]; then \
		cp -R src/$(PREFIX) build; \
		$(MAKE) -C build/$(PREFIX) LDFLAGS=-static exo0; \
		cp build/$(PREFIX)/exo0 build/zcc/bin; \
	 fi
	@mkdir -p $(PEPPER_FOLDER)/bin
	@mkdir -p $(PEPPER_FOLDER)/apps_sfdl_gen
	@mkdir -p $(PEPPER_FOLDER)/apps_sfdl_hw
	@for i in $(INPUTFILES); do \
		./bin/inputGen.sh $(PREFIX)_ver build/$(PREFIX)_prog_input $$i build/zcc/apps_sfdl_gen/$(PREFIX)_ver_v_inp_gen.cpp; \
		./bin/exoGen.sh $(PREFIX)_ver $(TR_AUXTAPELEN) build/zcc/apps_sfdl_hw/$(PREFIX)_ver_p_exo.cpp; \
		cp build/zcc/bin/* $(PEPPER_FOLDER)/bin; \
		cp build/zcc/apps_sfdl_gen/* $(PEPPER_FOLDER)/apps_sfdl_gen; \
		cp build/zcc/apps_sfdl_hw/* $(PEPPER_FOLDER)/apps_sfdl_hw; \
		if [ x"$(RUN_LOCAL)" = x"1" ]; then \
			cd $(PEPPER_FOLDER); \
			./run/run_pepper.sh $(PREFIX)_ver; \
		elif [ x"$(RUN_REMOTE)" = x"1" ]; then \
			cd $(SCRIPT_FOLDER); \
			./run_mem_job.py $(PREFIX)_ver $(TR_NUMSTEPS) $(RUN_TIME) $(RUN_LARGEMEM); \
		fi; \
		cd $(OLDPWD) ;\
		done
	@echo Done.
	@echo

pantry_release:
	@echo "Assembling release tarball."
	@if [ x"$(RELEASE_NAME)" = x"" ]; then echo "ERROR: RELEASE_NAME not set"; exit 1; fi
	@cd build/zcc && tar czvpf ../../release/$(RELEASE_NAME).tgz apps_sfdl_gen apps_sfdl_hw bin
	@echo Done.
	@echo

build/%_ver.c: build/%.jo
	@echo "Creating Pantry verifier..."
	@./bin/mkPantry.sh $<
	@echo Done.
	@echo

bin/zcc: $(ZCCORIG) $(ZCCPATCH)
	@echo "Building patched zcc script..."
	@mkdir -p bin
	@cp $(ZCCORIG) bin
	@patch -p0 < $(ZCCPATCH)
	@echo Done.
	@echo

build/zcc/%.ZAATAR.spec: build/% bin/zcc
	@echo "Building ZAATAR execution verifier from $<..."
	@mkdir -p build/zcc
	@./bin/zcc --frontendonly --zccdir $(ZCCPATH) --rundir build/zcc -f $< -t ZAATAR -w 10240 --limcsize --metrics $(ZCC_ECS)
	@echo Done.
	@echo

build/zcc/bin/%.qap: build/zcc/%.c.ZAATAR.spec bin/zcc
	@echo "Creating QAP file from specfile $<..."
	@mkdir -p build/zcc
	@./bin/zcc --backendonly --zccdir $(ZCCPATH) --rundir build/zcc -f $< -t ZAATAR -w 10240 --limcsize --metrics $(ZCC_ECS)
	@echo Done.
	@echo

clean:
	@echo -n "Cleaning up... "
	@rm -rf build bin/zcc
	@$(MAKE) -C $(ZCCPATH)/frontend clean
	@echo done.
	@echo


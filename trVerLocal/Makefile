
.SUFFIXES: .c .o .so

# don't delete intermediate files that result from these rules
.PRECIOUS: build/%.jo %_out %_ver %_ver_so %.o %.so ver_% build/TrVer build

SRCFILES:=$(shell find src -name '*.c' -or -name '*.h')
TR_NUMSTEPS?=2645
TR_TAPELEN_MAX?=33
CFLAGS+=-Wall
CFLAGS+=-fPIC
TRSIM_NARG=
ifeq ($(DEBUG),1)
	CFLAGS+=-ggdb3
endif
ifneq ($(TR_NUMSTEPS),)
	CPPFLAGS+=-DTR_NUMSTEPS=$(TR_NUMSTEPS)
	TRSIM_NARG=-n $(TR_NUMSTEPS)
endif
ifneq ($(TR_TAPELEN_MAX),)
	CPPFLAGS+=-DTR_TAPELEN_MAX=$(TR_TAPELEN_MAX)
endif
#CPPFLAGS+=-DTR_VEC_DIVOPS=1
#CPPFLAGS+=-DTR_VECSIZE=8
SOFLAGS=-shared
CC=gcc

all: build/ver_fact
	@echo Done.

matmult: matmult.asm build/ver_matmult
	@echo Done.

mergesort: mergesort.asm build/ver_mergesort
	@echo Done.

mergesort.asm: build mergesort_v1.asm
	@echo "Creating mergesort.asm with random inputs. (MAT_FALSE value: $$MAT_FALSE)"
	@( cat mergesort_v1.asm ; bin/randmats.pl -n 5 -s 32 -l $(MAT_FALSE) ) > build/mergesort.asm
	@ln -sf build/mergesort.asm .
	@echo Done.
	@echo

matmult.asm: build matmult_v1.asm
	@echo "Creating matmult.asm with random inputs. (MAT_FALSE value: $$MAT_FALSE)"
	@( cat matmult_v1.asm ; bin/randmats.pl -n 5 -s 9 $(MAT_FALSE) ) > build/matmult.asm
	@ln -sf build/matmult.asm .
	@echo Done.
	@echo

build/ver_%: build/%_ver build build/TrVer build/%.jo build/%_out build/%_ver_so
	@echo "Running verification..."
	@$(eval SOFILES:=$(shell find build -type f -name '$(notdir $<)_*.so'))
	@bin/TrVer $(SOFILES)
	@echo Done.
	@echo

build/TrVer: build/TrVer.c build
	@echo "Building the dispatcher..."
	@$(CC) $(CFLAGS) $< -ldl -o $@
	@echo Done.
	@echo

build: $(SRCFILES)
	@echo -n "Setting up build... "
	@mkdir -p build
	@cp $(SRCFILES) build
	@touch build
	@echo done.
	@echo

build/%.jo: %.asm build
	@echo -n "Assembling $< with TrAsm... "
	@../trAsmProgs/bin/TrAsm -cP -p -f 2 $< -o $(basename $@)
	@echo done.
	@echo

%_out: %.jo
	@echo "Running $< with TrSim..."
	@../trAsmProgs/bin/TrSim -c $(TRSIM_NARG) build $< > $@
	@cat $@
	@echo Done.
	@echo

%_ver: %_out
	@echo -n "Creating execution verifiers... "
	@$(eval OUTFILES:=$(shell find build -type f -name '$(notdir $<)_*.c'))
	@for i in $(OUTFILES); do ./bin/mkVer.sh $$i ; done
	@touch $@
	@echo done.
	@echo

%_ver_so: %_ver
	@echo "Compiling execution verifiers..."
	@$(eval CFILES:=$(shell find build -type f -name '$(notdir $<)_*.c' -and -not -name '*cvars*'))
	@$(MAKE) $(addsuffix .so,$(basename $(CFILES)))
	@touch $@
	@echo Done.
	@echo

.o.so:
	$(CC) $(LDFLAGS) $(LDLIBS) $(SOFLAGS) -Wl,-soname,$(notdir $@) -o $@ $<

clean:
	@echo -n "Cleaning up... "
	@rm -rf build mergesort.asm matmult.asm
	@echo done.
	@echo


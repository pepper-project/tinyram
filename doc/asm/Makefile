WORK = $(shell basename "`pwd`")
TEXINPUTS = TEXINPUTS=.:${HOME}/tex//:

all: ${WORK}.dvi ${WORK}.ps ${WORK}.pdf

pdf: ${WORK}.pdf

bz2: ${WORK}.pdf.bz2

gz: ${WORK}.pdf.gz

%.ps: %.dvi
	dvips -f -Ppdf < $< > $@
	killall -q -HUP gv || echo "No gv running."

%.dvi: %.tex %.bib
	$(TEXINPUTS) latex $< < /dev/null || ( rm -f $@; false )
	$(TEXINPUTS) bibtex $(basename $<) < /dev/null || ( rm -f $@; false )
	$(TEXINPUTS) latex $< < /dev/null || ( rm -f $@; false )
	$(TEXINPUTS) latex $< < /dev/null || ( rm -f $@; false )

%.pdf: %.ps
	ps2pdf14 $< $@

%.pdf.bz2: %.pdf
	bzip2 -c < $< > $@

%.pdf.gz: %.pdf
	gzip -c < $< > $@

arch: ${WORK}.pdf
	cp $< ../arch

clean:
	rm -f *.aux *.log *.dvi *.ps *.pdf *.bbl *.toc *.lof *.lot *.blg *.out *.nav *.snm *.vrb

# $Header$

LATEX=latex
DVIPS=dvips
DVI2TTY=dvi2tty

SOURCES=FileFormat CodingStandards FAQ
DESTS=$(addsuffix .ps,$(SOURCES))
DESTSTXT=$(addsuffix .txt,$(SOURCES))

all: $(DESTS) $(DESTSTXT)

%.ps: %.dvi
	$(DVIPS) $< -o $@

# three passes should make all the references OK
%.dvi: %.tex
	$(LATEX) $(basename $<)
	$(LATEX) $(basename $<)
	$(LATEX) $(basename $<)

%.txt: %.dvi
	$(DVI2TTY) $< -o $@

clean:
	rm -f *.aux *.log *.toc *.blg *.bbl *.dvi *~

distclean: clean
	rm -f $(DESTS) $(DESTSTXT)

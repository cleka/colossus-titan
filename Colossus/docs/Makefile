# $Header$

MAKEDVI=../buildtools/makedvi.sh
DVIPS=dvips
DVI2TTY=dvi2tty

SOURCES=FileFormat CodingStandards FAQ build
DESTS=$(addsuffix .ps,$(SOURCES))
DESTSTXT=$(addsuffix .txt,$(SOURCES))

all: $(DESTS) $(DESTSTXT)

%.ps: %.dvi
	$(DVIPS) $< -o $@

# three passes should make all the references OK
%.dvi: %.tex
	$(MAKEDVI) $(basename $<)

%.txt: %.dvi
	$(DVI2TTY) $< -o $@

clean:
	rm -f *.aux *.log *.toc *.blg *.bbl *.dvi *~

distclean: clean
	rm -f $(DESTS) $(DESTSTXT)

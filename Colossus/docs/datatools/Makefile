# $Header$

MAKEDVI=../../buildtools/makedvi.sh
DVIPS=dvips
DVI2TTY=dvi2tty
LATEX2HTML=latex2html

SOURCES=BattlelandsBuilder BattlelandsRandomizer
DESTS=$(addsuffix .ps,$(SOURCES))
DESTSTXT=$(addsuffix .txt,$(SOURCES))
DESTSHTML=$(addsuffix .html,$(SOURCES))

all: $(DESTS) $(DESTSTXT)

clean:
	rm -f *.aux *.log *.toc *.blg *.bbl *.dvi *~

distclean: clean
	rm -f $(DESTS) $(DESTSTXT)

html: all $(DESTSHTML)


htmlclean: distclean
	rm -rf $(basename $(DESTSHTML))

## actual work below
%.ps: %.dvi
	$(DVIPS) $< -o $@

# three passes should make all the references OK
%.dvi: %.tex
	$(MAKEDVI) $(basename $<)

%.txt: %.dvi
	$(DVI2TTY) $< -o $@

%.html: %.tex
	$(LATEX2HTML) $<

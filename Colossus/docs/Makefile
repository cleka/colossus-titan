# $Header$

MAKEDVI=../buildtools/makedvi.sh
DVIPS=dvips
DVI2TTY=dvi2tty
LATEX2HTML=latex2html

SOURCES=Base FileFormat CodingStandards FAQ build Credits README Variant-HOWTO
DESTS=$(addsuffix .ps,$(SOURCES))
DESTSTXT=$(addsuffix .txt,$(SOURCES))
DESTSHTML=$(addsuffix .html,$(SOURCES))

all: $(DESTS) $(DESTSTXT) all-datatools

clean: clean-datatools
	rm -f *.aux *.log *.toc *.blg *.bbl *.dvi *~

distclean: clean distclean-datatools
	rm -f $(DESTS) $(DESTSTXT)

html: all $(DESTSHTML) html-datatools


htmlclean: distclean htmlclean-datatools
	rm -rf $(basename $(DESTSHTML))


# recursion in datatools
all-datatools:
	cd datatools && $(MAKE)

html-datatools:
	cd datatools && $(MAKE) html

clean-datatools:
	cd datatools && $(MAKE) clean

distclean-datatools:
	cd datatools && $(MAKE) distclean

htmlclean-datatools:
	cd datatools && $(MAKE) htmlclean

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

# $Header$

MAKEDVI=../../buildtools/makedvi.sh

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

include ../Makefile.rules

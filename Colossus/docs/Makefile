# $Header$

MAKEDVI=../buildtools/makedvi.sh

SOURCES=Base FileFormat CodingStandards FAQ build Credits README Variant-HOWTO
DESTS=$(addsuffix .ps,$(SOURCES))
DESTSTXT=$(addsuffix .txt,$(SOURCES))
DESTSHTML=$(addsuffix .html,$(SOURCES))
SUBDIR=datatools

all: $(DESTS) $(DESTSTXT) all-recursive

clean: clean-recursive
	rm -f *.aux *.log *.toc *.blg *.bbl *.dvi *~

distclean: clean distclean-recursive
	rm -f $(DESTS) $(DESTSTXT)

html: all $(DESTSHTML) html-recursive


htmlclean: distclean htmlclean-recursive
	rm -rf $(basename $(DESTSHTML))


# recursion in recursive
all-recursive html-recursive clean-recursive distclean-recursive htmlclean-recursive:
	target=`echo $@ | sed 's/-recursive//g'`
	for subdir in $(SUBDIR); do cd $$subdir && $(MAKE) $$target; done

include Makefile.rules

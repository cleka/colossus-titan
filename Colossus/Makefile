# Makefile for Colossus
# $Id$

# Make sure to save this file in Unix rather than DOS endline format, so that
#    it will continue to work portably.  GNU make has hard-to-diagnose 
#    problems with spurious ^M characters.

# Java compiler
# jikes is really fast, good, and free.  It supports -depend so all
#    dependent objects will be recompiled when you change one.
# javac is the standard; use it if you don't have jikes.  Sun has removed
#    the -depend/-Xdepend option from javac in JDK 1.3, so be careful.
# kjc comes with kaffe.  I like its --warning option.

#JCC = jikes -g -depend -deprecation +P
JCC = javac -J-Djavac.pipe.output=true -g -deprecation -target 1.2
#JCC = kjc -g --deprecation --warning


# Install directory for html and applet jar files.
HTMLDIR = /home/groups/c/co/colossus/htdocs
DOWNLOAD_DIR = $(HTMLDIR)/download
FTPDIR = /home/groups/ftp/pub/Colossus

ZIPFILE = Colossus.`date +%Y%m%d`.zip


# Targets

all: jar

.PHONY : all classes jar clean dist javadoc inst $(ZIPFILE)


# New implicit rule for Java
%.class : %.java
	$(JCC) $<


# Find target files

sources = $(wildcard *.java)

CLASS_FILES = $(sources:%.java=%.class)

classes : $(CLASS_FILES)

IMAGES = $(wildcard images/*.gif)

JARFILE = Colossus.jar

jar : $(JARFILE)

# Create jar file
$(JARFILE): $(CLASS_FILES) $(IMAGES) META-INF/MANIFEST.MF
	jar -mcf META-INF/MANIFEST.MF Colossus.jar *.class images/*.gif
	chmod +x Colossus.jar

# Install jar and html files for applet
INST_JARFILE = $(HTMLDIR)/$(JARFILE)
INST_HTML = $(HTMLDIR)/Colossus_applet_plug.html
INST_README = $(HTMLDIR)/README.txt
INST_DIST = $(FTPDIR)/$(ZIPFILE)
$(INST_JARFILE): $(JARFILE)
	cp Colossus.jar $(HTMLDIR)
$(INST_HTML): Colossus_applet_plug.html
	cp Colossus_applet_plug.html $(HTMLDIR)
$(INST_README): docs/README.txt
	cp docs/README.txt $(HTMLDIR)
$(INST_DIST): dist
	cp $(ZIPFILE) $(FTPDIR)
	cp $(ZIPFILE) $(DOWNLOAD_DIR)

inst: $(INST_JARFILE) $(INST_HTML) $(INST_README) $(INST_DIST)

# Clean up
clean:
	rm -f *~ *.class core javacore.txt *.bak *.jar

# Make zip file.  Don't include saved games, CVS dirs, or other zip files.
dist: $(JARFILE)
	zip -pr $(ZIPFILE) * -x *.sav -x *.zip -x \*CVS\* -x \*javadoc\*


# Make javadoc docs
javadoc:
	javadoc -d javadoc/ -version -author -private *.java


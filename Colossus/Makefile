# Makefile for Colossus
# $Id$

# Make sure to save this file in Unix rather than DOS endline format, so that
#    it will continue to work portably.  GNU make has hard-to-diagnose 
#    problems with spurious ^M characters.


# Java compiler
# jikes is really fast, good, and free.
# javac is the standard; use it if you don't have jikes.  If you have 
#    JDK 1.1 you'll need to use -depend instead of -Xdepend.
# kjc comes with kaffe.  I like its --warning option.

JCC = jikes -g -depend +P
#JCC = javac -J-Djavac.pipe.output=true -g -deprecation -Xdepend
#JCC = kjc -g --deprecation --warning


# Install directory for html and applet jar files.
HTMLDIR = /home/httpd/html

# Targets

all: jar

.PHONY : all classes jar clean dist


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
INST_HTML = $(HTMLDIR)/Colossus_applet.html
$(INST_JARFILE): $(JARFILE)
	cp Colossus.jar $(HTMLDIR)
$(INST_HTML): Colossus_applet.html
	cp Colossus_applet.html $(HTMLDIR)
inst: $(INST_JARFILE) $(INST_HTML)

# Clean up
clean:
	rm -f *~ *.class core javacore.txt *.bak *.jar

# Make zip file.  Don't include saved games or other zip files.
dist:
	zip -pr Colossus.`date +%Y%m%d`.zip * -x *.sav *.zip


# Make javadoc docs
javadoc:
	javadoc -d javadoc/ -version -author -private *.java


# $Id$

# Make sure to save this file in Unix rather than DOS endline format, so that
#    it will continue to work portably.  GNU make has hard-to-diagnose 
#    problems with spurious ^M characters.

# Java compiler
# jikes is really fast.  It supports -depend so all
#    dependent objects will be recompiled when you change one.  But
#    the jswat debugger has problems running its class files.  And jikes
#    has problems compiling some code that javac accepts.  If you have a
#    slow box you might want to use jikes during development cycles then
#    recompile with javac before check-in.
# javac is the standard; use it if you don't have jikes.  Sun has removed
#    the -depend/-Xdepend option from javac in JDK 1.3, so be careful to
#    manually recompile dependent classes when necessary.
# kjc comes with kaffe.  I like its --warning option.

#JCC = jikes -g -depend -deprecation +P
JCC = javac -J-Djavac.pipe.output=true -g -deprecation -target 1.2
#JCC = kjc -g --deprecation --warning

# JavaCC converts .jj parser description files to .java source
# JavaCC 2.1 is available from 
# http://www.webgain.com/trialware/javacc/JavaCC2_1.zip

JAVACC= javacc


HTMLDIR = /home/groups/c/co/colossus/htdocs
DOWNLOAD_DIR = $(HTMLDIR)/download

ZIPFILE = Colossus.`date +%Y%m%d`.zip


# Targets

all: jar

.PHONY : all classes jar clean dist javadoc inst $(ZIPFILE)


# New implicit rule for Java
%.class : %.java
	$(JCC) $<

%.java: %.jj
	$(JAVACC) -STATIC=false $<

# Find target files

sources = $(wildcard *.java) \
	StrategicMapLoader.java \
	TerrainRecruitLoader.java \
	CreatureLoader.java \
	BattlelandLoader.java \
	VariantLoader.java

CLASS_FILES = $(sources:%.java=%.class)

classes : $(CLASS_FILES)

IMAGES = $(wildcard images/*.gif)

JARFILE = Colossus.jar

jar : $(JARFILE)

# TODO Update this whenever anything else changes, and bundle
# it into the jar.
version :
	echo `date +%Y%m%d` > version


# Create jar file
$(JARFILE): StrategicMapLoader.java TerrainRecruitLoader.java \
	CreatureLoader.java BattlelandLoader.java \
	$(CLASS_FILES) $(IMAGES) META-INF/MANIFEST.MF StrategicMap.map
	jar -mcf META-INF/MANIFEST.MF Colossus.jar *.class images/*.gif StrategicMap.map Recruit.ter Creature.cre Default.var Battlelands
	chmod +x Colossus.jar

# Sign jar file.  This target uses dripton's key; other developers can
# make their own and add their own lines to the Makefile.
signjar : jar
	jarsigner -keystore driptonKeystore Colossus.jar dripton

INST_JARFILE = $(HTMLDIR)/$(JARFILE)
INST_README = $(HTMLDIR)/README.txt
INST_DIST = $(DOWNLOAD_DIR)/$(ZIPFILE)
$(INST_JARFILE): $(JARFILE)
	cp Colossus.jar $(HTMLDIR)
$(INST_README): docs/README.txt
	cp docs/README.txt $(HTMLDIR)
$(INST_DIST): dist
	cp $(ZIPFILE) $(DOWNLOAD_DIR)
$(INST_JNLP): dist
	cp Colossus.jnlp $(HTMLDIR)

inst: signjar $(INST_JARFILE) $(INST_HTML) $(INST_README) $(INST_DIST) $(INST_JNLP)

# Clean up
clean:
	rm -f *~ core javacore.txt *.bak *.jar *.class .#*.jar 

# Make zip file.  Don't include saved games, CVS dirs, or other zip files.
dist: $(JARFILE)
	zip -pr $(ZIPFILE) * -x *.sav -x *.zip -x \*CVS\* -x \*javadoc\* -x *Keystore*


# Make javadoc docs
javadoc:
	javadoc -d javadoc/ -version -author -private *.java


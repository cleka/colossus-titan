# $Id$

# If you have problems with make, use Ant instead.  See build.xml.

# Make sure to save this file in Unix rather than DOS endline format, so that
#    it will continue to work portably.  GNU make has hard-to-diagnose 
#    problems with spurious ^M characters.  And be certain that tabs do not 
#    get converted to spaces.

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

# Needs to be in the path.
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
	$(JAVACC) -STATIC=false -OUTPUT_DIRECTORY=net/sf/colossus/parser $<

# Find target files

sources = $(wildcard *.java) \
	$(wildcard net/sf/colossus/client/*.java) \
	$(wildcard net/sf/colossus/server/*.java) \
	$(wildcard net/sf/colossus/util/*.java) \
	$(wildcard net/sf/colossus/parser/*.java)

CLASS_FILES = $(sources:%.java=%.class)

classes : $(CLASS_FILES)

IMAGES = $(wildcard */images/*.gif)

JARFILE = Colossus.jar

jar : $(JARFILE)

version : classes
	echo `date +%Y%m%d` > version


# Create jar file
$(JARFILE): net/sf/colossus/parser/StrategicMapLoader.java \
	net/sf/colossus/parser/TerrainRecruitLoader.java \
	net/sf/colossus/parser/CreatureLoader.java \
	net/sf/colossus/parser/BattlelandLoader.java \
	net/sf/colossus/parser/VariantLoader.java \
	$(CLASS_FILES) $(IMAGES) META-INF/MANIFEST.MF version
	jar -mcf META-INF/MANIFEST.MF Colossus.jar \
        net/sf/colossus/client/*.class net/sf/colossus/server/*.class \
        net/sf/colossus/util/*.class net/sf/colossus/parser/*.class \
        Default TitanPlus ExtTitan Badlands Outlands Undead version
	chmod +x Colossus.jar

# Sign jar file.  You need a keystore file named {your user name}Keystore
#    containing a key called {your user name}
sign : jar
	jarsigner -keystore $(USER)Keystore Colossus.jar $(USER)

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

inst: signjar $(INST_JARFILE) $(INST_README) $(INST_DIST) $(INST_JNLP)

# Clean up
clean:
	rm -f version core javacore.txt *~ *.bak *.class *.jar .#*.jar \
	javadoc/ net/sf/colossus/*/*.class \
        net/sf/colossus/parser/*.java

# Make zip file.  Don't include saved games, CVS dirs, or other zip files.
dist: $(JARFILE)
	zip -pr $(ZIPFILE) * -x *.sav -x *.zip -x \*CVS\* -x \*javadoc\* \
            -x *Keystore* -x *cfg -x *properties* -x .*



# Make javadoc docs
javadoc:
	javadoc -d javadoc/ -version -author -private \
        net/sf/colossus/client/*.java net/sf/colossus/server/*.java \
        net/sf/colossus/util/*.java net/sf/colossus/parser/*.java \
	net/sf/colossus/datatools/*.java

# Make the Colossus-related tools
BBJAR=BattlelandsBuilder.jar

BRJAR=BattlelandsRandomizer.jar

BBSRC=net/sf/colossus/datatools/BattlelandsBuilder.java \
      net/sf/colossus/datatools/BuilderHexMap.java \
      net/sf/colossus/datatools/ShowBuilderHexMap.java \
      net/sf/colossus/client/Hex.java \
      net/sf/colossus/client/BattleHex.java \
      net/sf/colossus/client/GUIBattleHex.java \
      net/sf/colossus/util/HTMLColor.java \
      net/sf/colossus/parser/BattlelandLoader.java

BRSRC=net/sf/colossus/parser/BattlelandRandomizerLoader.java \
      net/sf/colossus/datatools/BattlelandsRandomizer.java \
      net/sf/colossus/datatools/BuilderHexMap.java \
      net/sf/colossus/client/Hex.java \
      net/sf/colossus/client/BattleHex.java \
      net/sf/colossus/client/GUIBattleHex.java \
      net/sf/colossus/util/HTMLColor.java \
      net/sf/colossus/parser/BattlelandLoader.java

BBCLASS=$(addsuffix .class,$(basename $(BBSRC)))

BRCLASS=$(addsuffix .class,$(basename $(BRSRC)))

BBMANIFEST=META-INF/datatools/BattlelandsBuilder/MANIFEST.MF

BRMANIFEST=META-INF/datatools/BattlelandsRandomizer/MANIFEST.MF

datatools: $(BBJAR) $(BRJAR)


$(BBJAR): net/sf/colossus/parser/BattlelandLoader.java \
          net/sf/colossus/parser/BattlelandRandomizerLoader.java \
          $(BBCLASS)
	jar -cmf $(BBMANIFEST) $@ net/sf/colossus/datatools/*.class \
                                  net/sf/colossus/client/*.class \
                                  net/sf/colossus/parser/*.class \
                                  net/sf/colossus/server/*.class \
                                  net/sf/colossus/util/*.class \
                                  Default
	chmod +x $@

$(BRJAR): net/sf/colossus/parser/BattlelandLoader.java \
          net/sf/colossus/parser/BattlelandRandomizerLoader.java \
	  $(BRCLASS)
	jar -cmf $(BRMANIFEST) $@ net/sf/colossus/datatools/*.class \
                                  net/sf/colossus/client/*.class \
                                  net/sf/colossus/parser/*.class \
                                  net/sf/colossus/server/*.class \
                                  net/sf/colossus/util/*.class
	chmod +x $@

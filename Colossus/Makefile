# Makefile for Colossus
# $Id$

# Make sure to save this file in Unix rather than DOS endline format, so that
#    it will continue to work portably.

# Compiler

JCC = jikes -g -depend +P
#JCC = javac -J-Djavac.pipe.output=true -g -deprecation -Xdepend


# Targets

all: classes

.PHONY : all classes jar clean dist


# New implicit rule for Java
%.class : %.java
	$(JCC) $<


# Find target files

sources = $(wildcard *.java)

classes: $(sources:%.java=%.class)



# Create jar file
jar: classes
	jar -mcf META-INF/MANIFEST.MF Colossus.jar *.class images/*.gif
	chmod +x Colossus.jar

# Install jar file
inst: classes
	cp Colossus.jar /home/httpd/html

# Clean up
clean:
	rm -f *~ *.class core *.bak *.jar

# Make zip file
dist:
	zip -pr Colossus.`date +%Y%m%d`.zip *


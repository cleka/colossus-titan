# Makefile for Colossus
# $Id$

# Make sure to save this file in Unix rather than DOS endline format, so that
#    it will continue to work portably.

# compiler

JCC = jikes -g -depend +P
#JCC = javac -J-Djavac.pipe.output=true -g -deprecation -Xdepend


# Targets

all: classes


# New rule for Java
.SUFFIXES:
.SUFFIXES: .java .class


# Compile command
.java.class:
	$(JCC) $<


# Find target files

FILES = $(wildcard *.java)
classes: $(FILES:.java=.class)

# Create jar file
jar: classes
	jar -mcf META-INF/MANIFEST.MF Colossus.jar *.class images/*.gif
	chmod +x Colossus.jar

# Clean up

clean:
	@@ echo 'rm -f *~ *.class core *.bak *.jar'
	@@rm -f *~ *.class core *.bak *.jar

# Makefile for Colossus
# dripton  June 26, 1998
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


# Clean up

clean:
	@@ echo 'rm -f *~ *.class core *.bak'
	@@rm -f *~ *.class core *.bak


# Simple Makefile to compile Assignment 1 source files.
# Adapted from file uploaded to Vula site CSC2001F(2019) by P Marais.

CC=javac

BINDIR=./bin
SRCDIR=./src
DOCDIR=./doc

PKG=flow

.SUFFIXES: .java .class

default: all

# General build rule: .java => .class
${BINDIR}/%.class: ${SRCDIR}/%.java
	javac $< -cp ${BINDIR} -d ${BINDIR}

# Build dependency rules
${BINDIR}/${PKG}.Terrain.class: ${SRCDIR}/${PKG}.Terrain.java
${BINDIR}/${PKG}.FlowPanel.class: ${SRCDIR}/${PKG}.FlowPanel.java ${BINDIR}/${PKG}.Terrain.class
${BINDIR}/${PKG}.Flow.class: ${SRCDIR}/${PKG}.Flow.java ${BINDIR}/${PKG}.FlowPanel.class ${BINDIR}/${PKG}.Terrain.class

all: clean clean-docs compile docs

compile:
	javac -d ${BINDIR} ${SRCDIR}/${PKG}/*.java

docs:
	javadoc  -private -d ${DOCDIR} -cp ${BINDIR}/${PKG} ${SRCDIR}/${PKG}/*.java

clean:
	rm -f ${BINDIR}/${PKG}/*.class

clean-docs:
	rm -rf ${DOCDIR}/*

run:
	./run.sh

.PHONY: default all run compile docs clean clean-docs

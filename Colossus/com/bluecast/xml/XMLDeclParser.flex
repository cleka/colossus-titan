package com.bluecast.xml;

import java.io.*;
import com.bluecast.io.*;

/*
 * $Id$
 *
 * (C) Copyright 2002 by Yuval Oren. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *   
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

/**
 * Parses an XML declaration (<?xml...?>). The parse() method returns
 * SUCCESS if a declaration was parsed, or NO_DECLARATION if none was found.
 */

/* This JFlex source file should be compiled with Yuval's "skeleton2" skeleton. */

%%

%char
%buffer 0
%class XMLDeclParser
%final

%unicode
%yylexthrow FileFormatException
%function parse
%int

ws = [ \t\r\n]+
opt_ws = [ \t\r\n]*
eq = {opt_ws}"="{opt_ws}

VersionNum = [a-zA-Z0-9_.:\-]+
EncName = [A-Za-z][A-Za-z0-9._\-]*

%{
    static public final int SUCCESS = 1;
    static public final int NO_DECLARATION = YYEOF;

    private String xmlVersion=null;
    private String xmlEncoding=null;
    private boolean xmlStandalone=false;
    private boolean xmlStandaloneDeclared=false;

    public XMLDeclParser(char[] buf,int off,int len) throws IOException {
        yyreset(buf,off,len);
    }

    public XMLDeclParser() { }

    public void reset(char[] buf, int off, int len) throws IOException {
        xmlVersion = xmlEncoding = null;
        xmlStandaloneDeclared = xmlStandalone = false;
        yyreset(buf,off,len);
    }

    public String getXMLVersion() { return xmlVersion; }
    public String getXMLEncoding() { return xmlEncoding; }
    public boolean isXMLStandaloneDeclared() { return xmlStandaloneDeclared; }
    public boolean isXMLStandalone() { return xmlStandalone; }
    public int getCharsRead() { return yychar+yylength(); }

%}

%state XML_DECL VERSION GOT_VERSION ENCODING GOT_ENCODING STANDALONE
%state GOT_STANDALONE


%%

<YYINITIAL> {
    "<?xml"/{ws} { yybegin(XML_DECL); }

    // Anything else at the top-level means there's no xml declaration
    .|\n { return NO_DECLARATION; }
}

<XML_DECL>{ws}"version"{eq} {
    yybegin(VERSION);
}

<VERSION> {
    "\"1.0\"" | "\'1.0\'" {
        xmlVersion = "1.0";
        yybegin(GOT_VERSION);
    }

    "\""{VersionNum}"\"" | "\'"{VersionNum}"\'" {
            xmlVersion = yytext(1,yylength()-2);
            yybegin(GOT_VERSION);
    }
}

// After the version comes either the encoding, standalone, or the end
<XML_DECL,GOT_VERSION> {
    {ws}"encoding"{eq} {
        yybegin(ENCODING);
    }

    // Standalone can appear after version or encoding
    <GOT_ENCODING> {
        {ws}"standalone"{eq} {
            xmlStandaloneDeclared = true;
            yybegin(STANDALONE);
        }

        // The declaration can end after version, encoding, or standalone
        <GOT_STANDALONE>{opt_ws}"?>" {
            return SUCCESS; // success
        }
    }
}

<ENCODING> {
    "\"UTF-8\"" |
    "\'UTF-8\'" |
    "\"UTF8\"" |
    "\'UTF8\'" {
        xmlEncoding = "UTF-8";
        yybegin(GOT_ENCODING);
    }

    "\"US-ASCII\"" |
    "\'US-ASCII\'" |
    "\"ASCII\"" |
    "\'ASCII\'" {
        xmlEncoding = "US-ASCII";
        yybegin(GOT_ENCODING);
    }

    "\"UTF-16\"" |
    "\'UTF-16\'" |
    "\"UTF16\"" |
    "\'UTF16\'" {
        xmlEncoding = "UTF-16";
        yybegin(GOT_ENCODING);
    }

    "\"ISO-8859-1\"" |
    "\'ISO-8859-1\'" {
        xmlEncoding = "ISO-8859-1";
        yybegin(GOT_ENCODING);
    }

    "\""{EncName}"\"" | "\'"{EncName}"\'" {
        xmlEncoding = yytext(1,yylength()-2);
        yybegin(GOT_ENCODING);
    }
}

<STANDALONE> {
    "\"yes\"" | "\'yes\'" { xmlStandalone = true; yybegin(GOT_STANDALONE); }
    "\"no\"" | "\'no\'"   { xmlStandalone = false; yybegin(GOT_STANDALONE); }
}

// Anything we didn't catch is an error
.|\n {
    throw new FileFormatException("XML Declaration not well-formed",
                                  -1,-1);
}





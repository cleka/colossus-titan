//### This file created by BYACC 1.8(/Java extension  1.1)
//### Java capabilities added 7 Jan 97, Bob Jamison
//### Updated : 27 Nov 97  -- Bob Jamison, Joe Nieten
//###           01 Jan 98  -- Bob Jamison -- fixed generic semantic constructor
//###           01 Jun 99  -- Bob Jamison -- added Runnable support
//###           06 Aug 00  -- Bob Jamison -- made state variables class-global
//###           03 Jan 01  -- Bob Jamison -- improved flags, tracing
//###           16 May 01  -- Bob Jamison -- added custom stack sizing
//###           04 Mar 02  -- Yuval Oren  -- improved java performance, added options
//### Please send bug reports to rjamison@lincom-asg.com
//### static char yysccsid[] = "@(#)yaccpar	1.8 (Berkeley) 01/20/90";






//#line 2 "Piccolo.y"
package com.bluecast.xml;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.xml.sax.ext.*;
import java.io.*;
import java.net.MalformedURLException;
import com.bluecast.util.*;
import java.util.*;

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
  * Piccolo is a small, high-performance SAX1 and SAX2 XML parser.
  * As per the SAX2 specification, namespace handling is on by
  * default. You can improve performance by turning it off.
  *
  * Note that if used in SAX1 mode, namespace handling is
  * automatically turned off.
  */
//#line 56 "Piccolo.java"




public class Piccolo
             implements org.xml.sax.Parser,org.xml.sax.Locator,org.xml.sax.XMLReader
{

boolean yydebug;        //do I want debug output?
int yynerrs;            //number of errors so far
int yyerrflag;          //was there an error?
int yychar;             //the current working character

//########## MESSAGES ##########
//###############################################################
// method: debug
//###############################################################
void debug(String msg)
{
  if (yydebug)
    System.out.println(msg);
}

//########## STATE STACK ##########
final static int YYSTACKSIZE = 500;  //maximum stack size
int statestk[] = new int[YYSTACKSIZE]; //state stack
int stateptr;
int stateptrmax;                     //highest index of stackptr
int statemax;                        //state when highest index reached
//###############################################################
// methods: state stack push,pop,drop,peek
//###############################################################
final void state_push(int state)
{
  try {
		stateptr++;
		statestk[stateptr]=state;
	 }
	 catch (ArrayIndexOutOfBoundsException e) {
     int oldsize = statestk.length;
     int newsize = oldsize * 2;
     int[] newstack = new int[newsize];
     System.arraycopy(statestk,0,newstack,0,oldsize);
     statestk = newstack;
     statestk[stateptr]=state;
  }
}
final int state_pop()
{
  try {
    return statestk[stateptr--];
  } catch (ArrayIndexOutOfBoundsException e) {
    return -1;
  }
}
final void state_drop(int cnt)
{
    stateptr -= cnt;
}
final int state_peek(int relative)
{
  try {
    return statestk[stateptr-relative];
  } catch (ArrayIndexOutOfBoundsException e) {
    return -1;
  }
}
//###############################################################
// method: init_stacks : allocate and prepare stacks
//###############################################################
final boolean init_stacks()
{
  stateptr = -1;
  val_init();
  return true;
}
//###############################################################
// method: dump_stacks : show n levels of the stacks
//###############################################################
void dump_stacks(int count)
{
int i;
  System.out.println("=index==state====value=     s:"+stateptr+"  v:"+valptr);
  for (i=0;i<count;i++)
    System.out.println(" "+i+"    "+statestk[i]+"      "+valstk[i]);
  System.out.println("======================");
}


//########## SEMANTIC VALUES ##########
//## **user defined:String
String   yytext;//user variable to return contextual strings
String yyval; //used to return semantic vals from action routines
String yylval;//the 'lval' (result) I got from yylex()
String valstk[] = new String[YYSTACKSIZE];
int valptr;
//###############################################################
// methods: value stack push,pop,drop,peek.
//###############################################################
final void val_init()
{
  yyval=new String();
  yylval=new String();
  valptr=-1;
}
final void val_push(String val)
{
  try {
    valptr++;
    valstk[valptr]=val;
  }
  catch (ArrayIndexOutOfBoundsException e) {
    int oldsize = valstk.length;
    int newsize = oldsize*2;
    String[] newstack = new String[newsize];
    System.arraycopy(valstk,0,newstack,0,oldsize);
    valstk = newstack;
    valstk[valptr]=val;
  }
}
final String val_pop()
{
  return valstk[valptr--];
}
final void val_drop(int cnt)
{
  valptr -= cnt;
}
final String val_peek(int relative)
{
  return valstk[valptr-relative];
}
//#### end semantic value section ####
public final static short CDATA=257;
public final static short TAG_END=258;
public final static short PI=259;
public final static short NAME=260;
public final static short STRING=261;
public final static short EQ=262;
public final static short OPEN_TAG=263;
public final static short CLOSE_TAG=264;
public final static short EMPTY_TAG=265;
public final static short WHITESPACE=266;
public final static short DTD_START=267;
public final static short DTD_START_SKIPEXTERNAL=268;
public final static short SYSTEM=269;
public final static short PUBLIC=270;
public final static short REQUIRED=271;
public final static short IMPLIED=272;
public final static short FIXED=273;
public final static short LPAREN=274;
public final static short RPAREN=275;
public final static short LBRACKET=276;
public final static short PIPE=277;
public final static short ENTITY_DECL_START=278;
public final static short ATTLIST_START=279;
public final static short NOTATION_START=280;
public final static short RBRACKET_END=281;
public final static short DOUBLE_RBRACKET_END=282;
public final static short PERCENT=283;
public final static short ENUMERATION=284;
public final static short NOTATION=285;
public final static short ID=286;
public final static short IDREF=287;
public final static short IDREFS=288;
public final static short ENTITY=289;
public final static short ENTITIES=290;
public final static short NMTOKEN=291;
public final static short NMTOKENS=292;
public final static short ENTITY_REF=293;
public final static short ENTITY_END=294;
public final static short INTERNAL_ENTITY_REF=295;
public final static short EXTERNAL_ENTITY_REF=296;
public final static short SKIPPED_ENTITY_REF=297;
public final static short PREFIXED_NAME=298;
public final static short UNPREFIXED_NAME=299;
public final static short NDATA=300;
public final static short COMMENT=301;
public final static short CONDITIONAL_START=302;
public final static short IGNORED_CONDITIONAL_START=303;
public final static short INCLUDE=304;
public final static short IGNORE=305;
public final static short MODIFIER=306;
public final static short PCDATA=307;
public final static short ELEMENT_DECL_START=308;
public final static short EMPTY=309;
public final static short ANY=310;
public final static short STAR=311;
public final static short COMMA=312;
public final static short QUESTION=313;
public final static short PLUS=314;
public final static short XML_DOC_DECL=315;
public final static short XML_TEXT_DECL=316;
public final static short XML_DOC_OR_TEXT_DECL=317;
public final static short YYERRCODE=256;
final static short yylhs[] = {                           -1,
    0,    0,    1,    1,    1,    5,    5,    3,    3,    3,
    4,    4,    7,    7,    7,    8,    8,    9,    9,    2,
    2,    2,    2,    2,   12,   12,   14,   14,   10,   10,
   10,   13,   13,   11,   11,   11,   11,   11,   11,   11,
   11,   11,   11,   15,   15,   20,   20,   21,   21,   22,
   22,   16,   16,   16,   16,   16,   16,   18,   18,   17,
   23,   24,   24,   25,   25,   25,   25,   25,   25,   25,
   25,   26,   26,   26,   26,   26,   26,   26,   26,   26,
   26,   27,   27,   19,   28,   28,   28,   28,   29,   29,
   29,   29,   30,   30,   30,   34,   34,   36,   36,   35,
   35,   35,   35,   31,   31,   33,   33,   32,   32,   32,
   32,    6,    6,    6,    6,    6,    6,    6,    6,    6,
    6,
};
final static short yylen[] = {                            2,
    4,    3,    1,    1,    0,    1,    1,    1,    3,    2,
    2,    0,    1,    1,    1,    1,    2,    0,    1,    3,
    4,    3,    6,    2,    2,    4,    7,    9,    3,    3,
    5,    3,    5,    0,    2,    2,    2,    2,    2,    2,
    3,    3,    4,    4,    4,    3,    2,    3,    2,    0,
    4,    7,    7,   11,    8,    8,   11,    7,    9,    4,
    3,    0,    3,    5,    5,    5,    5,    7,    7,    5,
    5,    1,    1,    1,    1,    1,    1,    1,    1,    5,
    7,    1,    5,    7,    1,    1,    1,    1,    6,    5,
   10,    2,    2,    2,    2,    5,    5,    1,    4,    3,
    3,    3,    2,    4,    2,    4,    2,    1,    1,    1,
    0,    0,    4,    4,    5,    4,    2,    2,    2,    2,
    2,
};
final static short yydefred[] = {                         0,
    3,    4,    0,    0,   14,  112,    8,   13,    0,    0,
   15,    0,    0,    0,   34,    0,    0,    0,    0,    0,
    0,    0,    2,    0,   24,   10,    0,    6,    7,   34,
    0,   25,   34,  121,  118,  112,    9,  117,  120,  112,
    0,  119,   16,   29,    0,   30,    0,    1,   11,    0,
    0,    0,   20,   34,    0,    0,    0,   39,   35,   36,
   37,   38,   40,   62,    0,   22,    0,    0,    0,  112,
    0,   17,    0,    0,    0,    0,    0,    0,    0,    0,
    0,   34,    0,    0,    0,    0,   34,   50,    0,    0,
   21,   26,  116,  113,    0,  114,    0,    0,    0,    0,
   31,    0,    0,    0,    0,   61,    0,    0,    0,    0,
   16,   47,   49,    0,    0,    0,    0,    0,  115,    0,
    0,   32,    0,   23,    0,    0,    0,   46,   48,   44,
   45,   50,    0,    0,    0,   63,   60,   27,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,   85,
   86,    0,   87,   88,    0,    0,    0,    0,    0,   33,
    0,    0,    0,    0,    0,    0,    0,    0,   51,    0,
   92,   95,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  109,  108,  110,   93,   94,   72,
    0,    0,   73,   74,   75,   76,   77,   78,   79,    0,
    0,   28,   52,    0,   53,    0,    0,    0,    0,   58,
    0,    0,    0,    0,  103,    0,    0,    0,    0,    0,
    0,   84,    0,    0,    0,    0,    0,   55,   56,    0,
    0,    0,  100,    0,  101,  102,  104,    0,    0,  106,
   82,    0,    0,   70,   64,   66,    0,   71,   65,   67,
    0,    0,    0,   59,    0,    0,    0,    0,   99,    0,
    0,    0,    0,    0,    0,   89,    0,   97,    0,    0,
   80,    0,    0,   68,   69,   54,   57,    0,    0,    0,
    0,    0,   83,   81,    0,   91,
};
final static short yydgoto[] = {                          3,
    4,   12,   26,   23,   30,   18,   58,  102,  103,   15,
   27,   16,   77,   17,   59,   60,   61,   62,   63,   87,
   88,  115,   64,   90,  136,  200,  242,  152,  171,  172,
  179,  188,  180,  181,  182,  183,
};
final static short yysindex[] = {                      -106,
    0,    0,    0, -100,    0,    0,    0,    0, -220, -195,
    0, -121, -196, -100,    0, -302, -155, -108, -253,  -63,
 -196, -121,    0, -196,    0,    0,  -11,    0,    0,    0,
   -7,    0,    0,    0,    0,    0,    0,    0,    0,    0,
 -302,    0,    0,    0,  202,    0,  214,    0,    0, -160,
 -160, -160,    0,    0, -302, -230, -160,    0,    0,    0,
    0,    0,    0,    0,   20,    0,   45,  103,  118,    0,
  128,    0, -160, -160, -160, -160,  -42, -248, -249,   17,
 -250,    0, -250, -160, -160, -186,    0,    0,   36, -160,
    0,    0,    0,    0,  143,    0,   44,   84,   98,  104,
    0, -116,  -56, -160,   -2,    0, -160, -250,  -58,  -29,
    0,    0,    0,   53, -181, -160, -104,    7,    0, -160,
 -160,    0, -160,    0,   73,  219,  221,    0,    0,    0,
    0,    0, -174, -160, -160,    0,    0,    0,  110,  129,
 -160, -160,  212, -160, -160, -160, -143, -176,  -88,    0,
    0, -160,    0,    0,  107,  107,  169,  169, -160,    0,
   34, -205,   62, -160, -160, -146,  131,   80,    0, -176,
    0,    0,  107,  107,  107,  -83,  -83,   56,  107,  107,
 -160, -160, -160,   93,    0,    0,    0,    0,    0,    0,
 -160, -160,    0,    0,    0,    0,    0,    0,    0, -160,
 -160,    0,    0, -160,    0,   94,  120, -160, -160,    0,
 -160,  -83,  107,  107,    0, -160, -160, -160,  113, -243,
  114,    0,  122, -157,  144,  204,   43,    0,    0,   47,
  129,  145,    0,  -48,    0,    0,    0, -160,  -83,    0,
    0, -160, -160,    0,    0,    0, -160,    0,    0,    0,
 -160, -160, -160,    0,  -55, -160,  -83, -160,    0,   31,
  122,  135,  185,  149,  178,    0,  122,    0, -160,   99,
    0, -160, -160,    0,    0,    0,    0, -160,  171,  193,
   52,  153,    0,    0,  156,    0,
};
final static short yyrindex[] = {                       -51,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,  466,    0,    0,   78,    0,    0,    0,    0,
  466,    0,    0,  466,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  168,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  -59,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  216,    0,    0,    0,
  -43,    0,  -18,  203,  203,    0,    0,    0,    0,  216,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0, -164,    0,    0,    0,    0,    0,   -1,    0,    0,
    0,    0,    0,    0,    0,    0,  228,    0,    0, -112,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  216,  216,    0,    0,    0,  216,    0,    8,  186,    0,
    0,  216,    0,    0, -125, -125,    0,    0, -112,    0,
    0,  228,    0,  216,  216,    0,    0,    0,    0,    0,
    0,    0,  -33,  -13, -213,  188,    0,    0, -213, -213,
  222, -269,  222,    0,    0,    0,    0,    0,    0,    0,
  236,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  216,    0,
 -188,    0, -198, -184,    0,  170, -188, -188,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  228,    0,    0,    0,    0,    0,    0,  -76,    0,    0,
    0,  170,  236,    0,    0,    0,    0,    0,    0,    0,
    0,  216,  216,    0,   -4,  236,    0, -270,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  217,    0,
    0,  236,  170,    0,    0,    0,    0,  170,    0,    0,
    0,    0,    0,    0,    0,    0,
};
final static short yygindex[] = {                         0,
    0,  484,  270,  254,  155,   91,  428,  -19,  -12,    0,
   -6,    0,   49,    0,    0,    0,    0,    0,    0,  413,
  414,  369,    0,    0,    0,  344,   69,    0,  370,  371,
 -129, -154, -126,  248, -173,  267,
};
final static int YYTABLESIZE=506;
final static short yytable[] = {                         45,
   47,  189,  215,  155,   98,   98,  156,   18,    5,   31,
  106,  104,   43,   28,   29,    8,   72,   72,  173,  189,
  211,  174,   44,   65,  217,  218,   67,   50,   51,   52,
   78,   79,   80,  238,  105,   43,   86,   89,  215,   19,
  173,   18,   18,  174,   54,   55,  213,   81,   83,  214,
   11,   56,  111,   97,   98,   99,  100,   57,  217,  218,
   72,  111,    5,  111,   20,  258,   86,  105,  239,    8,
  117,  109,  110,   84,   85,  108,  105,  118,  105,  111,
  114,  107,  213,  269,  125,  214,   18,  127,   18,  170,
  107,  148,  107,   19,  204,   19,  133,  149,  111,  149,
  131,  139,   32,  140,   11,   43,  143,  138,   72,   19,
   19,   19,   19,  105,  157,  158,  243,   84,   85,   72,
   33,  132,  162,   18,  166,  167,   68,  107,  161,  163,
   69,   71,  111,  168,  150,  151,  178,    5,  169,  184,
  111,    6,   19,    7,    8,   18,  202,   19,   34,   72,
   35,  206,  207,  208,   36,   37,   38,   39,    5,  132,
   95,   72,    6,   18,    7,    8,    9,   10,  219,  220,
  221,  175,  224,  142,  144,  146,  175,  176,  223,   11,
  225,  226,  212,   18,  227,  177,   40,   41,  230,  231,
  177,  165,   42,  134,  135,   70,  232,   18,  233,   34,
   11,  124,   43,  234,  235,  236,   34,    5,    1,   82,
    2,    5,   46,    5,    5,    5,    5,  128,   34,   34,
   34,   34,   34,   43,  105,  257,  255,  262,  256,  260,
  261,  263,  105,  101,   34,   34,   34,   41,   41,  264,
  265,   34,   34,  267,  107,  270,  129,    5,   34,    5,
   41,    5,  107,   90,    8,  266,  279,  126,    8,  280,
  281,   90,   42,   42,  137,  282,   50,   51,   52,   53,
   50,   51,   52,   13,   48,   42,  107,   49,    5,   43,
   43,   21,   72,   54,   55,    8,   66,   54,   55,   11,
   56,  203,   43,   11,   56,  116,   57,   50,   51,   52,
   57,   72,  252,    5,  120,  271,  253,  272,   72,   72,
    8,    5,   72,   91,   54,   55,   17,   17,    8,  205,
   11,   56,   50,   51,   52,   92,  284,   57,  272,  273,
   50,   51,   52,  141,  130,  278,   34,  210,   72,   54,
   55,   75,   76,   34,  121,   11,   56,   54,   55,   72,
  222,  228,   57,   11,   56,   34,   34,   34,  122,   34,
   57,   35,  216,   72,  123,   36,   93,   38,   39,   72,
  159,   34,   34,   34,   34,   72,   35,  229,   34,   34,
   36,  241,   38,   39,   34,   34,   35,  237,  240,  160,
   36,  209,   38,   39,   72,  274,   72,   40,   41,   34,
   72,   35,  254,   42,  244,   36,  276,   38,   39,   72,
  239,   94,   40,   41,  245,  246,  247,  185,   42,  186,
  187,   96,   40,   41,  112,  190,  112,  285,   42,  272,
  112,   14,  112,  112,   72,  277,  119,   40,   41,   22,
   24,   14,  191,   42,   18,  275,   18,  238,   24,   22,
   72,   24,  283,  192,  193,  194,  195,  196,  197,  198,
  199,  112,  112,  112,  248,   12,  286,   72,  112,   72,
   73,   74,  164,   18,  249,  250,  251,   72,   18,   72,
   75,   76,   75,   76,   43,   19,   72,   75,   76,   75,
  145,   96,   18,   18,   16,   18,   18,   25,  112,  113,
  147,  201,  153,  154,  268,  259,
};
final static short yycheck[] = {                         19,
   20,  156,  176,  133,  275,  275,  133,  277,  259,   16,
  260,  260,  266,  316,  317,  266,  266,  266,  148,  174,
  175,  148,  276,   30,  179,  180,   33,  278,  279,  280,
   50,   51,   52,  277,  283,  266,   56,   57,  212,  260,
  170,  312,  312,  170,  295,  296,  176,   54,   55,  176,
  301,  302,  266,   73,   74,   75,   76,  308,  213,  214,
  266,  275,  259,  277,  260,  239,   86,  266,  312,  266,
   90,   84,   85,  304,  305,   82,  275,   90,  277,  266,
   87,  266,  212,  257,  104,  212,  275,  107,  277,  266,
  275,  266,  277,  258,  300,  260,  116,  274,  312,  274,
  282,  121,  258,  123,  301,  266,  126,  120,  266,  274,
  275,  276,  277,  312,  134,  135,  274,  304,  305,  266,
  276,  303,  142,  312,  144,  145,   36,  312,  141,  142,
   40,   41,  258,  146,  309,  310,  149,  259,  282,  152,
  266,  263,  307,  265,  266,  258,  159,  312,  257,  266,
  259,  164,  165,  300,  263,  264,  265,  266,  259,  303,
   70,  266,  263,  276,  265,  266,  267,  268,  181,  182,
  183,  260,  192,  125,  126,  127,  260,  266,  191,  301,
  200,  201,  266,  260,  204,  274,  295,  296,  208,  209,
  274,  143,  301,  298,  299,   41,  209,  274,  211,  259,
  301,  258,  266,  216,  217,  218,  266,  259,  315,   55,
  317,  263,  276,  265,  266,  267,  268,  276,  278,  279,
  280,  281,  282,  266,  258,  238,  275,  247,  277,  242,
  243,  251,  266,  276,  294,  295,  296,  281,  282,  252,
  253,  301,  302,  256,  258,  258,  276,  259,  308,  301,
  294,  259,  266,  258,  266,  311,  269,  260,  266,  272,
  273,  266,  281,  282,  258,  278,  278,  279,  280,  281,
  278,  279,  280,    4,   21,  294,  260,   24,  259,  281,
  282,   12,  266,  295,  296,  266,  294,  295,  296,  301,
  302,  258,  294,  301,  302,  260,  308,  278,  279,  280,
  308,  266,  260,  259,  261,  275,  260,  277,  266,  266,
  266,  259,  266,  294,  295,  296,  309,  310,  266,  258,
  301,  302,  278,  279,  280,  281,  275,  308,  277,  261,
  278,  279,  280,  261,  282,  267,  259,  258,  266,  295,
  296,  269,  270,  266,  261,  301,  302,  295,  296,  266,
  258,  258,  308,  301,  302,  278,  279,  280,  261,  257,
  308,  259,  307,  266,  261,  263,  264,  265,  266,  266,
  261,  294,  295,  296,  257,  266,  259,  258,  301,  302,
  263,  260,  265,  266,  257,  308,  259,  275,  275,  261,
  263,  261,  265,  266,  266,  261,  266,  295,  296,  257,
  266,  259,  258,  301,  261,  263,  258,  265,  266,  266,
  312,  294,  295,  296,  271,  272,  273,  311,  301,  313,
  314,  294,  295,  296,  257,  257,  259,  275,  301,  277,
  263,    4,  265,  266,  266,  258,  294,  295,  296,   12,
   13,   14,  274,  301,  275,  261,  277,  277,   21,   22,
  266,   24,  260,  285,  286,  287,  288,  289,  290,  291,
  292,  294,  295,  296,  261,    0,  311,  266,  301,  266,
  269,  270,  261,  258,  271,  272,  273,  266,  276,  266,
  269,  270,  269,  270,  266,  258,  266,  269,  270,  269,
  270,  275,  307,  277,  307,  260,  275,   14,   86,   86,
  132,  158,  133,  133,  257,  239,
};
final static short YYFINAL=3;
final static short YYMAXTOKEN=317;
final static String yyname[] = {
"end-of-file",null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,"CDATA","TAG_END","PI","NAME","STRING","EQ","OPEN_TAG",
"CLOSE_TAG","EMPTY_TAG","WHITESPACE","DTD_START","DTD_START_SKIPEXTERNAL",
"SYSTEM","PUBLIC","REQUIRED","IMPLIED","FIXED","LPAREN","RPAREN","LBRACKET",
"PIPE","ENTITY_DECL_START","ATTLIST_START","NOTATION_START","RBRACKET_END",
"DOUBLE_RBRACKET_END","PERCENT","ENUMERATION","NOTATION","ID","IDREF","IDREFS",
"ENTITY","ENTITIES","NMTOKEN","NMTOKENS","ENTITY_REF","ENTITY_END",
"INTERNAL_ENTITY_REF","EXTERNAL_ENTITY_REF","SKIPPED_ENTITY_REF",
"PREFIXED_NAME","UNPREFIXED_NAME","NDATA","COMMENT","CONDITIONAL_START",
"IGNORED_CONDITIONAL_START","INCLUDE","IGNORE","MODIFIER","PCDATA",
"ELEMENT_DECL_START","EMPTY","ANY","STAR","COMMA","QUESTION","PLUS",
"XML_DOC_DECL","XML_TEXT_DECL","XML_DOC_OR_TEXT_DECL",
};
final static String yyrule[] = {
"$accept : document",
"document : xml_decl dtd body epilog",
"document : xml_decl body epilog",
"xml_decl : XML_DOC_DECL",
"xml_decl : XML_DOC_OR_TEXT_DECL",
"xml_decl :",
"xml_text_decl : XML_TEXT_DECL",
"xml_text_decl : XML_DOC_OR_TEXT_DECL",
"body : EMPTY_TAG",
"body : OPEN_TAG content CLOSE_TAG",
"body : misc body",
"epilog : misc epilog",
"epilog :",
"misc : WHITESPACE",
"misc : PI",
"misc : COMMENT",
"ws : WHITESPACE",
"ws : ws WHITESPACE",
"opt_ws :",
"opt_ws : ws",
"dtd : dtd_only_internal_start dtd_content RBRACKET_END",
"dtd : dtd_with_external xml_text_decl dtd_content ENTITY_END",
"dtd : dtd_with_external dtd_content ENTITY_END",
"dtd : DTD_START_SKIPEXTERNAL NAME ws external_id opt_ws TAG_END",
"dtd : misc dtd",
"dtd_with_external : dtd_with_external_start TAG_END",
"dtd_with_external : dtd_with_external_start LBRACKET dtd_content RBRACKET_END",
"dtd_with_external_start : DTD_START NAME ws SYSTEM ws STRING opt_ws",
"dtd_with_external_start : DTD_START NAME ws PUBLIC ws STRING ws STRING opt_ws",
"dtd_only_internal_start : DTD_START NAME LBRACKET",
"dtd_only_internal_start : DTD_START_SKIPEXTERNAL NAME LBRACKET",
"dtd_only_internal_start : DTD_START_SKIPEXTERNAL NAME ws external_id LBRACKET",
"external_id : SYSTEM ws STRING",
"external_id : PUBLIC ws STRING ws STRING",
"dtd_content :",
"dtd_content : dtd_content dtd_conditional",
"dtd_content : dtd_content dtd_entity",
"dtd_content : dtd_content dtd_attlist",
"dtd_content : dtd_content dtd_notation",
"dtd_content : dtd_content misc",
"dtd_content : dtd_content dtd_element",
"dtd_content : dtd_content INTERNAL_ENTITY_REF dtd_content",
"dtd_content : dtd_content EXTERNAL_ENTITY_REF dtd_content",
"dtd_content : dtd_content EXTERNAL_ENTITY_REF xml_text_decl dtd_content",
"dtd_conditional : CONDITIONAL_START dtd_include dtd_content DOUBLE_RBRACKET_END",
"dtd_conditional : CONDITIONAL_START dtd_ignore ignored_dtd_content DOUBLE_RBRACKET_END",
"dtd_include : INCLUDE opt_ws LBRACKET",
"dtd_include : ws dtd_include",
"dtd_ignore : IGNORE opt_ws LBRACKET",
"dtd_ignore : ws dtd_ignore",
"ignored_dtd_content :",
"ignored_dtd_content : ignored_dtd_content IGNORED_CONDITIONAL_START ignored_dtd_content DOUBLE_RBRACKET_END",
"dtd_entity : ENTITY_DECL_START ws NAME ws STRING opt_ws TAG_END",
"dtd_entity : ENTITY_DECL_START ws NAME ws external_id opt_ws TAG_END",
"dtd_entity : ENTITY_DECL_START ws NAME ws external_id ws NDATA ws NAME opt_ws TAG_END",
"dtd_entity : ENTITY_DECL_START ws PERCENT NAME ws STRING opt_ws TAG_END",
"dtd_entity : ENTITY_DECL_START ws PERCENT NAME ws external_id opt_ws TAG_END",
"dtd_entity : ENTITY_DECL_START ws PERCENT NAME external_id ws NDATA ws NAME opt_ws TAG_END",
"dtd_notation : NOTATION_START ws NAME ws external_id opt_ws TAG_END",
"dtd_notation : NOTATION_START ws NAME ws PUBLIC ws STRING opt_ws TAG_END",
"dtd_attlist : attlist_start att_def_list opt_ws TAG_END",
"attlist_start : ATTLIST_START ws NAME",
"att_def_list :",
"att_def_list : att_def_list ws att_def",
"att_def : PREFIXED_NAME ws att_type ws REQUIRED",
"att_def : UNPREFIXED_NAME ws att_type ws REQUIRED",
"att_def : PREFIXED_NAME ws att_type ws IMPLIED",
"att_def : UNPREFIXED_NAME ws att_type ws IMPLIED",
"att_def : PREFIXED_NAME ws att_type ws FIXED ws STRING",
"att_def : UNPREFIXED_NAME ws att_type ws FIXED ws STRING",
"att_def : PREFIXED_NAME ws att_type ws STRING",
"att_def : UNPREFIXED_NAME ws att_type ws STRING",
"att_type : CDATA",
"att_type : ID",
"att_type : IDREF",
"att_type : IDREFS",
"att_type : ENTITY",
"att_type : ENTITIES",
"att_type : NMTOKEN",
"att_type : NMTOKENS",
"att_type : LPAREN opt_ws word_list opt_ws RPAREN",
"att_type : NOTATION ws LPAREN opt_ws word_list opt_ws RPAREN",
"word_list : NAME",
"word_list : word_list opt_ws PIPE opt_ws NAME",
"dtd_element : ELEMENT_DECL_START ws NAME ws element_spec opt_ws TAG_END",
"element_spec : EMPTY",
"element_spec : ANY",
"element_spec : element_spec_mixed",
"element_spec : element_spec_children",
"element_spec_mixed : LPAREN opt_ws PCDATA opt_ws RPAREN STAR",
"element_spec_mixed : LPAREN opt_ws PCDATA opt_ws RPAREN",
"element_spec_mixed : LPAREN opt_ws PCDATA opt_ws PIPE opt_ws word_list opt_ws RPAREN STAR",
"element_spec_mixed : WHITESPACE element_spec_mixed",
"element_spec_children : element_choice element_modifier",
"element_spec_children : element_seq element_modifier",
"element_spec_children : WHITESPACE element_spec_children",
"element_cp_pipe_list : element_cp opt_ws PIPE opt_ws element_cp",
"element_cp_pipe_list : element_cp opt_ws PIPE opt_ws element_cp_pipe_list",
"element_cp_comma_list : element_cp",
"element_cp_comma_list : element_cp opt_ws COMMA element_cp_comma_list",
"element_cp : NAME element_modifier opt_ws",
"element_cp : element_choice element_modifier opt_ws",
"element_cp : element_seq element_modifier opt_ws",
"element_cp : WHITESPACE element_cp",
"element_choice : LPAREN element_cp_pipe_list opt_ws RPAREN",
"element_choice : WHITESPACE element_choice",
"element_seq : LPAREN element_cp_comma_list opt_ws RPAREN",
"element_seq : WHITESPACE element_seq",
"element_modifier : QUESTION",
"element_modifier : STAR",
"element_modifier : PLUS",
"element_modifier :",
"content :",
"content : content INTERNAL_ENTITY_REF content ENTITY_END",
"content : content EXTERNAL_ENTITY_REF content ENTITY_END",
"content : content EXTERNAL_ENTITY_REF xml_text_decl content ENTITY_END",
"content : content OPEN_TAG content CLOSE_TAG",
"content : content EMPTY_TAG",
"content : content PI",
"content : content COMMENT",
"content : content WHITESPACE",
"content : content CDATA",
};

//#line 548 "Piccolo.y"

DocumentHandler documentHandler = null;
DTDHandler dtdHandler = null;
ErrorHandler errorHandler = null;
ContentHandler contentHandler = null;
int saxVersion = 0;
int attributeType=-1;
StringBuffer modelBuffer = new StringBuffer(100);
ElementDefinition elementDefinition=null;
String pubID=null,sysID=null;
String dtdName=null,dtdPubID=null,dtdSysID=null;
PiccoloLexer lexer = new PiccoloLexer(this);
DocumentEntity docEntity = new DocumentEntity();
LexicalHandler lexHandler = null;
DeclHandler declHandler = null;


/// Create an instance of the Piccolo parser
public Piccolo() { }

/**
 * Create an instance with the same configuration
 * as the given instance. ContentHandler, DTDHandler, etc.
 * will not be copied.
 */
public Piccolo(Piccolo template) {
    fNamespaces = template.fNamespaces;
    fNamespacePrefixes = template.fNamespacePrefixes;
    fExternalGeneralEntities = template.fExternalGeneralEntities;
    fExternalParameterEntities = template.fExternalParameterEntities;
    fLexicalParameterEntities = template.fLexicalParameterEntities;
    lexer.enableNamespaces(fNamespaces);
    fResolveDTDURIs = template.fResolveDTDURIs;
}

private void reset() {
    modelBuffer.setLength(0);
}

public void setDebug(boolean debug) {
    yydebug = debug;
}


/************************************************************************
 * Methods common to both SAX1 and SAX2
 ************************************************************************/

public void parse(InputSource source) throws IOException, SAXException {
    try {
        reset();
        docEntity.reset(source);
        lexer.reset(docEntity);
        reportStartDocument();
        yyparse();
    }
    catch (FatalParsingException e) {
        reportFatalError(e.getMessage(),e.getException());
    }
    finally {
        reportEndDocument();
    }
}



public void parse(String sysID) throws IOException, SAXException {
    try {
        reset();
        docEntity.reset(sysID);
        lexer.reset(docEntity);
        reportStartDocument();
        yyparse();
    }
    catch (FatalParsingException e) {
        reportFatalError(e.getMessage(),e.getException());
    }
    finally {
        reportEndDocument();
    }
}


/************************************************************************
 * SAX1 methods
 ************************************************************************/

public void setDocumentHandler(DocumentHandler handler) {
    documentHandler = handler;
    if (documentHandler != null) {
      saxVersion = 1;
      fNamespaces = false;
      lexer.enableNamespaces(false);
      fNamespacePrefixes = true;
      documentHandler.setDocumentLocator(this);
    }
    else
      saxVersion = 0;
}


public void setDTDHandler(DTDHandler handler) {
    dtdHandler = handler;
}

public void setEntityResolver(EntityResolver resolver) {
    lexer.entityManager.setResolver(resolver);
}

public void setErrorHandler(ErrorHandler handler) {
    errorHandler = handler;
}

public void setLocale(java.util.Locale locale) 
throws SAXException {
    if (!("en".equals(locale.getLanguage())))
        throw new SAXException("Only English (EN) locales are supported");
}

// Locator
public int getColumnNumber() { return lexer.getColumnNumber(); }
public int getLineNumber() { return lexer.getLineNumber(); }
public String getPublicId() { return lexer.getPublicID(); }
public String getSystemId() { return lexer.getSystemID(); }



/************************************************************************
 * SAX2 methods
 ************************************************************************/

public ContentHandler getContentHandler() { return contentHandler; }

public void setContentHandler(ContentHandler handler) {
    contentHandler = handler;

    if (contentHandler != null) {
      // Are we switching from SAX1? If so, turn namespace processing on
      if (saxVersion == 1) {
          fNamespaces = true;
          lexer.enableNamespaces(true);
          fNamespacePrefixes = false;
      }

      saxVersion = 2;
      contentHandler.setDocumentLocator(this);
    }
    else
        saxVersion = 0;
}

public DTDHandler getDTDHandler() { return dtdHandler; }
public EntityResolver getEntityResolver() { return lexer.entityManager.getResolver(); }
public ErrorHandler getErrorHandler() { return errorHandler; }


// SAX2 Features
boolean fNamespaces=true,fNamespacePrefixes=false,fResolveDTDURIs=true;
boolean fExternalGeneralEntities=true,fExternalParameterEntities=true;
boolean fLexicalParameterEntities = true;

public boolean getFeature(String name)
    throws SAXNotSupportedException,SAXNotRecognizedException {
    if (name.equals("http://xml.org/sax/features/namespaces"))
        return fNamespaces;
    else if (name.equals("http://xml.org/sax/features/namespace-prefixes"))
        return fNamespacePrefixes;
    else if (name.equals("http://xml.org/sax/features/external-general-entities"))
        return fExternalGeneralEntities;
    else if (name.equals("http://xml.org/sax/features/external-parameter-entities"))
        return fExternalGeneralEntities;
    else if (name.equals("http://xml.org/sax/features/lexical-handler/parameter-entities"))
        return fLexicalParameterEntities;
    else if (name.equals("http://xml.org/sax/features/string-interning"))
        return true;
    else if (name.equals("http://xml.org/sax/features/is-standalone"))
        return docEntity.isStandalone();
    else if (name.equals("http://xml.org/sax/features/resolve-dtd-uris"))
        return fResolveDTDURIs;
    else if (name.equals("http://xml.org/sax/features/use-attributes2")
          || name.equals("http://xml.org/sax/features/validation")
          || name.equals("http://xml.org/sax/features/use-locator2")
          || name.equals("http://xml.org/sax/features/use-entity2")
          || name.equals("http://xml.org/sax/features/use-locator2"))
          return false;
    else
        throw new SAXNotRecognizedException(name);
}

public void setFeature(String name, boolean value)
throws SAXNotSupportedException,SAXNotRecognizedException {
    if (name.equals("http://xml.org/sax/features/namespaces")) {
        fNamespaces = value;
        lexer.enableNamespaces(value);
    }
    else if (name.equals("http://xml.org/sax/features/namespace-prefixes")) {
        fNamespacePrefixes = value;
    }
    else if (name.equals("http://xml.org/sax/features/external-general-entities")) {
        fExternalGeneralEntities = value;
    }
    else if (name.equals("http://xml.org/sax/features/external-parameter-entities")) {
        fExternalParameterEntities = value;
    }
    else if (name.equals("http://xml.org/sax/features/lexical-handler/parameter-entities")) {
        fLexicalParameterEntities = value;
    }
    else if (name.equals("http://xml.org/sax/features/resolve-dtd-uris")) {
        fResolveDTDURIs = value;
    }
    else if (name.equals("http://xml.org/sax/features/validation")) {
        if (value)
            throw new SAXNotSupportedException("validation is not supported");
    }
    else if (name.equals("http://xml.org/sax/features/string-interning")) {
        if (!value)
            throw new SAXNotSupportedException("strings are always internalized");
    }
    else if (name.equals("http://xml.org/sax/features/use-attributes2")
          || name.equals("http://xml.org/sax/features/validation")
          || name.equals("http://xml.org/sax/features/use-locator2")
          || name.equals("http://xml.org/sax/features/use-entity2")
          || name.equals("http://xml.org/sax/features/use-locator2")) {
        if (value)
            throw new SAXNotSupportedException(name);
    }
    else
        throw new SAXNotRecognizedException(name);
}

public Object getProperty(String name)
throws SAXNotRecognizedException, SAXNotSupportedException {
    if (name.equals("http://xml.org/sax/properties/declaration-handler"))
        return declHandler;
    else
    if (name.equals("http://xml.org/sax/properties/lexical-handler"))
        return lexHandler;
    else
        throw new SAXNotRecognizedException(name);
}

public void setProperty(String name,Object value)
throws SAXNotRecognizedException, SAXNotSupportedException {
    if (name.equals("http://xml.org/sax/properties/declaration-handler")) {
        try {
            declHandler = (DeclHandler) value;
        }
        catch (ClassCastException e) {
            throw new SAXNotSupportedException("property value is not a DeclHandler");
        }
    }
    else
    if (name.equals("http://xml.org/sax/properties/lexical-handler")) {
        try {
            lexHandler = (LexicalHandler) value;
        }
        catch (ClassCastException e) {
            throw new SAXNotSupportedException("property value is not a LexicalHandler");
        }
    }
    else
        throw new SAXNotRecognizedException(name);
}


/************************************************************************
 * Methods used to call ContentHandlers or DocumentHandlers
 ************************************************************************/

void reportCdata() throws SAXException {
    reportCdata(lexer.cdataBuffer,lexer.cdataStart,lexer.cdataLength);
}

private char[] oneCharBuffer = new char[1];
void reportCdata(char c) throws SAXException {
    oneCharBuffer[0] = c;
    reportCdata(oneCharBuffer,0,1);
}

void reportCdata(char[] buf, int off, int len) throws SAXException {
    switch (saxVersion) {
        case 2:
            contentHandler.characters(buf,off,len);
            break;
        case 1:
            documentHandler.characters(buf,off,len);
            break;
    }
}


void reportWhitespace() throws SAXException {
    reportWhitespace(lexer.cdataBuffer,lexer.cdataStart,lexer.cdataLength);
}

void reportWhitespace(char[] buf, int off, int len)  throws SAXException {
    switch (saxVersion) {
      case 2:
        contentHandler.characters(buf,off,len);
        break;
      case 1:
        documentHandler.characters(buf,off,len);
        break;
    }
}


void reportError(String msg)  throws SAXException {
    if (errorHandler != null) {
      errorHandler.error(new SAXParseException(msg,getPublicId(),getSystemId(),getLineNumber(),getColumnNumber()));
    }
}


void reportFatalError(String msg)  throws SAXException {
  reportFatalError(msg,null);
}

void reportFatalError(String msg, Exception e)  throws SAXException {
  if (e != null) {
    StringWriter stackTrace = new StringWriter();
    e.printStackTrace(new PrintWriter(stackTrace));
    if (msg != null)
        msg += "\n" + stackTrace.toString();
    else
        msg = stackTrace.toString();
  }

  SAXParseException spe = 
       new SAXParseException(msg,getPublicId(),
                    getSystemId(),getLineNumber(),getColumnNumber(),e);

  if (errorHandler != null)
        errorHandler.fatalError(spe);
  else
        throw spe;
}


void reportSkippedEntity(String entity)  throws SAXException {
    if (saxVersion == 2) {
      contentHandler.skippedEntity(entity);
    }
}


void reportPI(String entity, String data)  throws SAXException {
    switch (saxVersion) {
        case 2:
            contentHandler.processingInstruction(entity,data);
            break;
        case 1:
            documentHandler.processingInstruction(entity,data);
            break;
    }
}

void reportUnparsedEntityDecl(String entity, String pubID, String sysID, String notation)  throws SAXException {
    if (dtdHandler != null) {
      dtdHandler.unparsedEntityDecl(entity,pubID,resolveSystemID(sysID),notation);
    }
}

void reportNotationDecl(String name, String pubID, String sysID)  throws SAXException {
    if (dtdHandler != null)
        dtdHandler.notationDecl(name,pubID,resolveSystemID(sysID));
}


void reportStartTag(String ns, String entity, String qEntity)  throws SAXException {
    switch (saxVersion) {
      case 2:
        contentHandler.startElement(ns,entity,qEntity,lexer.attribs);
        break;
      case 1:
        documentHandler.startElement(qEntity,lexer.attribs);
        break;
    }
}


void reportEndTag(String ns, String entity, String qEntity)  throws SAXException {
    switch (saxVersion) {
      case 2:
        contentHandler.endElement(ns,entity,qEntity);
        break;
      case 1:
        documentHandler.endElement(qEntity);
        break;
    }
}


void reportStartPrefixMapping(String prefix, String uri)  throws SAXException {
    if (saxVersion == 2) {
        contentHandler.startPrefixMapping(prefix,uri);
    }
}

void reportEndPrefixMapping(String prefix)  throws SAXException {
    if (saxVersion == 2) {
        contentHandler.endPrefixMapping(prefix);
    }
}



void reportStartDocument()  throws SAXException {
    switch (saxVersion) {
      case 2:
        contentHandler.startDocument();
        break;
      case 1:
        documentHandler.startDocument();
        break;
    }
}

void reportEndDocument()  throws SAXException {
    switch (saxVersion) {
      case 2:
        contentHandler.endDocument();
        break;
      case 1:
        documentHandler.endDocument();
        break;
    }
}

/************************************************************************
 * Methods used for SAX 2 extensions
 ************************************************************************/

// *** LexicalHandler ***

void reportStartDTD(String name, String pubID, String sysID)
throws SAXException {
    if (lexHandler != null)
        lexHandler.startDTD(name,pubID,sysID);
}

void reportEndDTD()
throws SAXException {
    if (lexHandler != null)
        lexHandler.endDTD();
}

void reportStartEntity(String name)
throws SAXException {
    if (lexHandler != null) {
        if (fLexicalParameterEntities || name.charAt(0) != '%')
            lexHandler.startEntity(name);
    }
}

void reportEndEntity(String name)
throws SAXException {
    if (lexHandler != null) {
        if (fLexicalParameterEntities || name.charAt(0) != '%')
            lexHandler.endEntity(name);
    }
}

void reportStartCdata()
throws SAXException {
    if (lexHandler != null)
        lexHandler.startCDATA();
}

void reportEndCdata()
throws SAXException {
    if (lexHandler != null)
        lexHandler.endCDATA();
}

void reportComment(char[] ch, int start, int length)
throws SAXException {
    if (lexHandler != null)
        lexHandler.comment(ch,start,length);
}


/************************************************************************
 * Miscellaneous methods used internally
 ************************************************************************/

private void addAttributeDefinition(String qName, int valueType, int defaultType, String defaultValue)
throws SAXException, IOException {
    String prefix="", localName="";
    if (fNamespaces) {
        localName = qName;
        if (qName == "xmlns") // Internalize all URIs
            defaultValue.intern();
    }

    saveAttributeDefinition(prefix,localName,qName,valueType,defaultType,defaultValue);
}

private void addPrefixedAttributeDefinition(String qName, int valueType, int defaultType, String defaultValue)
throws SAXException, IOException {
    String prefix, localName;
    if (fNamespaces) {
        int colon = qName.indexOf(':');
        int len = qName.length();
        qName.getChars(0,len,lexer.cbuf,0);
        prefix = lexer.stringConverter.convert(lexer.cbuf,0,colon);
        localName = lexer.stringConverter.convert(lexer.cbuf,colon+1,len-(colon+1));
    }
    else {
        prefix=localName="";
    }

    saveAttributeDefinition(prefix,localName,qName,valueType,defaultType,defaultValue);
}

private void saveAttributeDefinition(String prefix,String localName,
                                     String qName, int valueType, int defaultType, String defaultValue)
throws SAXException, IOException {
    try {
        if (defaultValue != null) {
            if (valueType == AttributeDefinition.NMTOKEN || valueType == AttributeDefinition.NMTOKENS)
                defaultValue = lexer.normalizeValue(defaultValue);

            defaultValue = lexer.rescanAttributeValue(defaultValue);
        }

        if (declHandler != null) {
            String valueTypeString = null;
            if (valueType == AttributeDefinition.NOTATION) {
                modelBuffer.insert(0,"NOTATION (");
                modelBuffer.append(')');
                valueTypeString = modelBuffer.toString();
            }
            else
            if (valueType == AttributeDefinition.ENUMERATION) {
                modelBuffer.insert(0,'(');
                modelBuffer.append(')');
                valueTypeString = modelBuffer.toString();
            }
            else
                valueTypeString = AttributeDefinition.getValueTypeString(valueType);

            declHandler.attributeDecl(elementDefinition.getName(),qName,valueTypeString,
                                AttributeDefinition.getDefaultTypeString(defaultType),
                                defaultValue);

            modelBuffer.setLength(0);
        }


        elementDefinition.addAttribute(
            new AttributeDefinition(prefix,localName,qName,valueType,null,
                                    defaultType,defaultValue));
    }
    catch (DuplicateKeyException e) { // Attribute already exists; XML spec says ignore it
    }
}


private String resolveSystemID(String sysID) {
    String resolvedSysID;
    if (fResolveDTDURIs) {
        try {
            return EntityManager.resolveSystemID(docEntity.getSystemID(),sysID);
        }
        catch (MalformedURLException e) {
            return sysID;
        }
    }
    else 
        return sysID;
}



private int yylex() throws IOException, SAXException
{
    try {
     int tok = lexer.yylex();
     yylval = lexer.stringValue;
     lexer.stringValue = null;

    /* Uncomment for serious debugging
    if (yydebug) {
        if (tok == CDATA)
            System.out.println("Token: CDATA");
        else
            System.out.println("Token: " + yyname[tok] + " (" + yylval + ")");

        System.out.println("\tlexical state is now " + lexer.yystate() + ", line number " + getLineNumber() );
    }
    */

     return tok;
    }
    catch (IOException e) {
        while (lexer.currentEntity == null && lexer.entityStack.size() > 0) {
            lexer.currentEntity = (Entity) lexer.entityStack.pop();
            try {
                if (lexer.yymoreStreams())
                    lexer.yypopStream();
            }
            catch (IOException ie) {}
        }

        throw e;
    }
    catch (SAXException e) {
        while (lexer.currentEntity == null && lexer.entityStack.size() > 0) {
            lexer.currentEntity = (Entity) lexer.entityStack.pop();
            try {
                if (lexer.yymoreStreams())
                    lexer.yypopStream();
            }
            catch (IOException ie) {}
        }

        throw e;
    }


}


void yyerror(String msg) throws SAXException {
    // Check if this is because of an invalid entity reference
    if (yychar <= 0)
        reportFatalError("Unexpected end of file after " + yylval);
    else
        reportFatalError("Unexpected element: " + yyname[yychar]);
}








//#line 1241 "Piccolo.java"
//###############################################################
// method: yylexdebug : check lexer state
//###############################################################
void yylexdebug(int state,int ch)
{
String s=null;
  if (ch < 0) ch=0;
  if (ch <= YYMAXTOKEN) //check index bounds
     s = yyname[ch];    //now get it
  if (s==null)
    s = "illegal-symbol";
  debug("state "+state+", reading "+ch+" ("+s+")");
}





//The following are now global, to aid in error reporting
int yyn;       //next next thing to do
int yym;       //
int yystate;   //current parsing state from state table
String yys;    //current token string


//###############################################################
// method: yyparse : parse input and execute indicated items
//###############################################################
int yyparse()
throws SAXException, IOException
{
boolean doaction;
  init_stacks();
  yynerrs = 0;
  yyerrflag = 0;
  yychar = -1;          //impossible char forces a read
  yystate=0;            //initial state
  state_push(yystate);  //save it
  while (true) //until parsing is done, either correctly, or w/error
    {
    doaction=true;
    //if (yydebug) debug("loop"); 
    //#### NEXT ACTION (from reduction table)
    for (yyn=yydefred[yystate];yyn==0;yyn=yydefred[yystate])
      {
      //if (yydebug) debug("yyn:"+yyn+"  state:"+yystate+"  yychar:"+yychar);
      if (yychar < 0)      //we want a char?
        {
        yychar = yylex();  //get next token
        //if (yydebug) debug(" next yychar:"+yychar);
        //#### ERROR CHECK ####
        //if (yychar < 0)    //it it didn't work/error
        //  {
        //  yychar = 0;      //change it to default string (no -1!)
          //if (yydebug)
          //  yylexdebug(yystate,yychar);
        //  }
        }//yychar<0
      yyn = yysindex[yystate];  //get amount to shift by (shift index)
      if ((yyn != 0) && (yyn += yychar) >= 0 &&
          yyn <= YYTABLESIZE && yycheck[yyn] == yychar)
        {
        //if (yydebug)
          //debug("state "+yystate+", shifting to state "+yytable[yyn]);
        //#### NEXT STATE ####
        yystate = yytable[yyn];//we are in a new state
        state_push(yystate);   //save it
        val_push(yylval);      //push our lval as the input for next rule
        yychar = -1;           //since we have 'eaten' a token, say we need another
        if (yyerrflag > 0)     //have we recovered an error?
           --yyerrflag;        //give ourselves credit
        doaction=false;        //but don't process yet
        break;   //quit the yyn=0 loop
        }

    yyn = yyrindex[yystate];  //reduce
    if ((yyn !=0 ) && (yyn += yychar) >= 0 &&
            yyn <= YYTABLESIZE && yycheck[yyn] == yychar)
      {   //we reduced!
      //if (yydebug) debug("reduce");
      yyn = yytable[yyn];
      doaction=true; //get ready to execute
      break;         //drop down to actions
      }
    else //ERROR RECOVERY
      {
      if (yyerrflag==0)
        {
        yyerror("syntax error");
        yynerrs++;
        }
      if (yyerrflag < 3) //low error count?
        {
        yyerrflag = 3;
        while (true)   //do until break
          {
          yyn = yysindex[state_peek(0)];
          if ((yyn != 0) && (yyn += YYERRCODE) >= 0 &&
                    yyn <= YYTABLESIZE && yycheck[yyn] == YYERRCODE)
            {
            //if (yydebug)
              //debug("state "+state_peek(0)+", error recovery shifting to state "+yytable[yyn]+" ");
            yystate = yytable[yyn];
            state_push(yystate);
            val_push(yylval);
            doaction=false;
            break;
            }
          else
            {
            //if (yydebug)
              //debug("error recovery discarding state "+state_peek(0)+" ");
            state_pop();
            val_pop();
            }
          }
        }
      else            //discard this token
        {
        if (yychar == 0)
          return 1; //yyabort
        //if (yydebug)
          //{
          //yys = null;
          //if (yychar <= YYMAXTOKEN) yys = yyname[yychar];
          //if (yys == null) yys = "illegal-symbol";
          //debug("state "+yystate+", error recovery discards token "+yychar+" ("+yys+")");
          //}
        yychar = -1;  //read another
        }
      }//end error recovery
    }//yyn=0 loop
    if (!doaction)   //any reason not to proceed?
      continue;      //skip action
    yym = yylen[yyn];          //get count of terminals on rhs
    //if (yydebug)
      //debug("state "+yystate+", reducing "+yym+" by rule "+yyn+" ("+yyrule[yyn]+")");
    if (yym>0)                 //if count of rhs not 'nil'
      yyval = val_peek(yym-1); //get current semantic value
    switch(yyn)
      {
//########## USER-SUPPLIED ACTIONS ##########
case 20:
//#line 111 "Piccolo.y"
{
        /* Internal subset only*/
        lexer.yybegin(0);
        reportEndDTD();
    }
break;
case 21:
//#line 116 "Piccolo.y"
{
        /* Internal+External or External only with an <?xml?> declaration*/
        lexer.yybegin(0);
        reportEndDTD();
    }
break;
case 22:
//#line 121 "Piccolo.y"
{
        /* Internal+External or External only with no <?xml?> declaration*/
        lexer.yybegin(0);
        reportEndDTD();
    }
break;
case 23:
//#line 126 "Piccolo.y"
{
        /* External subset with no internal subset. Skip external*/
        dtdName = val_peek(4);
        lexer.yybegin(0);
        reportStartDTD(dtdName,pubID,sysID);
        reportEndDTD();
    }
break;
case 25:
//#line 136 "Piccolo.y"
{
        /* External subset with no internal subset*/
        lexer.pushEntity("[dtd]",dtdPubID,dtdSysID,false,true);
        lexer.yybegin(lexer.DTD);
    }
break;
case 26:
//#line 142 "Piccolo.y"
{
        /* Both external and internal subsets. Internal comes first.*/
        lexer.pushEntity("[dtd]",dtdPubID,dtdSysID,false,true);
        lexer.yybegin(lexer.DTD);
    }
break;
case 27:
//#line 151 "Piccolo.y"
{
        dtdName = lexer.normalizeValue(val_peek(5));
        dtdPubID = null;
        dtdSysID = lexer.normalizeValue(val_peek(1));
        reportStartDTD(dtdName,dtdPubID,dtdSysID);
    }
break;
case 28:
//#line 158 "Piccolo.y"
{
        dtdName = val_peek(7);
        dtdPubID = lexer.normalizeValue(val_peek(3));
        dtdSysID = lexer.normalizeValue(val_peek(1));
        reportStartDTD(dtdName,dtdPubID,dtdSysID);
    }
break;
case 29:
//#line 167 "Piccolo.y"
{
        dtdName = val_peek(1);
        reportStartDTD(dtdName,null,null);
    }
break;
case 30:
//#line 171 "Piccolo.y"
{
        dtdName = val_peek(1);
        reportStartDTD(dtdName,null,null);
    }
break;
case 31:
//#line 175 "Piccolo.y"
{
        dtdName = val_peek(3);
        reportStartDTD(dtdName,pubID,sysID);
    }
break;
case 32:
//#line 182 "Piccolo.y"
{ pubID=null; sysID=lexer.normalizeValue(val_peek(0)); }
break;
case 33:
//#line 183 "Piccolo.y"
{
            pubID=lexer.normalizeValue(val_peek(2));
            sysID=lexer.normalizeValue(val_peek(0));
        }
break;
case 45:
//#line 203 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD);
  }
break;
case 46:
//#line 209 "Piccolo.y"
{
    lexer.yybegin(lexer.DTD);
}
break;
case 48:
//#line 215 "Piccolo.y"
{
    lexer.yybegin(lexer.DTD_IGNORE);
}
break;
case 52:
//#line 228 "Piccolo.y"
{
        lexer.entityManager.putInternal(val_peek(4),val_peek(2),EntityManager.GENERAL);
        if (declHandler != null)
            declHandler.internalEntityDecl(val_peek(4),val_peek(2));
    }
break;
case 53:
//#line 233 "Piccolo.y"
{
        try {
            lexer.entityManager.putExternal(lexer.currentEntity,val_peek(4),pubID,sysID,EntityManager.GENERAL);
            if (declHandler != null)
                declHandler.externalEntityDecl(val_peek(4),pubID,resolveSystemID(sysID));
        }
        catch (MalformedURLException e) {
            reportFatalError("Invalid system identifier: "
                         + sysID + "; " + e.getMessage());
        }
    }
break;
case 54:
//#line 244 "Piccolo.y"
{
        try {
            lexer.entityManager.putUnparsed(lexer.currentEntity,val_peek(8),pubID,sysID,val_peek(2),EntityManager.GENERAL);
            reportUnparsedEntityDecl(val_peek(8),pubID,sysID,val_peek(2));

        }
        catch (MalformedURLException e) {
            reportFatalError("Invalid system identifier: "
                         + sysID + "; " + e.getMessage());
        }
    }
break;
case 55:
//#line 255 "Piccolo.y"
{
        lexer.entityManager.putInternal(val_peek(4),val_peek(2),EntityManager.PARAMETER);
        if (declHandler != null)
            declHandler.internalEntityDecl("%"+val_peek(4),val_peek(2));
    }
break;
case 56:
//#line 260 "Piccolo.y"
{
        try {
            lexer.entityManager.putExternal(lexer.currentEntity,val_peek(4),pubID,sysID,EntityManager.PARAMETER);
            if (declHandler != null)
                declHandler.externalEntityDecl("%"+val_peek(4),pubID,resolveSystemID(sysID));
        }
        catch (MalformedURLException e) {
            reportFatalError("Invalid system identifier: "
                         + sysID + "; " + e.getMessage());
        }
    }
break;
case 57:
//#line 271 "Piccolo.y"
{
        try {
            lexer.entityManager.putUnparsed(lexer.currentEntity,val_peek(7),pubID,sysID,val_peek(2),EntityManager.PARAMETER);
            reportUnparsedEntityDecl(val_peek(7),pubID,sysID,val_peek(2));

        }
        catch (MalformedURLException e) {
            reportFatalError("Invalid system identifier: "
                         + sysID + "; " + e.getMessage());
        }
    }
break;
case 58:
//#line 285 "Piccolo.y"
{
                reportNotationDecl(val_peek(4),pubID,sysID);
              }
break;
case 59:
//#line 288 "Piccolo.y"
{
                reportNotationDecl(val_peek(6),lexer.normalizeValue(val_peek(2)),null);
              }
break;
case 60:
//#line 294 "Piccolo.y"
{
	lexer.defineElement(elementDefinition.getName(),elementDefinition);
}
break;
case 61:
//#line 299 "Piccolo.y"
{
	/* Look up this element. If we've seen previous ATTLIST definitions for it, we'll add these to it.*/
	elementDefinition = lexer.getElement(val_peek(0));
	if (elementDefinition == null)
		elementDefinition = new ElementDefinition(val_peek(0));
}
break;
case 64:
//#line 313 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD_ATT_NAME);
        addPrefixedAttributeDefinition(val_peek(4),attributeType,AttributeDefinition.REQUIRED,null);
    }
break;
case 65:
//#line 317 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD_ATT_NAME);
        addAttributeDefinition(val_peek(4),attributeType,AttributeDefinition.REQUIRED,null);
    }
break;
case 66:
//#line 321 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD_ATT_NAME);
        addPrefixedAttributeDefinition(val_peek(4),attributeType,AttributeDefinition.IMPLIED,null);
    }
break;
case 67:
//#line 325 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD_ATT_NAME);
        addAttributeDefinition(val_peek(4),attributeType,AttributeDefinition.IMPLIED,null);
    }
break;
case 68:
//#line 329 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD_ATT_NAME);
        addPrefixedAttributeDefinition(val_peek(6),attributeType,AttributeDefinition.FIXED,val_peek(0));
    }
break;
case 69:
//#line 333 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD_ATT_NAME);
        addAttributeDefinition(val_peek(6),attributeType,AttributeDefinition.FIXED,val_peek(0));
    }
break;
case 70:
//#line 337 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD_ATT_NAME);
        addPrefixedAttributeDefinition(val_peek(4),attributeType,0,val_peek(0));
    }
break;
case 71:
//#line 341 "Piccolo.y"
{
        lexer.yybegin(lexer.DTD_ATT_NAME);
        addAttributeDefinition(val_peek(4),attributeType,0,val_peek(0));
    }
break;
case 72:
//#line 348 "Piccolo.y"
{ attributeType = AttributeDefinition.CDATA; }
break;
case 73:
//#line 349 "Piccolo.y"
{ attributeType = AttributeDefinition.ID; }
break;
case 74:
//#line 350 "Piccolo.y"
{ attributeType = AttributeDefinition.IDREF; }
break;
case 75:
//#line 351 "Piccolo.y"
{ attributeType = AttributeDefinition.IDREFS; }
break;
case 76:
//#line 352 "Piccolo.y"
{ attributeType = AttributeDefinition.ENTITY; }
break;
case 77:
//#line 353 "Piccolo.y"
{ attributeType = AttributeDefinition.ENTITIES; }
break;
case 78:
//#line 354 "Piccolo.y"
{ attributeType = AttributeDefinition.NMTOKEN; }
break;
case 79:
//#line 355 "Piccolo.y"
{ attributeType = AttributeDefinition.NMTOKENS; }
break;
case 80:
//#line 356 "Piccolo.y"
{
                attributeType = AttributeDefinition.ENUMERATION;
          }
break;
case 81:
//#line 359 "Piccolo.y"
{
                attributeType = AttributeDefinition.NOTATION;
          }
break;
case 82:
//#line 365 "Piccolo.y"
{
        if (declHandler != null)
            modelBuffer.append(val_peek(0));
    }
break;
case 83:
//#line 369 "Piccolo.y"
{
        if (declHandler != null) {
            modelBuffer.append('|');
            modelBuffer.append(val_peek(0));
        }
    }
break;
case 84:
//#line 378 "Piccolo.y"
{
    if (declHandler != null)
        declHandler.elementDecl(val_peek(4),val_peek(2));
}
break;
case 85:
//#line 385 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "EMPTY";
      }
break;
case 86:
//#line 389 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "ANY";
    }
break;
case 87:
//#line 393 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(0);
    }
break;
case 88:
//#line 397 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(0);
    }
break;
case 89:
//#line 404 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "(#PCDATA)*";
    }
break;
case 90:
//#line 408 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "(#PCDATA)";
    }
break;
case 91:
//#line 412 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "(#PCDATA|" + modelBuffer.toString() + ")*";
    }
break;
case 92:
//#line 416 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(0);
    }
break;
case 93:
//#line 423 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(1) + val_peek(0);
    }
break;
case 94:
//#line 427 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(1) + val_peek(0);
    }
break;
case 95:
//#line 431 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(0);
    }
break;
case 96:
//#line 438 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(4) + "|" + val_peek(0);
    }
break;
case 97:
//#line 442 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(4) + "|" + val_peek(0);
    }
break;
case 98:
//#line 449 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(0);
    }
break;
case 99:
//#line 453 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(3) + "," + val_peek(0);
    }
break;
case 100:
//#line 460 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(2) + val_peek(1);
    }
break;
case 101:
//#line 464 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(2) + val_peek(1);
    }
break;
case 102:
//#line 468 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(2) + val_peek(1);
    }
break;
case 103:
//#line 472 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(0);
    }
break;
case 104:
//#line 480 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "(" + val_peek(2) + ")";
    }
break;
case 105:
//#line 484 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(0);
    }
break;
case 106:
//#line 491 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "(" + val_peek(2) + ")";
    }
break;
case 107:
//#line 495 "Piccolo.y"
{
        if (declHandler != null)
            yyval = val_peek(0);
    }
break;
case 108:
//#line 502 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "?";
    }
break;
case 109:
//#line 506 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "*";
    }
break;
case 110:
//#line 510 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "+";
    }
break;
case 111:
//#line 514 "Piccolo.y"
{
        if (declHandler != null)
            yyval = "";
    }
break;
case 113:
//#line 524 "Piccolo.y"
{
                lexer.setTokenize(false);
          }
break;
case 114:
//#line 528 "Piccolo.y"
{
                lexer.setTokenize(false);
          }
break;
case 115:
//#line 532 "Piccolo.y"
{
                lexer.setTokenize(false);
          }
break;
case 120:
//#line 539 "Piccolo.y"
{
                reportWhitespace();
          }
break;
//#line 1931 "Piccolo.java"
//########## END OF USER-SUPPLIED ACTIONS ##########
    }//switch
    //#### Now let's reduce... ####
    //if (yydebug) debug("reduce");
    state_drop(yym);             //we just reduced yylen states
    yystate = state_peek(0);     //get new state
    val_drop(yym);               //corresponding value drop
    yym = yylhs[yyn];            //select next TERMINAL(on lhs)
    if (yystate == 0 && yym == 0)//done? 'rest' state and at first TERMINAL
      {
      //if (yydebug) debug("After reduction, shifting from state 0 to state "+YYFINAL+"");
      yystate = YYFINAL;         //explicitly say we're done
      state_push(YYFINAL);       //and save it
      val_push(yyval);           //also save the semantic value of parsing
      if (yychar < 0)            //we want another character?
        {
        yychar = yylex();        //get next character
        //if (yychar<0) yychar=0;  //clean, if necessary
        //if (yydebug)
          //yylexdebug(yystate,yychar);
        }
      if (yychar == 0)          //Good exit (if lex returns 0 ;-)
         break;                 //quit the loop--all DONE
      }//if yystate
    else                        //else not done yet
      {                         //get next state and push, for next yydefred[]
      yyn = yygindex[yym];      //find out where to go
      if ((yyn != 0) && (yyn += yystate) >= 0 &&
            yyn <= YYTABLESIZE && yycheck[yyn] == yystate)
        yystate = yytable[yyn]; //get new state
      else
        yystate = yydgoto[yym]; //else go to new defred
      //if (yydebug) debug("after reduction, shifting from state "+state_peek(0)+" to state "+yystate+"");
      state_push(yystate);     //going again, so push state & val...
      val_push(yyval);         //for next action
      }
    }//main loop
  return 0;//yyaccept!!
}
//## end of method parse() ######################################



//## run() --- for Thread #######################################
//## The -Jnorun option was used ##
//## end of method run() ########################################



//## Constructors ###############################################
//## The -Jnoconstruct option was used ##
//###############################################################



}
//################### END OF CLASS ##############################

/* Generated By:JavaCC: Do not edit this line. StrategicMapLoaderTokenManager.java */
import java.util.*;

public class StrategicMapLoaderTokenManager implements StrategicMapLoaderConstants
{
  public  java.io.PrintStream debugStream = System.out;
  public  void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }
private final int jjStopStringLiteralDfa_0(int pos, long active0)
{
   switch (pos)
   {
      default :
         return -1;
   }
}
private final int jjStartNfa_0(int pos, long active0)
{
   return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
}
private final int jjStopAtPos(int pos, int kind)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   return pos + 1;
}
private final int jjStartNfaWithStates_0(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_0(state, pos + 1);
}
private final int jjMoveStringLiteralDfa0_0()
{
   switch(curChar)
   {
      case 10:
         return jjStopAtPos(0, 4);
      case 67:
      case 99:
         return jjMoveStringLiteralDfa1_0(0x20L);
      default :
         return jjMoveNfa_0(6, 0);
   }
}
private final int jjMoveStringLiteralDfa1_0(long active0)
{
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(0, active0);
      return 1;
   }
   switch(curChar)
   {
      case 65:
      case 97:
         return jjMoveStringLiteralDfa2_0(active0, 0x20L);
      default :
         break;
   }
   return jjStartNfa_0(0, active0);
}
private final int jjMoveStringLiteralDfa2_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(0, old0); 
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(1, active0);
      return 2;
   }
   switch(curChar)
   {
      case 83:
      case 115:
         return jjMoveStringLiteralDfa3_0(active0, 0x20L);
      default :
         break;
   }
   return jjStartNfa_0(1, active0);
}
private final int jjMoveStringLiteralDfa3_0(long old0, long active0)
{
   if (((active0 &= old0)) == 0L)
      return jjStartNfa_0(1, old0); 
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(2, active0);
      return 3;
   }
   switch(curChar)
   {
      case 69:
      case 101:
         if ((active0 & 0x20L) != 0L)
            return jjStopAtPos(3, 5);
         break;
      default :
         break;
   }
   return jjStartNfa_0(2, active0);
}
private final void jjCheckNAdd(int state)
{
   if (jjrounds[state] != jjround)
   {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
   }
}
private final void jjAddStates(int start, int end)
{
   do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
   } while (start++ != end);
}
private final void jjCheckNAddTwoStates(int state1, int state2)
{
   jjCheckNAdd(state1);
   jjCheckNAdd(state2);
}
private final void jjCheckNAddStates(int start, int end)
{
   do {
      jjCheckNAdd(jjnextStates[start]);
   } while (start++ != end);
}
private final void jjCheckNAddStates(int start)
{
   jjCheckNAdd(jjnextStates[start]);
   jjCheckNAdd(jjnextStates[start + 1]);
}
private final int jjMoveNfa_0(int startState, int curPos)
{
   int[] nextStates;
   int startsAt = 0;
   jjnewStateCnt = 75;
   int i = 1;
   jjstateSet[0] = startState;
   int j, kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 6:
               case 0:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  kind = 6;
                  jjCheckNAdd(0);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 6:
                  if ((0x400000004L & l) != 0L)
                     jjAddStates(0, 1);
                  else if ((0x200000002000L & l) != 0L)
                     jjAddStates(2, 3);
                  else if ((0x10000000100000L & l) != 0L)
                     jjAddStates(4, 5);
                  else if ((0x200000002L & l) != 0L)
                     jjAddStates(6, 8);
                  else if ((0x8000000080000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 30;
                  else if ((0x10000000100L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 25;
                  else if ((0x1000000010L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 21;
                  else if ((0x80000000800000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 15;
                  else if ((0x1000000010000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 11;
                  else if ((0x40000000400L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 5;
                  break;
               case 1:
                  if ((0x2000000020L & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               case 2:
                  if ((0x100000001000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 1;
                  break;
               case 3:
                  if ((0x8000000080L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 2;
                  break;
               case 4:
                  if ((0x400000004000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 3;
                  break;
               case 5:
                  if ((0x20000000200000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 4;
                  break;
               case 7:
                  if ((0x8000000080000L & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               case 8:
               case 60:
                  if ((0x400000004000L & l) != 0L)
                     jjCheckNAdd(7);
                  break;
               case 9:
                  if ((0x20000000200L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 8;
                  break;
               case 10:
                  if ((0x200000002L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 9;
                  break;
               case 11:
                  if ((0x100000001000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 10;
                  break;
               case 12:
                  if ((0x1000000010000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 11;
                  break;
               case 13:
                  if ((0x1000000010L & l) != 0L)
                     jjCheckNAdd(7);
                  break;
               case 14:
                  if ((0x800000008000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 13;
                  break;
               case 15:
                  if ((0x800000008000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 14;
                  break;
               case 16:
                  if ((0x80000000800000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 15;
                  break;
               case 17:
                  if ((0x10000000100000L & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               case 18:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 17;
                  break;
               case 19:
                  if ((0x2000000020L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 18;
                  break;
               case 20:
                  if ((0x8000000080000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 19;
                  break;
               case 21:
                  if ((0x2000000020L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 20;
                  break;
               case 22:
                  if ((0x1000000010L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 21;
                  break;
               case 23:
                  if ((0x100000001000L & l) != 0L)
                     jjCheckNAdd(7);
                  break;
               case 24:
                  if ((0x100000001000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 23;
                  break;
               case 25:
                  if ((0x20000000200L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 24;
                  break;
               case 26:
                  if ((0x10000000100L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 25;
                  break;
               case 27:
                  if ((0x1000000010000L & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               case 28:
                  if ((0x200000002000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 27;
                  break;
               case 29:
                  if ((0x200000002L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 28;
                  break;
               case 30:
                  if ((0x80000000800000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 29;
                  break;
               case 31:
                  if ((0x8000000080000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 30;
                  break;
               case 32:
                  if ((0x200000002L & l) != 0L)
                     jjAddStates(6, 8);
                  break;
               case 33:
                  if ((0x80000000800000L & l) != 0L && kind > 9)
                     kind = 9;
                  break;
               case 34:
                  if ((0x800000008000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 33;
                  break;
               case 35:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 34;
                  break;
               case 36:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 35;
                  break;
               case 37:
                  if ((0x8000000080000L & l) != 0L && kind > 9)
                     kind = 9;
                  break;
               case 38:
                  if ((0x80000000800000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 37;
                  break;
               case 39:
                  if ((0x800000008000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 38;
                  break;
               case 40:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 39;
                  break;
               case 41:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 40;
                  break;
               case 42:
                  if ((0x10000000100L & l) != 0L && kind > 9)
                     kind = 9;
                  break;
               case 43:
                  if ((0x800000008L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 42;
                  break;
               case 44:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 43;
                  break;
               case 45:
                  if ((0x10000000100000L & l) != 0L)
                     jjAddStates(4, 5);
                  break;
               case 46:
                  if ((0x4000000040000L & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               case 47:
                  if ((0x2000000020L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 46;
                  break;
               case 48:
                  if ((0x80000000800000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 47;
                  break;
               case 49:
                  if ((0x800000008000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 48;
                  break;
               case 50:
                  if ((0x200000002L & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               case 51:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 50;
                  break;
               case 52:
                  if ((0x1000000010L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 51;
                  break;
               case 53:
                  if ((0x400000004000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 52;
                  break;
               case 54:
                  if ((0x20000000200000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 53;
                  break;
               case 55:
                  if ((0x200000002000L & l) != 0L)
                     jjAddStates(2, 3);
                  break;
               case 56:
                  if ((0x10000000100L & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               case 57:
               case 68:
                  if ((0x8000000080000L & l) != 0L)
                     jjCheckNAdd(56);
                  break;
               case 58:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 57;
                  break;
               case 59:
                  if ((0x200000002L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 58;
                  break;
               case 61:
                  if ((0x20000000200L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 60;
                  break;
               case 62:
                  if ((0x200000002L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 61;
                  break;
               case 63:
                  if ((0x10000000100000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 62;
                  break;
               case 64:
                  if ((0x400000004000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 63;
                  break;
               case 65:
                  if ((0x20000000200000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 64;
                  break;
               case 66:
                  if ((0x800000008000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 65;
                  break;
               case 67:
                  if ((0x400000004L & l) != 0L)
                     jjAddStates(0, 1);
                  break;
               case 69:
                  if ((0x20000000200000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 68;
                  break;
               case 70:
                  if ((0x4000000040000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 69;
                  break;
               case 71:
                  if ((0x80000000800L & l) != 0L)
                     kind = 9;
                  break;
               case 72:
                  if ((0x800000008L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 71;
                  break;
               case 73:
                  if ((0x800000008000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 72;
                  break;
               case 74:
                  if ((0x100000001000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 73;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 75 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
static final int[] jjnextStates = {
   70, 74, 59, 66, 49, 54, 36, 41, 44, 
};
public static final String[] jjstrLiteralImages = {
"", null, null, null, "\12", null, null, null, null, null, };
public static final String[] lexStateNames = {
   "DEFAULT", 
};
static final long[] jjtoToken = {
   0x371L, 
};
static final long[] jjtoSkip = {
   0xeL, 
};
private SimpleCharStream input_stream;
private final int[] jjrounds = new int[75];
private final int[] jjstateSet = new int[150];
protected char curChar;
public StrategicMapLoaderTokenManager(SimpleCharStream stream)
{
   if (SimpleCharStream.staticFlag)
      throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
   input_stream = stream;
}
public StrategicMapLoaderTokenManager(SimpleCharStream stream, int lexState)
{
   this(stream);
   SwitchTo(lexState);
}
public void ReInit(SimpleCharStream stream)
{
   jjmatchedPos = jjnewStateCnt = 0;
   curLexState = defaultLexState;
   input_stream = stream;
   ReInitRounds();
}
private final void ReInitRounds()
{
   int i;
   jjround = 0x80000001;
   for (i = 75; i-- > 0;)
      jjrounds[i] = 0x80000000;
}
public void ReInit(SimpleCharStream stream, int lexState)
{
   ReInit(stream);
   SwitchTo(lexState);
}
public void SwitchTo(int lexState)
{
   if (lexState >= 1 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
   else
      curLexState = lexState;
}

private final Token jjFillToken()
{
   Token t = Token.newToken(jjmatchedKind);
   t.kind = jjmatchedKind;
   String im = jjstrLiteralImages[jjmatchedKind];
   t.image = (im == null) ? input_stream.GetImage() : im;
   t.beginLine = input_stream.getBeginLine();
   t.beginColumn = input_stream.getBeginColumn();
   t.endLine = input_stream.getEndLine();
   t.endColumn = input_stream.getEndColumn();
   return t;
}

int curLexState = 0;
int defaultLexState = 0;
int jjnewStateCnt;
int jjround;
int jjmatchedPos;
int jjmatchedKind;

public final Token getNextToken() 
{
  int kind;
  Token specialToken = null;
  Token matchedToken;
  int curPos = 0;

  EOFLoop :
  for (;;)
  {   
   try   
   {     
      curChar = input_stream.BeginToken();
   }     
   catch(java.io.IOException e)
   {        
      jjmatchedKind = 0;
      matchedToken = jjFillToken();
      return matchedToken;
   }

   try { input_stream.backup(0);
      while (curChar <= 32 && (0x100002200L & (1L << curChar)) != 0L)
         curChar = input_stream.BeginToken();
   }
   catch (java.io.IOException e1) { continue EOFLoop; }
   jjmatchedKind = 0x7fffffff;
   jjmatchedPos = 0;
   curPos = jjMoveStringLiteralDfa0_0();
   if (jjmatchedKind != 0x7fffffff)
   {
      if (jjmatchedPos + 1 < curPos)
         input_stream.backup(curPos - jjmatchedPos - 1);
      if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
      {
         matchedToken = jjFillToken();
         return matchedToken;
      }
      else
      {
         continue EOFLoop;
      }
   }
   int error_line = input_stream.getEndLine();
   int error_column = input_stream.getEndColumn();
   String error_after = null;
   boolean EOFSeen = false;
   try { input_stream.readChar(); input_stream.backup(1); }
   catch (java.io.IOException e1) {
      EOFSeen = true;
      error_after = curPos <= 1 ? "" : input_stream.GetImage();
      if (curChar == '\n' || curChar == '\r') {
         error_line++;
         error_column = 0;
      }
      else
         error_column++;
   }
   if (!EOFSeen) {
      input_stream.backup(1);
      error_after = curPos <= 1 ? "" : input_stream.GetImage();
   }
   throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
  }
}

}

# Makefile for Titan
# dripton  June 26, 1998

JC = javac -J-Djavac.pipe.output=true -g -verbose -deprecation
#JC = javac -J-Djavac.pipe.output=true -O:interclass

all: BattleMap.class Chit.class Concede.class Creature.class Game.class \
    Hex.class Legion.class MasterBoard.class MasterHex.class MessageBox.class \
    Negotiate.class PickMarker.class PickRecruit.class Player.class \
    ShowLegion.class SplitLegion.class Turn.class

BattleMap.class: BattleMap.java
	$(JC) BattleMap.java

Chit.class: Chit.java
	$(JC) Chit.java

Concede.class: Concede.java
	$(JC) Concede.java

Creature.class: Creature.java
	$(JC) Creature.java

Game.class: Game.java
	$(JC) Game.java

Hex.class: BattleMap.java
	$(JC) BattleMap.java

Legion.class: Legion.java
	$(JC) Legion.java

MasterBoard.class: MasterBoard.java
	$(JC) MasterBoard.java

MasterHex.class: MasterHex.java
	$(JC) MasterHex.java

MessageBox.class: MessageBox.java
	$(JC) MessageBox.java

Negotiate.class: Negotiate.java
	$(JC) Negotiate.java

PickMarker.class: PickMarker.java
	$(JC) PickMarker.java

PickRecruit.class: PickRecruit.java
	$(JC) PickRecruit.java

Player.class: Player.java
	$(JC) Player.java

ShowLegion.class: ShowLegion.java
	$(JC) ShowLegion.java

SplitLegion.class: SplitLegion.java
	$(JC) SplitLegion.java

Turn.class: Turn.java
	$(JC) Turn.java


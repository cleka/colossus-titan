#!/usr/bin/perl -w
use strict;


# Prototype to figure out balanced tower algorithm for Colossus
# dripton 20020305

# We use 0-based math everywhere to make mods work easily.
# Convert at the end for output.



# Convert from 0-based to 1-based then multiply by 100.
sub pretty_tower_num($)
{
    my ($ord) = shift;
    return 100 * ($ord + 1);
}

my $ntowers;
my $nplayers;

if (@ARGV >= 2)
{
    $ntowers = $ARGV[0];
    $nplayers = $ARGV[1];
}
else
{
    print "Enter number of towers: ";
    $ntowers = <STDIN>;
    chomp $ntowers;

    print "Enter number of players: ";
    $nplayers = <STDIN>;
    chomp $nplayers;
}

print "$ntowers towers and $nplayers players\n";

if ($nplayers > $ntowers)
{
    die "More players than towers -- can't work.  Aborting.\n";
}

if ($nplayers < 1)
{
    die "No players.  Aborting.\n";
}

my $towers_per_player = $ntowers / $nplayers;

print "towers / players = $towers_per_player\n";



# Figure out the starting sequence of towers.
my %sequence;
my $ndone = 0;
my $f = 0;
# Prevent roundoff error
my $epsilon = 0.000001;
while ($ndone < $nplayers)
{
    $sequence{int $f + $epsilon} = 1;
    $ndone++;
    $f += $towers_per_player; 
}

print "starting sequence: ";
foreach my $key (sort {$a <=> $b} keys %sequence)
{
    print $key . " "
}
print "\n";


# Pick a random starting tower.
my $offset = int rand $ntowers;
print "random offset =    $offset\n"; 

# Make the sequence relative to the starting tower.
my %offset_sequence;
foreach my $key (sort {$a <=> $b} keys %sequence)
{
    $offset_sequence{($key + $offset) % $ntowers} = 1;
}
my @towers = keys %offset_sequence;

print "offset sequence:   ";
foreach my $tower (sort {$a <=> $b} @towers)
{
    print &pretty_tower_num($tower) . " "
}
print "\n";



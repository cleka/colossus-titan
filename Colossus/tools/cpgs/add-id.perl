#! /usr/bin/perl

use strict;
use Data::Dumper;


my $USERS_FILE = "users.dat";
my $NEW_FILE =  "users.dat.new";

my %data = ();

my $what = "lastonline";
my $cnt = 0;

$what = "registered";

open(NEW, ">$NEW_FILE") or die "Can't open/write new file '$NEW_FILE'!\nDied ";

open(USERS, $USERS_FILE) or die "Can't open 'user file $USERS_FILE' !\nDied ";

while(<USERS>)
{
    chomp $_;
#    print "$_\n";

    my @fields = split(/ ~ /, $_);
    if (@fields <= 8)
    {
#	print "no id column, adding it.\n";
    }
    else
    {
	print STDERR "Already 9 columns, giving up!\n";
    }
    my $user = $fields[0];
    $cnt++;
    my $registered = $fields[4];
    my $lastlogin = $fields[5];
    my $lastlogout = $fields[6];

    if ($lastlogout lt $lastlogin)
    {
	my $waslastlogout = $lastlogout;
	$lastlogout = $lastlogin;
	my $secs = substr($lastlogout, 17, 18);
	if ($secs == 59)
	{
	    print STDERR "WARNING: can't increment seconds: $lastlogin  ($user)\n";
	    substr($lastlogin, 17, 18) = "58";

	    if ($registered gt $lastlogin)
	    {
		
		print STDERR "Needed to fix also registration:  $registered => $lastlogin\n";
		$registered = $lastlogin;
	    }
	}
	else
	{
	    $secs++;
	    substr($lastlogout, 17,18) = sprintf("%02d", $secs);
	}

    }
    my $seconds = $fields[7];

    
    $data{$user} = { 
		     name        => $user,
                     email       => $fields[1],
                     password    => $fields[2],
		     type        => $fields[3],
                     registered  => $registered,
                     lastlogin   => $lastlogin,
                     lastlogout  => $lastlogout,
                     seconds     => $seconds      }; 
}


close USERS;
my @users = sort by_reg_date keys %data;

my $id = 1;
foreach my $u (@users)
{
    if ( $what eq "registered" )
    {
        # printf "%05d %-30s %s\n", $id, $u, $data{$u}{registered};
    }

    my $ref = $data{$u};
    my @cols = ( $id,
		 $data{$u}->{name},
		 $data{$u}->{email},
		 $data{$u}->{password},
		 $data{$u}->{type},
		 $data{$u}->{registered},
		 $data{$u}->{lastlogin},
		 $data{$u}->{lastlogout},
		 $data{$u}->{seconds},
		 );

    my $line = join(" ~ ", @cols);
    print NEW $line, "\n";
    $id++;
}

close NEW;
print "\nYeah, $cnt users converted.\n";

sub by_reg_date
{
    my $ra = $data{$a}{$what};
    my $rb = $data{$b}{$what};

    return $ra cmp $rb;
}




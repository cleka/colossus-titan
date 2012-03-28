#! /usr/bin/perl

my $USERS_FILE = "/work/colossus/cpgs/data/users.dat";
if (`hostname --short` eq "cleka.net")
{
    $USERS_FILE= "/var/colossus/data/users.dat"
}


my %data = ();

my $what = "lastonline";
my $cnt = 0;

$what = "registered" if @ARGV > 0 and $ARGV[0] =~ /^r/i;
$what = "seconds"    if @ARGV > 0 and $ARGV[0] =~ /^s/i;

open(USERS, $USERS_FILE) or die "Can't open 'user file $USERS_FILE' !\nDied ";


while(<USERS>)
{
    chomp $_;
    my @fields = split(/ ~ /, $_);
    if (@fields != 9)
    {
	die "Wrong format - 9 columns expected!\nDied ";
    }
    # id just inserted recently, fix indices one day...
    my $id = shift @fields;
    my $user = $fields[0];
    next if $user =~ /^(admin|clemens|nobody|dummy[0-9][0-9]?)$/;
    $cnt++;
    my $lastonline = $fields[5];
    $lastonline = $fields[6] if $fields[6] gt $lastonline;
    my $stillonline = ($fields[5] gt $fields[6] ? 1 : 0);
    # print "still online $stillonline\n";

    my $seconds = $fields[7];
    $data{$user} = { id         => $id,
		     name 	=> $user,
                     registered => $fields[4],
                     lastlogin  => $fields[5],
                     lastlogout => $fields[6],
                     lastonline => $lastonline,
                     stillonline => $stillonline,
                     seconds    => $seconds      }; 
}

close USERS;
my @users;
if ($what eq "seconds")
{
    @users = sort by_online keys %data;
}
else
{
    @users = sort by_date keys %data;
}


for $u (@users)
{
    if ( $what eq "registered" )
    {
        printf "%5d %-30s RD: %s | LL: %s\n", $data{$u}{id}, $u, 
	    $data{$u}{registered}, $data{$u}{lastlogin};
    }
    else
    {
        my $secs = $data{$u}{seconds};

        my $seconds = $secs % 60;
        my $minutes  = ($secs - $seconds) / 60;
        my $mins = $minutes % 60;
        $minutes -= $mins;
        my $hours = $minutes / 60;
        my $onlinetime = sprintf("%4d:%02d:%02d", $hours, $mins, $seconds);
        # does not work - last logout data in some cases wrong???
	my $star = $data{$u}{stillonline} ? "*" : " ";
#        $star = " ";
        printf "%5d %-30s %1s LL: %20s | LX: %20s | OT: %10s\n", 
	    $data{$u}{id}, $u, $star, $data{$u}{lastlogin}, 
	    $data{$u}{lastlogout}, $onlinetime;
    }
}

print "\n$cnt users.\n";



sub by_date
{
    # does not work yet... probably because many old entries have wrong logout date.
    if ($what eq "lastonline" and 0)
    {
        # still online is higher than not online
        # both online: last login date
        # both offline: last logout date
        #
        return $data{$a}{stillonline} <=> $data{$b}{stillonline}
            || ($data{$a}{stillonline} && $data{$a}{lastlogin} cmp $data{$b}{lastlogin} )
            || $data{$a}{lastlogout} cmp $data{$b}{lastlogout};
    } 
    $ra = $data{$a}{$what};
    $rb = $data{$b}{$what};

    return $ra cmp $rb;
}

sub by_online
{
    return $data{$a}{seconds} <=> $data{$b}{seconds};
}



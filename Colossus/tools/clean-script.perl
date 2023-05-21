#! /usr/bin/perl


#
# Remove noise from beginning and enf of a "script" output file
#

foreach my $file (@ARGV)
{
    open(FILE, $file) or die "Can't open/read file '$file'!\nDied ";
    my @lines = <FILE>;
    chomp @lines;
    close FILE;

    
    my $outfile = $file . ".out";
    open(OUT, ">$outfile") or die "Can't open/write file '$outfile'!\nDied ";

    my $in_exception = 0;
    my $printit = 0;

    foreach(@lines)
    {
	$_ =~ s/\r//g;
	if (/^Game #1\b/)
	{
	    $printit = 1;
	}

	if (/^Batch loop ends/)
	{
	    $printit = 0;
	}

	if (/^Exception in/ or /^\tat /)
	{
	    $in_exception = 1;
	}
	else
	{
	    $in_exception = 0;
	}
	
	if ($printit and not  $in_exception)
	{
	    print STDERR "printit true\n";
	    print OUT $_, "\n";
	}
	else
	{
	    print STDERR "printit false\n";
	}
    }
    close OUT;

    system("mv $file $file.old");
    system("mv $outfile $file");

}




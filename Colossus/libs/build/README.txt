This is the SVNant task in version 1.2.1 -- downloaded from:

  http://subclipse.tigris.org/svnant.html

In the committed configuration it relies on having a command
line Subversion accessible on the shell's path, which has to 
be Subversion 1.5 or highter. If this folder contains the 
version of SVNkit distributed with the download
of SVNant it will use that, making the build 100% Java.

Subversion is set up to ignore the svnkit.jar file as not to
increase the checkout size too much.
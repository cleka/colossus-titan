# If rmiregistry is already running, this should fail harmlessly.
start rmiregistry

java -Djava.rmi.server.codebase=file:./ -Djava.security.policy=.java.policy net.sf.colossus.server.Start %1 %2 %3 %4 %5 %6 %7 %8 %9

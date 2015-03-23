# Java Client Library for the IOBeam RESTful API #



## Building ##

```$ mvn package```

A jar file will be produced under ```/target```

## Integration Tests ##

The test suite contains a number of integration tests that checks common API 
calls against the real API servers. These tests are not run by default, but can
be manually run as follows:

```$  mvn verify -DskipITs=false```
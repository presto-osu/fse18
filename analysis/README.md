# Static Analysis of Energy Inefficiencies in Watch Faces

This is the static analysis tool for detection of energy inefficiencies in
watch faces. The details of the analysis can be found in the paper. 

## Prerequisite

Python 3 and latest version of Java (tested on JDK 8u171).

## Build

```bash
$ ./gradlew :wear:shadowJar  # build Jar
```

## Run

```bash
$ ./gator w /path/to/some.apk
```

The analysis outputs the paths labelled by 'SENSOR LEAK' for a watch face
if it contains any potential sensor-related energy inefficiencies. It outputs
test cases with label 'LEAK TESTCASE'.

If a watch face has potential display-related inefficiencies, the analysis
outputs possible colors flow to setColor and drawColor calls, labelled with
'colors equal=true' and 'colors not equal' for Pattern 1 and Pattern 2.

Note that the static analysis provides a comprehensive evaluation of energy
inefficiencies. We have in Table 1 excluded reported watch faces acquiring
sensors that our watch, the LG Watch Style, is not equipped with.




#!/bin/bash

dacapo=(
    "avrora" "batik" "eclipse" "fop" "h2"
    "jython" "luindex" "lusearch" "pmd" "sunflow"
    "tomcat" "tradebeans" "tradesoap" "xalan"
)


for test in "${dacapo[@]}"
do
    # TODO
    # java.system.class.loader only works for single main class
    # need a custom built dacapo build with custom Harness
    java -cp build/libs/jrewriter-0.1.0.jar:bench/dacapo-9.12-MR1-bach.jar \
         Harness "$test"
done


# still failing:
# batik
# jython
# tomcat
# tradebeans
# tradesoap

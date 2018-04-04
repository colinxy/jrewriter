#!/bin/bash

dacapo=(
    "avrora" "batik" "eclipse" "fop" "h2"
    "jython" "luindex" "lusearch" "pmd" "sunflow"
    "tomcat" "tradebeans" "tradesoap" "xalan"
)

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"


cd "$DIR/../" && {
    # gradle has to be run from project root directory

    for test in "${dacapo[@]}"
    do
        java -Djava.system.class.loader=jrewriter.RewriterClassLoader \
             -cp build/libs/jrewriter-0.1.0.jar:bench/dacapo-9.12-MR1-bach.jar \
             Harness "$test"
    done
}

# still failing:
# batik
# jython
# tomcat
# tradebeans
# tradesoap

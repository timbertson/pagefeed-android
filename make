sbt compile | strip-nonprintable | sed -Ee 's/\[[0-9]+m//g'

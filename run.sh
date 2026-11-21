#!/usr/bin/env bash
# Usage: ./run.sh [--console]
set -euo pipefail
CP="out:$(ls lib/*.jar | tr '\n' ':')"
java --module-path "$(ls lib/javafx-*.jar | tr '\n' ':')" \
     --add-modules javafx.controls \
     -cp "$CP" coderev.Main "$@"

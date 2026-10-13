#!/bin/bash
# usage: drive.sh "<line>" ...  |  drive.sh -f <file.shots>
# Appends the lines to the GUI shot harness queue (-Dbts.guishots=true, see docs/singularity-ladder-build.md), waits
# until the harness has run them, and prints the log lines they produced. Shots land in run/client/screenshots/guishots.
if [ "$1" = "-f" ]; then mapfile -t ARGS < <(grep -v '^\s*#' "$2" | grep -v '^\s*$'); set -- "${ARGS[@]}"; fi
D="$(cd "$(dirname "$0")/../.." && pwd)/run/client/guishots"
L="$D/guishots.log"
n=$(($(wc -l < "$L") + 1))
M="M$$x$RANDOM"
{ for c in "$@"; do echo "$c"; done; echo "say $M"; } >> "$D/queue.txt"
for i in $(seq 1 1800); do tail -n +$n "$L" | grep -q "MARK $M" && break; sleep 1; done
tail -n +$n "$L" | grep -v "MARK $M\|> say $M" | sed -E 's/^\[t *[0-9]+\] //' | cut -c1-600

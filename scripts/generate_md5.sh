#!/usr/bin/env bash
# ArizenOS Lite — generate_md5.sh
# Appends MD5 checksum to tar (Samsung Odin format)
set -euo pipefail

INPUT_TAR="${1:-}"
OUTPUT_FILE="${2:-}"

[[ -z "$INPUT_TAR" ]] && { echo "Usage: $0 <input.tar> <output.tar.md5>"; exit 1; }
[[ ! -f "$INPUT_TAR" ]] && { echo "File not found: $INPUT_TAR"; exit 1; }

OUTPUT_FILE="${OUTPUT_FILE:-${INPUT_TAR%.tar}.tar.md5}"

MD5=$(md5sum "$INPUT_TAR" | awk '{print $1}')
echo "MD5: $MD5"

cat "$INPUT_TAR" > "$OUTPUT_FILE"
printf "%s" "$MD5" >> "$OUTPUT_FILE"

echo "Generated: $OUTPUT_FILE"

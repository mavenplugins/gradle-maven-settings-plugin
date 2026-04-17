#!/bin/bash
#
# Derive next snapshot version from EFFECTIVE_RELEASE_VERSION
#
RELEASE_VERSION="$1"
if [[ ! "${RELEASE_VERSION}" =~ ^[0-9]+(\.[0-9]+)*$ ]]; then
  echo "invalid"
  exit 1
fi
# Split release version parts into ARRAY
_IFS=$IFS && IFS='.' && read -r -a ARRAY <<< "${RELEASE_VERSION}" && IFS=$_IFS
j=$((${#ARRAY[@]} - 1))
ARRAY[j]=$((ARRAY[$j] + 1))
NEXT_VERSION=""
for i in ${!ARRAY[@]}; do
  NEXT_VERSION="${NEXT_VERSION}${ARRAY[$i]}"
  [[ $i -lt $j ]] && NEXT_VERSION="${NEXT_VERSION}."
done
echo "${NEXT_VERSION}"

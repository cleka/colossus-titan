#!/bin/bash


for FILE in "$@"; do
	mv "$FILE" "$FILE".orig
	cat "$FILE".orig |\
		sed -e 's/bramble=/Brambles=/' \
		    -e 's/drift=/Drift=/' \
		    -e 's/bog=/Bog=/' \
		    -e 's/sanddune=/Sand=/' \
		    -e 's/volcano=/Volcano=/' \
		    -e 's/stone=/Stone=/' \
		    -e 's/tree=/Tree=/' \
		    -e 's/water=/Lake=/' \
		         > "$FILE"
	echo "Please add 'version=\"2\"' to the XML file $FILE"
done

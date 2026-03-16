#!/bin/bash
set -e

CURRENT=$(sed -n '/<artifactId>sendkit<\/artifactId>/{n;s/.*<version>\(.*\)<\/version>.*/\1/p;}' pom.xml)
MAJOR=$(echo "$CURRENT" | cut -d. -f1)
MINOR=$(echo "$CURRENT" | cut -d. -f2)
PATCH=$(echo "$CURRENT" | cut -d. -f3)
VERSION="$MAJOR.$MINOR.$((PATCH + 1))"

echo "Current version: $CURRENT"
echo "New version: $VERSION"

sed -i '' "/<artifactId>sendkit<\/artifactId>/{n;s|<version>.*</version>|<version>$VERSION</version>|;}" pom.xml

git add pom.xml
git commit -m "bump version to $VERSION"
git push

git tag "$VERSION"
git push origin "$VERSION"

echo "Released $VERSION successfully!"

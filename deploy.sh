#!/bin/bash

OLDVER=$(grep -Po '<version>\K[^<]+' pom.xml | head -n1)
echo "Last version tag was ${OLDVER}"
read -e -p "New version tag: " ver

sed -i -e "0,/$OLDVER/s/$OLDVER/$ver/" pom.xml
sed -i -e "s/$OLDVER/$ver/g" docs/index.html
sed -i -e "s/$OLDVER/$ver/g" installer.sh
sed -i -e "s/$OLDVER/$ver/g" helm/scoold/values.yaml
sed -i -e "s/$OLDVER/$ver/g" helm/scoold/Chart.yaml
git add -A && git commit -m "Release v$ver."
git tag "$ver"
git push origin master
git push --tags
git log $OLDVER..HEAD --oneline > changelog.txt && \
echo "" >> changelog.txt && \
hub release create -F changelog.txt -t "$ver" $ver && \
rm changelog.txt
echo "--done--"

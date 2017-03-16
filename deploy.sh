#!/bin/bash
OLDVER=$(grep -Po '<version>\K[^<]+' pom.xml | head -n1)
read -e -p "Tag: " ver
sed -i -e "0,/$OLDVER/s/$OLDVER/$ver/" pom.xml 
sed -i -e "s/$OLDVER/$ver/g" docs/index.html 
git add -A && git commit -m "Release v$ver."
git tag "$ver"
git push origin master
git push --tags
echo "--done--"

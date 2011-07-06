#!/bin/bash
git add *
git commit -am "$1"

if [ -n "$2" ]; then
  git remote rm srv1
  git remote add srv1 git@$2:puppet.git
fi

git push srv1 master
#git push snow master

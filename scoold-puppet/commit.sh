#!/bin/bash
rm -rf .git/
git init
git add scoold/**
git commit -am "puppet code commit"

if [ -n "$1" ]; then
  git push ubuntu@$1:puppet.git +master
fi

#git push snow master

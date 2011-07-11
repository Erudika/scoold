#!/bin/bash
git add scoold/*
git commit -am "puppet code - no msg"

if [ -n "$1" ]; then
  git push git@$1:puppet.git master
fi

#git push snow master

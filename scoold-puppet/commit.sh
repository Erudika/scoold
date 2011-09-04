# !/bin/bash
rm -rf .git/
git init
git add scoold/**
git commit -am "puppet code commit"

if [ -n "$1" ]; then
  git push ubuntu@$1:puppet.git +master

  if [ -n "$2" ]; then
	sed -e "1,/\\\$nodename/ s/\\\$nodename.*/\\\$nodename = \"$2\"/" -i.bak ./scoold/manifests/init.pp
	rm ./scoold/manifests/*.bak
  fi
fi

#git push snow master

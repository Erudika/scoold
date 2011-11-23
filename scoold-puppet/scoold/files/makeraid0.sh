# create RAID0
apt-get -y install xfsprogs mdadm
umount /dev/xvdb
sudo sed -e "1,/\/dev.*/ s/\/dev\/xvdb/\#\/dev\/xvdb/" -i.bak /etc/fstab 
modprobe dm-mod
yes | mdadm --create /dev/md0 --level=0 --chunk=256 --raid-devices=2 /dev/xvdb /dev/xvdc
echo 'DEVICE /dev/xvdb /dev/xvdc' > /etc/mdadm.conf
mdadm --detail --scan >> /etc/mdadm.conf
blockdev --setra 65536 /dev/md0
mkfs.xfs -f /dev/md0
mkdir -p /mnt/md0 && mount -t xfs -o noatime /dev/md0 /mnt/md0


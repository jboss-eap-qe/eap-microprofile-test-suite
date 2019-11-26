#!/bin/bash

DOCKER_IMAGES=`git grep Docker.Builder 2>/dev/null | sed "s/.*Docker.Builder//g" | cut -d, -f2 | cut -d\" -f2 | sort | uniq | grep -v Invalid`

case "$1" in 
fetch-images)
   for i in $DOCKER_IMAGES; do 
      docker pull $i
   done
   ;;
save-images)
   $0 fetch-images
   docker save `echo $DOCKER_IMAGES | tr "\n" " "` | gzip > docker-images.tar.gz
   du -h docker-images.tar.gz
   ;;
load-images)
   docker load --input docker-images.tar.gz
   ;;
ts-maven-repo)
   rm -rf local-repo/
   ##  vvvv doesn'r cover download of surefire-junit4-2.22.2.jar because no tests were executed vvvv
   ## ./mvnw -s settings-dist.xml -Dmaven.repo.local=$PWD/local-repo verify       -Dtest=NONE -DfailIfNoTests=false -Plocal-m2-repository -q
   ./mvnw -s settings-dist.xml -Dmaven.repo.local=$PWD/local-repo verify       -Plocal-m2-repository -q -fn -Dtest=MicroProfileHealth21Test -DfailIfNoTests=false -Dmaven.test.redirectTestOutputToFile=true
   ./mvnw -s settings-dist.xml -Dmaven.repo.local=$PWD/local-repo clean        -Plocal-m2-repository -q
   du -cskh local-repo/
   ;;
dist)
   $0 save-images
   $0 ts-maven-repo
   ;;
dist-zip)
   $0 dist
   rm -f eap-microprofile-test-suite-dist.zip
   zip -q -r eap-microprofile-test-suite-dist.zip . -x *.idea* -x *.git* -x eap-microprofile-test-suite-dist.zip
   du -h eap-microprofile-test-suite-dist.zip
   ;;
dist-run)
   $0 load-images
   MODULES_WITH_MP_PREFIX=`find . -mindepth 1 -maxdepth 1 -type d | grep 'microprofile-' | cut -d'/' -f2- | tr '\n' ','`
   ./mvnw -s settings-dist.xml -Dmaven.repo.local=$PWD/local-repo verify       -o -pl $MODULES_WITH_MP_PREFIX -Dmaven.test.redirectTestOutputToFile=true
   ;;
clean)
   rm -rf local-repo
   rm docker-images.tar.gz
   rm eap-microprofile-test-suite-dist.zip
   ;;
*)
   echo "Usage: $0 {dist-zip|dist-run}"
esac

exit 0 
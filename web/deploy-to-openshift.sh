oc new-build openshift/nginx --name=web --binary
oc start-build web --from-dir=build -w
oc new-app web
oc expose svc web

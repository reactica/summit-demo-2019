policy add-role-to-user view -z default
echo '{"kind": "ServiceAccount", "apiVersion": "v1", "metadata": {"name": "amq-service-account"}}' | oc create -f -
oc policy add-role-to-user view system:serviceaccount:amq-demo:amq-service-account
oc new-app --template=amq-broker-72-basic \
   -e AMQ_PROTOCOL=amqp \
   -e AMQ_QUEUES=ride-started,ride-completed,user-in-line \
   -e AMQ_USER=user \
   -e ADMIN_PASSWORD=user123
oc new-app jboss/infinispan-server:10.0.0.Beta3 --name=datagrid


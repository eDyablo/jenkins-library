#!/usr/bin/env bash

MASTER_TEMPLATE=${MASTER_TEMPLATE:-load_test/deploy/kubernetes/locust_master_template.yml}
SLAVES_TEMPLATE=${SLAVES_TEMPLATE:-load_test/deploy/kubernetes/locust_workers_template.yml}
DEPLOY_TIMEOUT=${DEPLOY_TIMEOUT:-120}
DEPLOY_NAMESPACE=${DEPLOY_NAMESPACE:-default}
echo "Deployment timeout: $DEPLOY_TIMEOUT"

kubectl apply -f ${MASTER_TEMPLATE}

pods=$(kubectl get pods --namespace=$DEPLOY_NAMESPACE -l role=master --output=jsonpath='{.items[*].metadata.name}')
echo Running master: ${pods}

timeout=0
while sleep 1; do
    ((timeout++))

    status=$(kubectl get pods -n $DEPLOY_NAMESPACE $pods -o=custom-columns=STATUS:.status.phase)
    echo "waiting: $timeout sec. $status"

    if [[ $timeout == $DEPLOY_TIMEOUT ]]; then
      echo "Deployment failed. Timeout!"
      exit 1
    fi

    if [[ $status == *"Running"* ]]; then
        echo "Master is running!!!"
        break
        #exit 0
    fi
done


kubectl apply -f ${SLAVES_TEMPLATE}
slaves=$(kubectl get pods --namespace=$DEPLOY_NAMESPACE -l role=worker --output=jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}')
slave1=$(kubectl get pods --namespace=$DEPLOY_NAMESPACE -l role=worker --output=jsonpath='{.items[0].metadata.name}')

echo $slaves | tr " ", "\n" | while read slave; do
    echo Running slave: $slave
done

timeout=0
while sleep 1; do
    ((timeout++))

    status=$(kubectl get pods -n $DEPLOY_NAMESPACE $slave1 -o=custom-columns=STATUS:.status.phase)
    echo "waiting: $timeout sec. $status"

    if [[ $timeout == $DEPLOY_TIMEOUT ]]; then
      echo "Deployment failed. Timeout!"
      exit 1
    fi

    if [[ $status == *"Running"* ]]; then
        echo "Slave is running!!!"
        break
        #exit 0
    fi
done

kubectl logs -fn0 $pods -n $DEPLOY_NAMESPACE | while read line ; do
    echo "$line"
#    echo "$line" | grep "start sleeping for 10 sec."
#    if [ $? = 0 ]
#        then
#            kubectl cp ${pods}:/bzt-configs ./test_artifacts --namespace=$DEPLOY_NAMESPACE
#            echo "Copied reports!!"
#            break
#    fi
    if echo "$line" | grep -q "Test Failed"; then
        final_slaves=$(kubectl get pods --namespace=$DEPLOY_NAMESPACE -l role=worker --output=jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}')
        echo "Test Failed."
        kubectl cp ${pods}:/locust-tasks ./test_artifacts --namespace=$DEPLOY_NAMESPACE

        count=0
        echo $final_slaves | tr " ", "\n" | while read slave; do
            ((count++))
            kubectl cp ${slave}:/locust-tasks/locust-slave.log ./test_artifacts/locust-slave${count}.log --namespace=$DEPLOY_NAMESPACE
        done

        echo "Copied artifacts!!"
        exit 1
        break
    elif echo "$line" | grep -q  "Test Passed"; then
        final_slaves=$(kubectl get pods --namespace=$DEPLOY_NAMESPACE -l role=worker --output=jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}')
        echo "Test Passed."
        kubectl cp ${pods}:/locust-tasks ./test_artifacts --namespace=$DEPLOY_NAMESPACE

        count=0
        echo $final_slaves | tr " ", "\n" | while read slave; do
            ((count++))
            kubectl cp ${slave}:/locust-tasks/locust-slave.log ./test_artifacts/locust-slave${count}.log --namespace=${DEPLOY_NAMESPACE}
        done

        echo "Copied artifacts!!"
        exit 0
        break
    fi
done




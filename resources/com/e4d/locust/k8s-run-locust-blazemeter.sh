#!/usr/bin/env bash

TAURUS_TEMPLATE=${TAURUS_TEMPLATE:-load_test/deploy/kubernetes/locust_taurus_job_template.yml}
DEPLOY_TIMEOUT=${DEPLOY_TIMEOUT:-120}
echo "Deployment timeout: $DEPLOY_TIMEOUT"

kubectl apply -f ${TAURUS_TEMPLATE}

pods=$(kubectl get pods --namespace=locust --selector=job-name=locust-taurus-job --output=jsonpath='{.items[*].metadata.name}')
echo Running pod: ${pods}

timeout=0
while sleep 1; do
    ((timeout++))

    status=$(kubectl get pods -n locust $pods -o=custom-columns=STATUS:.status.phase)
    echo "waiting: $timeout sec. $status"

    if [[ $timeout == $DEPLOY_TIMEOUT ]]; then
      echo "Deployment failed. Timeout!"
      exit 1
    fi

    if [[ $status == *"Running"* ]]; then
        echo "Container is running!!!"
        break
        #exit 0
    fi
done

kubectl logs -fn0 $pods -n locust | while read line ; do
    echo "$line"
#    echo "$line" | grep "start sleeping for 10 sec."
#    if [ $? = 0 ]
#        then
#            kubectl cp ${pods}:/bzt-configs ./reports --namespace=locust
#            echo "Copied reports!!"
#            break
#    fi
    if echo "$line" | grep -q "WARNING: Done performing with code: 3"; then
        echo "Test Failed."
        kubectl cp ${pods}:/bzt-configs ./reports --namespace=locust
        echo "Copied reports!!"
        kubectl cp ${pods}:/tmp/artifacts ./artifacts --namespace=locust
        echo "Copied artifacts!!"
        exit 1
        break
    elif echo "$line" | grep -q  "INFO: Done performing with code: 0"; then
        echo "Test Passed."
        kubectl cp ${pods}:/bzt-configs ./reports --namespace=locust
        echo "Copied reports!!"
        kubectl cp ${pods}:/tmp/artifacts ./artifacts --namespace=locust
        echo "Copied artifacts!!"
        exit 0
        break
    fi
done


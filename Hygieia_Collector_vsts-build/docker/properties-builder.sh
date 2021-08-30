#!/bin/bash

# if [ "$SKIP_PROPERTIES_BUILDER" = true ]; then
#   echo "Skipping properties builder"
#   exit 0
# fi
  
# if [ "$MONGO_PORT" != "" ]; then
# 	# Sample: MONGO_PORT=tcp://172.17.0.20:27017
# 	MONGODB_HOST=`echo $MONGO_PORT|sed 's;.*://\([^:]*\):\(.*\);\1;'`
# 	MONGODB_PORT=`echo $MONGO_PORT|sed 's;.*://\([^:]*\):\(.*\);\2;'`
# else
# 	env
# 	echo "ERROR: MONGO_PORT not defined"
# 	exit 1
# fi

echo "MONGODB_HOST: $MONGODB_HOST"
echo "MONGODB_PORT: $MONGODB_PORT"

cat > $PROP_FILE <<EOF
#Database Name
dbname=${HYGIEIA_API_ENV_SPRING_DATA_MONGODB_DATABASE:-dashboarddb}

#Database HostName - default is localhost
dbhost=${MONGODB_HOST:-db}

#Database Port - default is 27017
dbport=${MONGODB_PORT:-27017}

#Database Username - default is blank
dbusername=${HYGIEIA_API_ENV_SPRING_DATA_MONGODB_USERNAME:-dashboarduser}

#Database Password - default is blank
dbpassword=${HYGIEIA_API_ENV_SPRING_DATA_MONGODB_PASSWORD:-dbpassword}

#Collector schedule (required)
vsts-build.cron=${VSTS_BUILD_CRON:-0 0/2 * * * *}

# Logging File location
logging.file=./logs/vsts-build.log

#VSTS protocol (optional, defaults to 'https')
vsts-build.protocol=${VSTS_PROTOCOL:-https}

#Gitlab port (optional, defaults to protocol default port)
vsts-build.port=${VSTS_PORT:-}

vsts-build.apiKey=${VSTS_API_TOKEN:-@tokenVSTS@}

#vsts API Version (optional, defaults to current version of 4)
vsts-build.apiVersion==${VSTS_API_VERSION:-4.1}

# VSTS Proxy Host proxywsmed.bancolombia.corp
vsts-build.proxyHost=${PROXY_HOST:-}

# VSTS Proxy Port 8080
vsts-build.proxyPort=${PROXY_PORT:-}

# VSTS Non Proxy xxx.xxx.xxx.xxx
vsts-build.nonProxy=${NOT_PROXY_HOST:-}

# Jenkins server (required) - Can provide multiple
vsts-build.servers[0]=${VSTS_SERVER:-https://grupobancolombia.visualstudio.com/Vicepresidencia%20Servicios%20de%20Tecnolog%C3%ADa}

#Determines if build console log is collected - defaults to false
vsts-build.saveLog=${VSTS_BUILD_SAVE_LOG:-false}

#Determines if build console log is collected - defaults to false
vsts-build.nice-names[0]=${VSTS_SERVER_NICE_NAME:-Azure_DevOps_Bancolombia}

EOF

echo "

===========================================
Properties file created `date`:  $PROP_FILE
Note: passwords & apiKey hidden
===========================================
`cat $PROP_FILE |egrep -vi 'password|apiKey'`
"

exit 0

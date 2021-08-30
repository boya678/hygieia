FROM ubuntu:16.04

# Installation:
# Import MongoDB public GPG key AND create a MongoDB list file
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
RUN echo "deb http://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/3.4 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-3.4.list

# Update apt-get sources AND install MongoDB
RUN apt-get update && apt-get install -y --allow-unauthenticated mongodb-org

RUN apt-get install -y supervisor

# Create the MongoDB data directory
RUN mkdir -p /data/db
RUN mkdir -p /var/log/supervisord
RUN mkdir -p /var/www

RUN export PATH=$PATH:/usr/bin/mongod:/usr/bin/supervisord

COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

EXPOSE 27017

EXPOSE 8090

ADD db-setup.js /tmp/db-setup.js

RUN chmod +x /tmp/db-setup.js

CMD /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf

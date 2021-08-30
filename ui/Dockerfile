FROM docker.io/nginx:latest

VOLUME ["/hygieia/logs"]

COPY docker/default.conf /etc/nginx/conf.d/default.conf.templ
COPY docker/conf-builder.sh /usr/bin/conf-builder.sh
COPY dist /usr/share/nginx/html
RUN chown -R nginx:nginx /usr/share/nginx/html/

EXPOSE 80 443

CMD sleep 70s && sed s:API_HOST:${API_HOST:-api}: /etc/nginx/conf.d/default.conf.templ | sed s:API_PORT:${API_PORT:-8080}: > /etc/nginx/conf.d/default.conf && nginx -g "daemon off;"
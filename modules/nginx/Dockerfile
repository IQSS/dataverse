FROM nginx:latest

# Remove the default NGINX configuration file
RUN rm /etc/nginx/conf.d/default.conf

# Copy the contents of the local default.conf to the container
COPY default.conf /etc/nginx/conf.d/

EXPOSE 4849
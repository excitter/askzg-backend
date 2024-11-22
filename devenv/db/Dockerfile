FROM postgres:14-alpine

# Copy the database dump into the image
COPY db.dump /docker-entrypoint-initdb.d/db.dump
COPY init-db.sh /docker-entrypoint-initdb.d/init-db.sh
RUN chmod +x /docker-entrypoint-initdb.d/init-db.sh

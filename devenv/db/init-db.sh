#!/bin/bash

pg_restore --verbose --clean --no-acl --no-owner -U devuser -d askzgdb /docker-entrypoint-initdb.d/db.dump

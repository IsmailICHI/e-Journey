SELECT 'CREATE DATABASE user_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_db')
\gexec

SELECT 'CREATE DATABASE travel_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travel_db')
\gexec

SELECT 'CREATE DATABASE user_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_db')
\gexec

SELECT 'CREATE DATABASE travel_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travel_db')
\gexec

SELECT 'CREATE DATABASE booking_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'booking_db')
\gexec

SELECT 'CREATE DATABASE payment_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')
\gexec

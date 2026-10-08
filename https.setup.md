# Https locally trusted CA
## RoadMap:


```
1. Clone Neo4flix
        ↓
2. Install mkcert locally
        ↓
3. Create a new local CA
        ↓
4. Trust the CA in Chrome
        ↓
5. Generate a new neo4flix.local certificate
        ↓
6. Verify front-end/certs/
        ↓
7. Start Docker frontend
        ↓
8. Launch dedicated Chrome
        ↓
9. Open https://neo4flix.local:8443
```

## Condensed commands:

```bash
# Install mkcert
mkdir -p ~/.local/bin

curl -L 'https://dl.filippo.io/mkcert/latest?for=linux/amd64' \
  -o ~/.local/bin/mkcert

chmod +x ~/.local/bin/mkcert

# Configure CA
export CAROOT="$HOME/.local/share/mkcert"
mkdir -p "$CAROOT"

# Trust CA in Chrome
certutil -A \
  -n "Neo4flix Local CA" \
  -t "C,," \
  -i "$CAROOT/rootCA.pem" \
  -d sql:$HOME/.pki/nssdb

# Generate certificate
mkdir -p front-end/certs

~/.local/bin/mkcert \
  -cert-file front-end/certs/neo4flix.pem \
  -key-file front-end/certs/neo4flix-key.pem \
  neo4flix.local localhost 127.0.0.1 ::1

# Start frontend
docker compose build frontend
docker compose up -d frontend

# Launch dedicated Chrome
/usr/bin/google-chrome \
  --user-data-dir="$HOME/.config/google-chrome-neo4flix" \
  --host-resolver-rules="MAP neo4flix.local 127.0.0.1"
```

## Domain name :

```
Try to access this in the freshly opened window in Chrome : https://neo4flix.local:8443
```
## Locally:
```
~/
├── .local/
│   ├── bin/
│   │   └── mkcert
│   └── share/
│       └── mkcert/
│           ├── rootCA.pem
│           └── rootCA-key.pem
│
├── .pki/
│   └── nssdb/
│
└── .config/
    └── google-chrome-neo4flix/
```
### Never commit:

```
front-end/certs/**
rootCA-key.pem
neo4flix-key.pem
```


## The complete path is:

```
Chrome
  │
  │ neo4flix.local → 127.0.0.1
  ▼
127.0.0.1:8443
  │
  │ Docker port mapping
  ▼
frontend container :443
  │
  │ Nginx TLS
  ▼
Angular
  │
  │ /api/*
  ▼
gateway:8089
```
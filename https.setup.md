# Https locally trusted CA
## RoadMap:


```
1. Clone travel-paln
        ↓
2. Install mkcert locally
        ↓
3. Create a new local CA
        ↓
4. Trust the CA in Chrome
        ↓
5. Generate a new travel-paln.local certificate
        ↓
6. Verify services/client/certs/
        ↓
7. Start Docker frontend
        ↓
8. Launch dedicated Chrome
        ↓
9. Open https://travel-paln.local:8443
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
  -n "travel-paln Local CA" \
  -t "C,," \
  -i "$CAROOT/rootCA.pem" \
  -d sql:$HOME/.pki/nssdb

# Generate certificate
mkdir -p services/client/certs

~/.local/bin/mkcert \
  -cert-file services/client/certs/travel-paln.pem \
  -key-file services/client/certs/travel-paln-key.pem \
  travel-paln.local localhost 127.0.0.1 ::1

# Start frontend
docker compose build frontend
docker compose up -d frontend

# Launch dedicated Chrome
/usr/bin/google-chrome \
  --user-data-dir="$HOME/.config/google-chrome-travel-paln" \
  --host-resolver-rules="MAP travel-paln.local 127.0.0.1"
```

## Domain name :

```
Try to access this in the freshly opened window in Chrome : https://travel-paln.local:8443
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
    └── google-chrome-travel-paln/
```
### Never commit:

```
front-end/certs/**
rootCA-key.pem
travel-paln-key.pem
```


## The complete path is:

```
Chrome
  │
  │ travel-paln.local → 127.0.0.1
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
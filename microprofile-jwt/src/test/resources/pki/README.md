# Pieces of public key infrastructure necessary for JWT tests


## How to recreate
```bash
ssh-keygen -t rsa -b 2048 -m PEM -f key.private.pem #with empty passphrase!
openssl pkcs8 -topk8 -v2 des3 -nocrypt \
    -in key.private.pem \
    -out key.private.pkcs8.pem
openssl rsa -in key.private.pkcs8.pem -pubout -outform PEM -out key.public.pem
rm key.private.pem key.private.pem.pub #cleanup
```

## Create a inline value from public key for properties files

```bash
sed '$d' key.public.pem | sed '1d' | tr -d '\n'
```
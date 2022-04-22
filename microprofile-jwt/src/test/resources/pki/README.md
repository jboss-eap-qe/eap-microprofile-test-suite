# Pieces of public key infrastructure necessary for JWT tests

There are three main key pairs:

* `key.private.pkcs8.pem` and `key.public.pem` - this pair is the main key pair use in most tests, 2048 bits long
* `key2048_2.private.pkcs8.pem` and `key2048_2.public.pem` - 2048 bits long key pair
* `key4096.private.pkcs8.pem` and `key4096.public.pem` - 4096 bits long key pair
* `key1024.private.pkcs8.pem` and `key1024.public.pem` - 1024 bits long key pair
* `key512.private.pkcs8.pem` and `key512.public.pem` - 512 bits long key pair

## How to recreate key pairs

Just run `gen-keys.sh`. For details inspect the script.

## How to create an inline value from public key for properties files

Just run:
```bash
sed '$d' key.public.pem | sed '1d' | tr -d '\n'
```
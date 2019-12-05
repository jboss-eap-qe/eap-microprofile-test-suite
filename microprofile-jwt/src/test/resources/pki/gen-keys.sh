function gen_key_pair() {

  PREFIX="${1}"
  KEY_SIZE="${2}"

  openssl genpkey -out "${PREFIX}.private.pkcs8.pem" -algorithm RSA -pkeyopt "rsa_keygen_bits:${KEY_SIZE}"
  openssl rsa -in "${PREFIX}.private.pkcs8.pem" -pubout -outform PEM -out "${PREFIX}.public.pem"

}

gen_key_pair key 2048
gen_key_pair key4096 4096
gen_key_pair key1024 1024
gen_key_pair key512 512
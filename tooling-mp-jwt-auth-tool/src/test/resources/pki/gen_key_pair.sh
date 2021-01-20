function gen_key_pair() {

  ALGORITHM="${1}"
  PREFIX="${2}"
  KEY_SIZE="${3}"

  # TODO: consider using a function instead of an if-then-else
  if [[ "${ALGORITHM}" == "RS256" ]]; then
    # RS256: RSASSA-PKCS1-v1_5 using SHA-256
    mkdir -p ${ALGORITHM}
    pushd ${ALGORITHM}

    openssl genpkey -out "${PREFIX}.private.pkcs8.pem" -algorithm RSA -pkeyopt "rsa_keygen_bits:${KEY_SIZE}"
    openssl rsa -in "${PREFIX}.private.pkcs8.pem" -pubout -outform PEM -out "${PREFIX}.public.pem"

    popd
  elif [[ "${ALGORITHM}" == "ES256" ]]; then
    # ES256: ECDSA using P-256 and SHA-256
    mkdir -p ${ALGORITHM}
    pushd ${ALGORITHM}

    # openssl ecparam -list_curves:
    #  secp224r1 : NIST/SECG curve over a 224 bit prime field
    #  secp256k1 : SECG curve over a 256 bit prime field
    #  secp384r1 : NIST/SECG curve over a 384 bit prime field
    #  secp521r1 : NIST/SECG curve over a 521 bit prime field
    #  prime256v1: X9.62/SECG curve over a 256 bit prime field

    # private key
    openssl ecparam -genkey -name prime256v1 -noout -out "${PREFIX}.private.pem"

    # convert the private key to "newer" pem format
    openssl pkcs8 -topk8 -inform pem -in "${PREFIX}.private.pem" -outform pem -nocrypt -out "${PREFIX}.private.pkcs8.pem"

    # public key
    openssl ec -in "${PREFIX}.private.pkcs8.pem" -pubout -out "${PREFIX}.public.pem"

    popd
  elif [[ "${ALGORITHM}" == "RSA-OAEP" ]]; then
    # Key management key algorithm which must be supported is RSA-OAEP (RSAES using Optimal
    # Asymmetric Encryption Padding) with a key length 2048 bits or higher.

    # The actual key-pair is RSA: RSA-OAEP is the specification for for RSA key transport (see https://tools.ietf.org/html/rfc3560)
    mkdir -p ${ALGORITHM}
    pushd ${ALGORITHM}

    openssl genpkey -out "${PREFIX}.private.pkcs8.pem" -algorithm RSA -outform PEM -pkeyopt "rsa_keygen_bits:${KEY_SIZE}"
    openssl rsa -in "${PREFIX}.private.pkcs8.pem" -pubout -outform PEM -out "${PREFIX}.public.pem"

    popd
  else
    echo "Unknown algorithm"
    echo "$0 [RS256|ES256|RSA-OAEP] PREFIX KEY_SIZE"
    exit 1
  fi
}
